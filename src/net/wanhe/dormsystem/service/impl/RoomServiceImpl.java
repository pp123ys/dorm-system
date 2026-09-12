package net.wanhe.dormsystem.service.impl;

import net.wanhe.dormsystem.dao.BedDao;
import net.wanhe.dormsystem.dao.BuildingDao;
import net.wanhe.dormsystem.dao.RoomDao;
import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Room;
import net.wanhe.dormsystem.service.RoomService;
import net.wanhe.dormsystem.util.JdbcUtil;

import java.util.List;

public class RoomServiceImpl implements RoomService {

    private RoomDao roomDao;
    private BedDao bedDao;
    private BuildingDao buildingDao;

    public RoomServiceImpl() {
        roomDao = (RoomDao) newDao("net.wanhe.dormsystem.dao.impl.RoomDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dormsystem.dao.impl.BedDaoImpl");
        buildingDao = (BuildingDao) newDao("net.wanhe.dormsystem.dao.impl.BuildingDaoImpl");
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
    public List<Room> listByBuilding(int buildingId) throws RoomException {
        if (buildingDao.selectById(buildingId) == null) {
            throw new RoomException("楼栋不存在");
        }
        return roomDao.selectByBuildingId(buildingId);
    }

    @Override
    public List<Bed> listBeds(int roomId) {
        return bedDao.selectByRoomId(roomId);
    }

    @Override
    public void add(Room room) throws RoomException {
        if (buildingDao.selectById(room.getBuildingId()) == null) {
            throw new RoomException("楼栋不存在");
        }
        checkRoomNo(room.getRoomNo());
        checkCapacity(room.getCapacity());
        if (roomDao.selectByBuildingAndNo(room.getBuildingId(), room.getRoomNo()) != null) {
            throw new RoomException("该楼栋下已存在房间:" + room.getRoomNo());
        }
        room.setStatus("正常");
        try {
            //房间与床位要么都成功, 要么都回滚
            JdbcUtil.beginTransaction();
            int roomId = roomDao.insert(room);
            for (int i = 1; i <= room.getCapacity(); i++) {
                bedDao.insert(new Bed(roomId, i, "正常"));
            }
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("新增房间失败, 已回滚或提交失败", e);
        }
    }

    @Override
    public void updateCapacity(int roomId, int newCapacity) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        checkCapacity(newCapacity);
        int occupied = roomDao.countOccupied(roomId);
        if (newCapacity < occupied) {
            throw new RoomException("新床位数不能少于已住人数" + occupied);
        }
        List<Bed> beds = bedDao.selectByRoomId(roomId);
        try {
            JdbcUtil.beginTransaction();
            roomDao.updateCapacity(roomId, newCapacity);
            if (newCapacity > beds.size()) {
                int maxBedNo = 0;
                for (Bed b : beds) {
                    if (b.getBedNo() > maxBedNo) {
                        maxBedNo = b.getBedNo();
                    }
                }
                //从最大床号往后补, 保证床号连续
                for (int i = maxBedNo + 1; i <= newCapacity; i++) {
                    bedDao.insert(new Bed(roomId, i, "正常"));
                }
            } else if (newCapacity < beds.size()) {
                //防 TOCTOU: 在事务内重查(检查与删除之间外部连接不能插入入住),
                //只删无人住的高床号床位, 保证「床数=容量」不变量
                int occupiedAbove = bedDao.countOccupiedAbove(roomId, newCapacity);
                if (occupiedAbove > 0) {
                    throw new RoomException("床号大于" + newCapacity + "的床位上有" + occupiedAbove + "名在住学生, 请先办理退住");
                }
                bedDao.deleteFreeBedsAbove(roomId, newCapacity);
            }
            JdbcUtil.commit();
        } catch (RoomException e) {
            JdbcUtil.rollbackQuietly();
            throw e;
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("调整房间容量失败, 已回滚或提交失败", e);
        }
    }

    @Override
    public void updateRoomStatus(int roomId, String status) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        checkStatus(status);
        if ("停用".equals(status) && roomDao.countOccupied(roomId) > 0) {
            throw new RoomException("房间内还有在住学生, 不能停用");
        }
        room.setStatus(status);
        roomDao.update(room);
    }

    @Override
    public void updateBedStatus(int bedId, String status) throws RoomException {
        Bed bed = bedDao.selectById(bedId);
        if (bed == null) {
            throw new RoomException("床位不存在");
        }
        checkStatus(status);
        if ("停用".equals(status) && bed.isOccupied()) {
            throw new RoomException("该床位上有在住学生, 不能停用");
        }
        bedDao.updateStatus(bedId, status);
    }

    @Override
    public void delete(int roomId) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        int occupied = roomDao.countOccupied(roomId);
        if (occupied > 0) {
            throw new RoomException("房间内还有" + occupied + "名在住学生, 不能删除");
        }
        try {
            JdbcUtil.beginTransaction();
            bedDao.deleteByRoomId(roomId);
            roomDao.delete(roomId);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("删除房间失败, 已回滚或提交失败", e);
        }
    }

    private void checkRoomNo(String roomNo) throws RoomException {
        if (roomNo == null || roomNo.trim().isEmpty()) {
            throw new RoomException("房间号不能为空");
        }
        if (roomNo.length() > 20) {
            throw new RoomException("房间号不能超过20个字符");
        }
    }

    private void checkCapacity(int capacity) throws RoomException {
        if (capacity < 1 || capacity > 20) {
            throw new RoomException("床位数必须在1~20之间");
        }
    }

    private void checkStatus(String status) throws RoomException {
        if (!"正常".equals(status) && !"停用".equals(status)) {
            throw new RoomException("状态只能是 正常 或 停用");
        }
    }
}
