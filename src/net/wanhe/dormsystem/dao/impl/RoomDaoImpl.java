package net.wanhe.dormsystem.dao.impl;

import net.wanhe.dormsystem.dao.RoomDao;
import net.wanhe.dormsystem.pojo.Room;
import net.wanhe.dormsystem.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomDaoImpl implements RoomDao {

    //房间列 + 楼栋名
    private static final String COLS = "r.id, r.building_id, r.room_no, r.capacity, r.status, b.name as building_name";

    //已住人数: 住在这个房间任意床位上的学生数
    private static final String OCCUPIED =
            "(select count(*) from t_student s where s.bed_id in"
                    + " (select bd.id from t_bed bd where bd.room_id = r.id)) as occupied_count";

    private static final String FROM = " from t_room r left join t_building b on b.id = r.building_id";

    @Override
    public List<Room> selectByBuildingId(int buildingId) {
        List<Room> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM
                    + " where r.building_id = ? order by r.room_no");
            state.setInt(1, buildingId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectByBuildingId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Room selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM + " where r.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Room selectByBuildingAndNo(int buildingId, String roomNo) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM
                    + " where r.building_id = ? and r.room_no = ?");
            state.setInt(1, buildingId);
            state.setString(2, roomNo);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectByBuildingAndNo失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Room room) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_room(building_id,room_no,capacity,status) values(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, room.getBuildingId());
            state.setString(2, room.getRoomNo());
            state.setInt(3, room.getCapacity());
            state.setString(4, room.getStatus());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Room room) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "update t_room set building_id=?,room_no=?,capacity=?,status=? where id=?");
            state.setInt(1, room.getBuildingId());
            state.setString(2, room.getRoomNo());
            state.setInt(3, room.getCapacity());
            state.setString(4, room.getStatus());
            state.setInt(5, room.getId());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.update失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int updateCapacity(int roomId, int capacity) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_room set capacity=? where id=?");
            state.setInt(1, capacity);
            state.setInt(2, roomId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.updateCapacity失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_room where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int countByBuildingId(int buildingId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select count(*) from t_room where building_id = ?");
            state.setInt(1, buildingId);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.countByBuildingId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int countOccupied(int roomId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "select count(*) from t_student s join t_bed bd on bd.id = s.bed_id where bd.room_id = ?");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.countOccupied失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Room map(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setId(rs.getInt("id"));
        r.setBuildingId(rs.getInt("building_id"));
        r.setRoomNo(rs.getString("room_no"));
        r.setCapacity(rs.getInt("capacity"));
        r.setStatus(rs.getString("status"));
        r.setBuildingName(rs.getString("building_name"));
        r.setOccupiedCount(rs.getInt("occupied_count"));
        return r;
    }
}
