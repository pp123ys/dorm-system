package net.wanhe.dormsystem.service;

import net.wanhe.dormsystem.exception.UserException;
import net.wanhe.dormsystem.pojo.User;

public interface UserService {

    /*
     * 登录, 用户名或密码错误时抛 UserException
     */
    User login(String loginName, String password) throws UserException;
}
