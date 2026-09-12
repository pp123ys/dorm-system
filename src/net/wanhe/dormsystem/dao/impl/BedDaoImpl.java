package net.wanhe.dormsystem.dao.impl;

import net.wanhe.dormsystem.dao.BedDao;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BedDaoImpl implements BedDao {

    //床位列 + 楼栋名/房间号/在住学生(LEFT JOIN 带出, 用于展示与占用判断)
    private static final String COLS = "bd.id, bd.room_id, bd.bed_no, bd.status, r.room_no,"
            + " bl.name as building_name, s.no as student_no, s.name as student_name";

    private static final String JOINS = " from t_bed bd"
            + " join t_room r on r.id = bd.room_id"
            + " join t_building bl on bl.id = r.building_id"
            + " left join t_student s on s.bed_id = bd.id";

    @Override
    public List<Bed> selectByRoomId(int roomId) {
        List<Bed> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS
                    + " where bd.room_id = ? order by bd.bed_no");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectByRoomId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Bed selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where bd.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public List<Bed> selectFreeBeds(Integer buildingId) {
        List<Bed> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            String sql = "select " + COLS + JOINS
                    + " where bd.status = '正常' and r.status = '正常' and s.id is null";
            if (buildingId != null) {
                sql = sql + " and r.building_id = ?";
            }
            sql = sql + " order by bl.name, r.room_no, bd.bed_no";
            state = conn.prepareStatement(sql);
            if (buildingId != null) {
                state.setInt(1, buildingId);
            }
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectFreeBeds失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Bed bed) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_bed(room_id,bed_no,status) values(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, bed.getRoomId());
            state.setInt(2, bed.getBedNo());
            state.setString(3, bed.getStatus());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_bed where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int deleteByRoomId(int roomId) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_bed where room_id = ?");
            state.setInt(1, roomId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.deleteByRoomId失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int deleteFreeBedsAbove(int roomId, int keepCount) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete bd from t_bed bd"
                    + " left join t_student s on s.bed_id = bd.id"
                    + " where bd.room_id = ? and bd.bed_no > ? and s.id is null");
            state.setInt(1, roomId);
            state.setInt(2, keepCount);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.deleteFreeBedsAbove失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int countOccupiedAbove(int roomId, int keepCount) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select count(*) from t_bed bd"
                    + " join t_student s on s.bed_id = bd.id"
                    + " where bd.room_id = ? and bd.bed_no > ?");
            state.setInt(1, roomId);
            state.setInt(2, keepCount);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.countOccupiedAbove失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int updateStatus(int id, String status) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_bed set status = ? where id = ?");
            state.setString(1, status);
            state.setInt(2, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.updateStatus失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     * student_no 可能为 NULL, getInt 会返回 0, 这里用 wasNull 还原成 null
     */
    private Bed map(ResultSet rs) throws SQLException {
        Bed b = new Bed();
        b.setId(rs.getInt("id"));
        b.setRoomId(rs.getInt("room_id"));
        b.setBedNo(rs.getInt("bed_no"));
        b.setStatus(rs.getString("status"));
        b.setRoomNo(rs.getString("room_no"));
        b.setBuildingName(rs.getString("building_name"));
        int studentNo = rs.getInt("student_no");
        b.setStudentNo(rs.wasNull() ? null : studentNo);
        b.setStudentName(rs.getString("student_name"));
        return b;
    }
}
