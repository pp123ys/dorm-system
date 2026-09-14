package net.wanhe.dormsystem.web.controller;

import net.wanhe.dormsystem.exception.UserException;
import net.wanhe.dormsystem.pojo.User;
import net.wanhe.dormsystem.service.UserService;
import net.wanhe.dormsystem.service.impl.UserServiceImpl;
import net.wanhe.dormsystem.web.auth.TokenStore;
import net.wanhe.dormsystem.web.common.R;
import net.wanhe.dormsystem.web.dto.LoginReq;
import net.wanhe.dormsystem.web.dto.LoginView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * 登录/登出/当前用户。
 * 复用既有 UserService.login（不新增用户表逻辑），token 由 TokenStore 发。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService = new UserServiceImpl();
    private final TokenStore tokenStore;

    public AuthController(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public R<LoginView> login(@RequestBody LoginReq req) throws UserException {
        if (req == null || req.getLoginName() == null || req.getLoginName().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        User user = userService.login(req.getLoginName().trim(), req.getPassword());
        String token = tokenStore.issue(user.getLoginName());
        return R.ok(new LoginView(token, user.getLoginName()));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        tokenStore.remove(token);
        return R.ok();
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me(@RequestHeader(value = "X-Token", required = false) String token) {
        TokenStore.LoginUser user = tokenStore.get(token);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("loginName", user == null ? "" : user.getLoginName());
        return R.ok(info);
    }
}
