package net.wanhe.dormsystem.web.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 简化版登录态：token 存内存，服务重启即全部失效。
 * 演示用足够；要持久化/集群可换成 JWT 或 Redis（见 WEB.md 的"已知限制"）。
 */
@Component
public class TokenStore {

    public static class LoginUser {
        private final String loginName;
        private final long loginAt;

        public LoginUser(String loginName) {
            this.loginName = loginName;
            this.loginAt = System.currentTimeMillis();
        }

        public String getLoginName() {
            return loginName;
        }

        public long getLoginAt() {
            return loginAt;
        }
    }

    private final Map<String, LoginUser> tokens = new ConcurrentHashMap<>();

    public String issue(String loginName) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new LoginUser(loginName));
        return token;
    }

    /*
     * 返回 null 表示 token 不存在/已失效
     */
    public LoginUser get(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return tokens.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
