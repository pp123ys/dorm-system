package net.wanhe.edusystem.service;

import net.wanhe.edusystem.exception.UserException;

public interface UserService {

    /*
     * 注册
     */
    void regist(String loginName,String password,String repassword) throws UserException;

    /*
     * 登录
     */
    void login(String loginName,String password) throws UserException;

}
