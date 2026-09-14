package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.util.JdbcUtil;
import net.wanhe.dormsystem.web.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * 健康检查：不鉴权，用于确认服务与数据库都可用。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public R<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "up");
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("select count(*) from t_building");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            info.put("database", "up");
            info.put("buildings", rs.getInt(1));
        } catch (Exception e) {
            info.put("status", "degraded");
            info.put("database", "down: " + e.getMessage());
        }
        return R.ok(info);
    }
}
