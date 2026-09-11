package net.wanhe.edusystem.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * JDBC 工具类: 集中处理 连接数据库 / 关闭资源
 * 各Dao实现类通过它获取连接, 不再各自重复连接代码
 */
public class JdbcUtil {

    //连接数据库的固定信息
    private static final String URL = "jdbc:mysql://localhost:3306/rg01?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    static {
        //1.注册驱动 (MySQL 8 的驱动类名是 com.mysql.cj.jdbc.Driver)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 2.获取一个对数据库的连接对象
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /*
     * 8.关闭资源
     */
    public static void close(ResultSet rs, PreparedStatement state, Connection conn) {
        try {
            if(rs != null){
                rs.close();
            }
            if(state != null){
                state.close();
            }
            if(conn != null){
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
