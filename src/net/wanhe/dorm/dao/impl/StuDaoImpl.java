package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class StuDaoImpl implements StuDao {

    //学生列 + 住宿位置(LEFT JOIN 带出, 未入住时三个字段为 null)
    private static final String COLS = "s.id, s.no, s.name, s.sex, s.age, s.phone, s.bed_id,"
            + " bl.name as building_name, r.room_no, bd.bed_no";

    private static final String JOINS = " from t_student s"
            + " left join t_bed bd on bd.id = s.bed_id"
            + " left join t_room r on r.id = bd.room_id"
            + " left join t_building bl on bl.id = r.building_id";

    @Override
    public List<Student> selectAll() {
        List<Student> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " order by s.no");
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectAll失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectByNo(int no) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.no = ?");
            state.setInt(1, no);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectByNo失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectByBedId(int bedId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.bed_id = ?");
            state.setInt(1, bedId);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectByBedId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Student student) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_student(no,name,sex,age,phone,bed_id) values(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, student.getNo());
            state.setString(2, student.getName());
            state.setString(3, student.getSex());
            setInteger(state, 4, student.getAge());
            state.setString(5, student.getPhone());
            setInteger(state, 6, student.getBedId());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Student student) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "update t_student set name=?,sex=?,age=?,phone=? where no=?");
            state.setString(1, student.getName());
            state.setString(2, student.getSex());
            setInteger(state, 3, student.getAge());
            state.setString(4, student.getPhone());
            state.setInt(5, student.getNo());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.update失败", e);
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
            state = conn.prepareStatement("delete from t_student where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int updateBedId(int studentId, Integer bedId) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_student set bed_id=? where id=?");
            setInteger(state, 1, bedId);
            state.setInt(2, studentId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.updateBedId失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * Integer 赋值: null 要用 setNull, 否则 setInt 会把 null 当 0
     */
    private void setInteger(PreparedStatement state, int index, Integer value) throws SQLException {
        if (value == null) {
            state.setNull(index, Types.INTEGER);
        } else {
            state.setInt(index, value);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setNo(rs.getInt("no"));
        s.setName(rs.getString("name"));
        s.setSex(rs.getString("sex"));
        int age = rs.getInt("age");
        s.setAge(rs.wasNull() ? null : age);
        s.setPhone(rs.getString("phone"));
        int bedId = rs.getInt("bed_id");
        s.setBedId(rs.wasNull() ? null : bedId);
        s.setBuildingName(rs.getString("building_name"));
        s.setRoomNo(rs.getString("room_no"));
        int bedNo = rs.getInt("bed_no");
        s.setBedNo(rs.wasNull() ? null : bedNo);
        return s;
    }
}
