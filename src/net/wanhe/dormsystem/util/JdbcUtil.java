package net.wanhe.dormsystem.util;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * JDBC 工具类: 集中处理 连接数据库 / 关闭资源 / 事务
 *
 * 双模式设计（同一个类同时服务控制台版与 Web 版）：
 *
 * 1) 控制台版（Run.main）：不注入 DataSource。
 *    数据源用下面硬编码的连接信息自建 DriverManagerDataSource，
 *    事务由 beginTransaction / commit / rollbackQuietly 手动控制，
 *    行为与改造前一致；Dao 与 Service 代码无需任何改动。
 *
 * 2) Web 版（Spring Boot）：由 web/config/DataSourceInitializer 调用 setDataSource()
 *    注入 Spring 管理的连接池。连接统一走 DataSourceUtils.getConnection()，
 *    因此会自动加入 Spring 声明式事务（Web 侧事务边界在 service/TxService 上声明）。
 */
public class JdbcUtil {

    //控制台版的默认连接信息（Web 版会通过 setDataSource 覆盖, 见 web/src/main/resources/application.yml）
    private static final String URL = "jdbc:mysql://localhost:3306/dorm_system?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    //由 Spring 注入; 为 null 表示运行在控制台模式
    private static volatile DataSource dataSource;

    //控制台模式下的自建数据源(懒加载)
    private static volatile DataSource defaultDataSource;

    //手动事务管理器(控制台模式使用; Web 模式走 @Transactional, 不会走到这里)
    private static volatile DataSourceTransactionManager txManager;

    //当前线程的手动事务状态, 为 null 表示当前不在本类开启的事务中
    private static final ThreadLocal<TransactionStatus> TX = new ThreadLocal<>();

    static {
        //1.注册驱动 (MySQL 8 的驱动类名是 com.mysql.cj.jdbc.Driver)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 注入 Spring 管理的 DataSource（由 web/config/DataSourceInitializer 在启动时调用）
     */
    public static void setDataSource(DataSource ds) {
        dataSource = ds;
        txManager = null;
    }

    /*
     * 当前生效的数据源：Web 模式用 Spring 注入的, 控制台模式用自建的
     */
    private static DataSource ds() {
        DataSource injected = dataSource;
        if (injected != null) {
            return injected;
        }
        if (defaultDataSource == null) {
            synchronized (JdbcUtil.class) {
                if (defaultDataSource == null) {
                    DriverManagerDataSource d = new DriverManagerDataSource();
                    d.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    d.setUrl(URL);
                    d.setUsername(USERNAME);
                    d.setPassword(PASSWORD);
                    defaultDataSource = d;
                }
            }
        }
        return defaultDataSource;
    }

    private static DataSourceTransactionManager txManager() {
        DataSourceTransactionManager m = txManager;
        if (m == null) {
            synchronized (JdbcUtil.class) {
                if (txManager == null) {
                    txManager = new DataSourceTransactionManager(ds());
                }
                m = txManager;
            }
        }
        return m;
    }

    /*
     * 2.获取一个对数据库的连接对象
     * 若当前线程已在事务中, 返回同一个连接, 保证多条SQL在同一个事务里
     */
    public static Connection getConnection() throws SQLException {
        return DataSourceUtils.getConnection(ds());
    }

    /*
     * 开启事务（控制台模式使用; Web 模式请用 @Transactional）
     * 不支持嵌套: 已有事务时直接报错, 避免旧连接被孤儿化导致静默丢失
     */
    public static void beginTransaction() {
        if (TX.get() != null) {
            throw new IllegalStateException("事务已开启, 不支持嵌套开启事务");
        }
        try {
            TX.set(txManager().getTransaction(new DefaultTransactionDefinition()));
        } catch (RuntimeException e) {
            throw new RuntimeException("开启事务失败", e);
        }
    }

    /*
     * 提交事务并归还连接
     */
    public static void commit() {
        TransactionStatus status = TX.get();
        if (status == null) {
            return;
        }
        try {
            txManager().commit(status);
        } catch (RuntimeException e) {
            throw new RuntimeException("提交事务失败", e);
        } finally {
            TX.remove();
        }
    }

    /*
     * 回滚事务并归还连接
     * 回滚自身失败只打印堆栈, 不掩盖真正的业务异常
     */
    public static void rollbackQuietly() {
        TransactionStatus status = TX.get();
        if (status == null) {
            return;
        }
        try {
            txManager().rollback(status);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            TX.remove();
        }
    }

    /*
     * 8.关闭资源
     * 连接交给 DataSourceUtils 按事务状态决定归还还是关闭(事务中不真正关闭)
     */
    public static void close(ResultSet rs, PreparedStatement state, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (state != null) {
                state.close();
            }
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, ds());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
