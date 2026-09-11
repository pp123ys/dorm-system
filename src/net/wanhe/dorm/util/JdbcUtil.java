package net.wanhe.dorm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * JDBC 工具类: 集中处理 连接数据库 / 关闭资源 / 事务
 * 事务连接用 ThreadLocal 持有, 事务中的各 Dao 自动复用同一个连接,
 * 因此 Dao 的方法签名不需要为事务做任何改动
 */
public class JdbcUtil {

    //连接数据库的固定信息
    private static final String URL = "jdbc:mysql://localhost:3306/rg01?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    //当前线程的事务连接, 为 null 表示当前不在事务中
    private static final ThreadLocal<Connection> TX = new ThreadLocal<>();

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
     * 事务中返回的是同一个连接, 保证多条SQL在同一个事务里
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = TX.get();
        if (conn != null) {
            return conn;
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /*
     * 开启事务
     * 不支持嵌套: 已有事务时直接报错, 避免旧连接被孤儿化导致静默丢失
     */
    public static void beginTransaction() {
        if (TX.get() != null) {
            throw new IllegalStateException("事务已开启, 不支持嵌套开启事务");
        }
        try {
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            conn.setAutoCommit(false);
            TX.set(conn);
        } catch (SQLException e) {
            throw new RuntimeException("开启事务失败", e);
        }
    }

    /*
     * 提交事务并归还连接
     */
    public static void commit() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("提交事务失败", e);
        } finally {
            TX.remove();
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 回滚事务并归还连接
     * 回滚自身失败只打印堆栈, 不掩盖真正的业务异常
     */
    public static void rollbackQuietly() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            TX.remove();
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 8.关闭资源
     * 事务中的连接不在这里关闭, 由 commit / rollbackQuietly 统一归还
     */
    public static void close(ResultSet rs, PreparedStatement state, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (state != null) {
                state.close();
            }
            if (conn != null && TX.get() == null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
