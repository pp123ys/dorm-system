package net.wanhe.dormsystem.web.config;

import net.wanhe.dormsystem.util.JdbcUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/*
 * 把 Spring 管理的 DataSource 交给 JdbcUtil。
 *
 * 现有 Dao 都是通过 JdbcUtil.getConnection() 取连接的，JdbcUtil 是静态工具类，
 * 无法用 @Autowired 注入，所以在启动时由这里把 DataSource 交进去。
 * 之后 JdbcUtil 走 DataSourceUtils，天然参与 Spring 声明式事务：
 *   - Web 模式：TxService 上的 @Transactional 生效
 *   - 控制台模式：JdbcUtil 未被注入，回退到自建 DriverManagerDataSource，行为不变
 */
@Component
public class DataSourceInitializer {

    private final DataSource dataSource;

    public DataSourceInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        JdbcUtil.setDataSource(dataSource);
    }
}
