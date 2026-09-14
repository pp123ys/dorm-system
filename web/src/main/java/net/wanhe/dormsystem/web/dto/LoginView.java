package net.wanhe.dormsystem.web.dto;

/*
 * 登录成功返回：token + 登录名
 */
public class LoginView {

    private final String token;
    private final String loginName;

    public LoginView(String token, String loginName) {
        this.token = token;
        this.loginName = loginName;
    }

    public String getToken() {
        return token;
    }

    public String getLoginName() {
        return loginName;
    }
}
