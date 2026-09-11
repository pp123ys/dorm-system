package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StatService;

import java.util.List;

public class StatServiceImpl implements StatService {

    private StatDao statDao;
    private RoomDao roomDao;
    private StuDao stuDao;
    private BedDao bedDao;
    private CheckinDao checkinDao;

    public StatServiceImpl() {
        statDao = (StatDao) newDao("net.wanhe.dorm.dao.impl.StatDaoImpl");
        roomDao = (RoomDao) newDao("net.wanhe.dorm.dao.impl.RoomDaoImpl");
        stuDao = (StuDao) newDao("net.wanhe.dorm.dao.impl.StuDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dorm.dao.impl.BedDaoImpl");
        checkinDao = (CheckinDao) newDao("net.wanhe.dorm.dao.impl.CheckinDaoImpl");
    }

    private static Object newDao(String className) {
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建" + className + "失败", e);
        }
    }

    @Override
    public List<Building> buildingOverview() {
        return statDao.selectBuildingOverview();
    }

    @Override
    public List<Student> roomRoster(int roomId) throws RoomException {
        if (roomDao.selectById(roomId) == null) {
            throw new RoomException("房间不存在");
        }
        return statDao.selectStudentsByRoom(roomId);
    }

    @Override
    public List<Bed> freeBeds(Integer buildingId) {
        //与入住办理共用同一份空床位查询, 不重复实现
        return bedDao.selectFreeBeds(buildingId);
    }

    @Override
    public Student studentStay(int studentNo) throws StuException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StuException("该学号的学生不存在");
        }
        return stu;
    }

    @Override
    public List<Checkin> checkinHistory(Integer studentNo) {
        if (studentNo == null) {
            return checkinDao.selectAll();
        }
        return checkinDao.selectByStudentNo(studentNo);
    }
}
