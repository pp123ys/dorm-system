package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StayService;
import net.wanhe.dorm.util.JdbcUtil;

import java.util.List;

public class StayServiceImpl implements StayService {

    private StuDao stuDao;
    private BedDao bedDao;
    private RoomDao roomDao;
    private BuildingDao buildingDao;
    private CheckinDao checkinDao;
    private StatDao statDao;

    public StayServiceImpl() {
        stuDao = (StuDao) newDao("net.wanhe.dorm.dao.impl.StuDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dorm.dao.impl.BedDaoImpl");
        roomDao = (RoomDao) newDao("net.wanhe.dorm.dao.impl.RoomDaoImpl");
        buildingDao = (BuildingDao) newDao("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
        checkinDao = (CheckinDao) newDao("net.wanhe.dorm.dao.impl.CheckinDaoImpl");
        statDao = (StatDao) newDao("net.wanhe.dorm.dao.impl.StatDaoImpl");
    }

    /*
     * 反射创建Dao, 服务层与Dao实现类解耦
     */
    private static Object newDao(String className) {
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建" + className + "失败", e);
        }
    }

    @Override
    public List<Building> buildingsWithFreeBed() {
        return statDao.selectBuildingsWithFreeBed();
    }

    @Override
    public List<Bed> freeBeds(int buildingId) {
        return bedDao.selectFreeBeds(buildingId);
    }

    @Override
    public Student stayInfo(int studentNo) throws StayException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StayException("该学号的学生不存在");
        }
        return stu;
    }

    @Override
    public Student checkInTarget(int studentNo) throws StayException {
        Student stu = stayInfo(studentNo);
        if (stu.isCheckedIn()) {
            throw new StayException("该学生已入住" + stu.location() + ", 请先办理退住");
        }
        return stu;
    }

    @Override
    public void checkIn(int studentNo, int bedId, String operator) throws StayException {
        //1.学生: 存在且未入住
        Student stu = checkInTarget(studentNo);
        //2.床位: 存在且正常
        Bed bed = bedDao.selectById(bedId);
        if (bed == null) {
            throw new StayException("床位不存在");
        }
        if (!"正常".equals(bed.getStatus())) {
            throw new StayException("该床位已停用, 不能分配");
        }
        //3.房间: 存在、正常、未住满
        Room room = roomDao.selectById(bed.getRoomId());
        if (room == null) {
            throw new StayException("床位所属房间不存在");
        }
        if (!"正常".equals(room.getStatus())) {
            throw new StayException("该房间已停用, 不能分配");
        }
        if (roomDao.countOccupied(room.getId()) >= room.getCapacity()) {
            throw new StayException("该房间已住满");
        }
        //4.床位没被别人占
        if (stuDao.selectByBedId(bedId) != null) {
            throw new StayException("该床位已被占用");
        }
        //5.楼栋存在且性别相符
        Building building = buildingDao.selectById(room.getBuildingId());
        if (building == null) {
            throw new StayException("房间所属楼栋不存在");
        }
        if (!building.getSex().equals(stu.getSex())) {
            throw new StayException("性别与楼栋不符, 不能入住");
        }
        //6.同事务: 改床位 + 写入住流水
        try {
            JdbcUtil.beginTransaction();
            stuDao.updateBedId(stu.getId(), bedId);
            Checkin c = new Checkin();
            c.setStudentId(stu.getId());
            c.setStudentNo(stu.getNo());
            c.setStudentName(stu.getName());
            c.setAction("入住");
            c.setBuildingName(building.getName());
            c.setRoomNo(room.getRoomNo());
            c.setBedNo(bed.getBedNo());
            c.setOperator(operator);
            checkinDao.insert(c);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            //commit 失败时 JdbcUtil 已清理事务连接, 此处回滚是空操作, 故文案不写死"已回滚"
            throw new StayException("办理入住失败(已回滚或提交失败), 请查询该学生当前状态", e);
        }
    }

    @Override
    public void checkOut(int studentNo, String operator) throws StayException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StayException("该学号的学生不存在");
        }
        if (!stu.isCheckedIn()) {
            throw new StayException("该学生当前未入住, 无需退住");
        }
        //流水要记录退住前的位置, 便于追溯退的是哪张床
        Bed bed = bedDao.selectById(stu.getBedId());
        try {
            JdbcUtil.beginTransaction();
            stuDao.updateBedId(stu.getId(), null);
            Checkin c = new Checkin();
            c.setStudentId(stu.getId());
            c.setStudentNo(stu.getNo());
            c.setStudentName(stu.getName());
            c.setAction("退住");
            if (bed != null) {
                c.setBuildingName(bed.getBuildingName());
                c.setRoomNo(bed.getRoomNo());
                c.setBedNo(bed.getBedNo());
            }
            c.setOperator(operator);
            checkinDao.insert(c);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new StayException("办理退住失败(已回滚或提交失败), 请查询该学生当前状态", e);
        }
    }
}
