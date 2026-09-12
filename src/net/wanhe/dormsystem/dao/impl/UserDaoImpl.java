package net.wanhe.dormsystem.dao.impl;

import net.wanhe.dormsystem.dao.UserDao;
import net.wanhe.dormsystem.pojo.User;
import net.wanhe.dormsystem.util.JdbcUtil;

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
            //1.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //2.定义SQL语句并预编译
            String sql = "select id,login_name,password from t_user where login_name = ?";
            state = conn.prepareStatement(sql);
            //3.为?赋值
            state.setString(1, loginName);
            //4.执行查询
            rs = state.executeQuery();
            //5.处理结果集
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLoginName(rs.getString("login_name"));
                user.setPassword(rs.getString("password"));
                return user;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("UserDao.selectByLoginName失败", e);
        } finally {
            //6.关闭资源
            JdbcUtil.close(rs, state, conn);
        }
    }
}
