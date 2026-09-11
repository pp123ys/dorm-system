package net.wanhe.edusystem.dao.impl;

import net.wanhe.edusystem.dao.ClazzDao;
import net.wanhe.edusystem.pojo.Clazz;
import net.wanhe.edusystem.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClazzDaoImpl implements ClazzDao {

    @Override
    public List<Clazz> select() {
        List<Clazz> clazzes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "select name,`count` from t_clazz";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //6.执行sql语句
            rs = state.executeQuery();
            //7.处理结果集
            while(rs.next()){
                String name = rs.getString("name");
                int count = rs.getInt("count");
                clazzes.add(new Clazz(name,count));
            }
            return clazzes;
        } catch (Exception e) {
            throw new RuntimeException("ClazzDao.select失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(rs,state,conn);
        }
    }

    @Override
    public Clazz selectByName(String name) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "select name,`count` from t_clazz where name = ?";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setString(1, name);
            //6.执行sql语句
            rs = state.executeQuery();
            //7.处理结果集
            if(rs.next()){
                String clazzName = rs.getString("name");
                int count = rs.getInt("count");
                return new Clazz(clazzName,count);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("ClazzDao.selectByName失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(rs,state,conn);
        }
    }

    @Override
    public void insert(Clazz clazz) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "insert into t_clazz(name,`count`) values(?,?)";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setString(1, clazz.getName());
            state.setInt(2, clazz.getCount());
            //6.执行sql语句
            state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ClazzDao.insert失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(null,state,conn);
        }
    }

    @Override
    public void delete(String name) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "delete from t_clazz where name = ?";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setString(1, name);
            //6.执行sql语句
            state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("ClazzDao.delete失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(null,state,conn);
        }
    }
}
