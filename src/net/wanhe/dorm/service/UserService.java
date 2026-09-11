package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.UserException;
import net.wanhe.dorm.pojo.User;

public interface UserService {

    /*
     * 登录, 用户名或密码错误时抛 UserException
     */
    User login(String loginName, String password) throws UserException;
}
