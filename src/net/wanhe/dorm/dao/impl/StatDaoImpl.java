package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatDaoImpl implements StatDao {

    //楼栋统计列: 房间数 / 可分配床位数(床位正常且房间正常) / 已住人数
    private static final String BUILDING_COLS = "b.id, b.name, b.sex, b.floors, b.remark,"
            + " (select count(*) from t_room r where r.building_id = b.id) as room_count,"
            + " (select count(*) from t_bed bd join t_room r on r.id = bd.room_id"
            + "     where r.building_id = b.id and bd.status = '正常' and r.status = '正常') as bed_count,"
            + " (select count(*) from t_student s join t_bed bd on bd.id = s.bed_id"
            + "     join t_room r on r.id = bd.room_id"
            + "     where r.building_id = b.id and bd.status = '正常' and r.status = '正常') as occupied_count";

    @Override
    public List<Building> selectBuildingOverview() {
        return selectBuildings("select " + BUILDING_COLS + " from t_building b order by b.id");
    }

    @Override
    public List<Building> selectBuildingsWithFreeBed() {
        return selectBuildings("select " + BUILDING_COLS + " from t_building b"
                + " having bed_count > occupied_count order by b.id");
    }

    @Override
    public List<Student> selectStudentsByRoom(int roomId) {
        List<Student> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select s.id, s.no, s.name, s.sex, s.age, s.phone, s.bed_id,"
                    + " bl.name as building_name, r.room_no, bd.bed_no"
                    + " from t_student s"
                    + " join t_bed bd on bd.id = s.bed_id"
                    + " join t_room r on r.id = bd.room_id"
                    + " join t_building bl on bl.id = r.building_id"
                    + " where bd.room_id = ? order by s.no");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(mapStudent(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StatDao.selectStudentsByRoom失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 两个楼栋统计查询只差 SQL, 收在一个私有方法里
     */
    private List<Building> selectBuildings(String sql) {
        List<Building> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(sql);
            rs = state.executeQuery();
            while (rs.next()) {
                Building b = new Building();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setSex(rs.getString("sex"));
                b.setFloors(rs.getInt("floors"));
                b.setRemark(rs.getString("remark"));
                b.setRoomCount(rs.getInt("room_count"));
                b.setBedCount(rs.getInt("bed_count"));
                b.setOccupiedCount(rs.getInt("occupied_count"));
                list.add(b);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StatDao.selectBuildings失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 学生对象(带住宿位置)
     */
    private Student mapStudent(ResultSet rs) throws SQLException {
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
