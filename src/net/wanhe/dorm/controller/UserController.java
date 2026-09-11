package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.UserException;
import net.wanhe.dorm.pojo.User;
import net.wanhe.dorm.service.UserService;
import net.wanhe.dorm.service.impl.UserServiceImpl;
import net.wanhe.dorm.util.LoginContext;
import net.wanhe.dorm.util.ScannerUtil;

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
