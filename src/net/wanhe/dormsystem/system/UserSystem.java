package net.wanhe.dormsystem.system;

import net.wanhe.dormsystem.controller.UserController;

/*
 * 登录流程: 失败就一直重新输入, 成功才返回
 */
public class UserSystem {

    private UserController userController = new UserController();

    public void run() {
        while (!userController.login()) {
            System.out.println("请重新登录");
        }
    }
}
