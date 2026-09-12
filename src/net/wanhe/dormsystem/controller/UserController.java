package net.wanhe.dormsystem.controller;

import net.wanhe.dormsystem.exception.UserException;
import net.wanhe.dormsystem.pojo.User;
import net.wanhe.dormsystem.service.UserService;
import net.wanhe.dormsystem.service.impl.UserServiceImpl;
import net.wanhe.dormsystem.util.LoginContext;
import net.wanhe.dormsystem.util.ScannerUtil;

public class UserController {

    private UserService userService = new UserServiceImpl();

    /*
     * 登录: 成功返回 true 并记录当前登录名, 失败返回 false 由 UserSystem 重试
     */
    public boolean login() {
        String loginName = ScannerUtil.nextNonEmpty("请输入用户名:");
        String password = ScannerUtil.nextNonEmpty("请输入密码:");
        try {
            User user = userService.login(loginName, password);
            LoginContext.setCurrentUser(user.getLoginName());
            System.out.println("登录成功");
            return true;
        } catch (UserException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
