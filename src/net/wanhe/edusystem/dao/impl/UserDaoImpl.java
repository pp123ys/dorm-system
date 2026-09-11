package net.wanhe.edusystem.dao.impl;

import net.wanhe.edusystem.dao.UserDao;
import net.wanhe.edusystem.pojo.User;
import net.wanhe.edusystem.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDaoImpl implements UserDao {

    @Override
    public User selectByLoginName(String loginName) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "select login_name,password from t_user where login_name = ?";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setString(1, loginName);
            //6.执行sql语句
            rs = state.executeQuery();
            //7.处理结果集
            if(rs.next()){
                String name = rs.getString("login_name");
                String password = rs.getString("password");
                return new User(name,password);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("UserDao.selectByLoginName失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(rs,state,conn);
        }
    }

    @Override
    public void insert(User user) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "insert into t_user(login_name,password) values(?,?)";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setString(1, user.getLoginName());
            state.setString(2, user.getPassword());
            //6.执行sql语句
            state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("UserDao.insert失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(null,state,conn);
        }
    }
}
