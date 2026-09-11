package net.wanhe.edusystem.controller;

import net.wanhe.edusystem.exception.UserException;
import net.wanhe.edusystem.service.UserService;
import net.wanhe.edusystem.service.impl.UserServiceImpl;
import net.wanhe.edusystem.util.ScannerUtil;

import java.util.Scanner;

public class UserController {

    //统一使用共享的Scanner 避免多个Scanner抢占System.in的缓冲
    Scanner sc = ScannerUtil.SC;

    UserService userService = new UserServiceImpl();

    public int print(){
        System.out.println("--登陆注册--");
        System.out.println("1.注册");
        System.out.println("2.登录");
        System.out.println("请选择:");
        return sc.nextInt();
    }

    /*
     * 注册功能
     */
    public void regist(){
        System.out.println("请输入用户名:");
        String loginName = sc.next();
        System.out.println("请输入密码:");
        String password = sc.next();
        System.out.println("请确认密码:");
        String repassword = sc.next();
        try {
            userService.regist(loginName,password,repassword);
            System.out.println("注册成功");
        } catch (UserException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 登录
     */
    public boolean login(){
        System.out.println("请输入用户名:");
        String loginName = sc.next();
        System.out.println("请输入密码:");
        String password = sc.next();
        try {
            userService.login(loginName,password);
            return true;
        } catch (UserException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
