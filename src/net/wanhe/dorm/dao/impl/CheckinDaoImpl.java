package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CheckinDaoImpl implements CheckinDao {

    private static final String COLS = "id, student_id, student_no, student_name, action,"
            + " building_name, room_no, bed_no, operator, create_time";

    @Override
    public int insert(Checkin checkin) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            //create_time 交给表上的默认值 CURRENT_TIMESTAMP
            state = conn.prepareStatement(
                    "insert into t_checkin(student_id,student_no,student_name,action,"
                            + "building_name,room_no,bed_no,operator) values(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, checkin.getStudentId());
            state.setInt(2, checkin.getStudentNo());
            state.setString(3, checkin.getStudentName());
            state.setString(4, checkin.getAction());
            state.setString(5, checkin.getBuildingName());
            state.setString(6, checkin.getRoomNo());
            if (checkin.getBedNo() == null) {
                state.setNull(7, Types.INTEGER);
            } else {
                state.setInt(7, checkin.getBedNo());
            }
            state.setString(8, checkin.getOperator());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("CheckinDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public List<Checkin> selectAll() {
        return select("select " + COLS + " from t_checkin order by id", null);
    }

    @Override
    public List<Checkin> selectByStudentNo(int studentNo) {
        return select("select " + COLS + " from t_checkin where student_no = ? order by id", studentNo);
    }

    /*
     * 两个查询只有 SQL 与有没有参数不同, 收在一个私有方法里
     */
    private List<Checkin> select(String sql, Integer param) {
        List<Checkin> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(sql);
            if (param != null) {
                state.setInt(1, param);
            }
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("CheckinDao.select失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 对象
     */
    private Checkin map(ResultSet rs) throws SQLException {
        Checkin c = new Checkin();
        c.setId(rs.getInt("id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentNo(rs.getInt("student_no"));
        c.setStudentName(rs.getString("student_name"));
        c.setAction(rs.getString("action"));
        c.setBuildingName(rs.getString("building_name"));
        c.setRoomNo(rs.getString("room_no"));
        int bedNo = rs.getInt("bed_no");
        c.setBedNo(rs.wasNull() ? null : bedNo);
        c.setOperator(rs.getString("operator"));
        c.setCreateTime(rs.getTimestamp("create_time"));
        return c;
    }
}
