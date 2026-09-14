package net.wanhe.dormsystem.web.auth;

import net.wanhe.dormsystem.util.LoginContext;
import net.wanhe.dormsystem.web.common.UnauthorizedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * 鉴权拦截器：
 *   1) 校验 X-Token（兼容 Authorization: Bearer <token>）
 *   2) 校验通过后把登录名放进 LoginContext，使入住/退住流水的 operator 字段来源不变
 *   3) 请求结束务必 clear()，因为 Tomcat 线程是复用的，不清理会串号
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_TOKEN = "X-Token";
    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER = "Bearer ";

    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行浏览器的 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(HEADER_TOKEN);
        if (token == null || token.isEmpty()) {
            String auth = request.getHeader(HEADER_AUTH);
            if (auth != null && auth.startsWith(BEARER)) {
                token = auth.substring(BEARER.length()).trim();
            }
        }

        TokenStore.LoginUser user = tokenStore.get(token);
        if (user == null) {
            throw new UnauthorizedException("登录已失效, 请重新登录");
        }

        LoginContext.setCurrentUser(user.getLoginName());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        LoginContext.clear();
    }
}
