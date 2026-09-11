package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BuildingDaoImpl implements BuildingDao {

    //查询列, 各查询共用
    private static final String COLS = "id,name,sex,floors,remark";

    @Override
    public List<Building> selectAll() {
        List<Building> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building order by id");
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectAll失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Building selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building where id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Building selectByName(String name) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building where name = ?");
            state.setString(1, name);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectByName失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Building building) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_building(name,sex,floors,remark) values(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setString(1, building.getName());
            state.setString(2, building.getSex());
            state.setInt(3, building.getFloors());
            state.setString(4, building.getRemark());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Building building) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_building set name=?,sex=?,floors=?,remark=? where id=?");
            state.setString(1, building.getName());
            state.setString(2, building.getSex());
            state.setInt(3, building.getFloors());
            state.setString(4, building.getRemark());
            state.setInt(5, building.getId());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.update失败", e);
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
            state = conn.prepareStatement("delete from t_building where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Building map(ResultSet rs) throws SQLException {
        Building b = new Building();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        b.setSex(rs.getString("sex"));
        b.setFloors(rs.getInt("floors"));
        b.setRemark(rs.getString("remark"));
        return b;
    }
}
