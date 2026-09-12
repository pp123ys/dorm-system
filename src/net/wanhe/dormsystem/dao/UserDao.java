package net.wanhe.dormsystem.dao;

import net.wanhe.dormsystem.pojo.User;

public interface UserDao {

    /*
     * 按登录名查询管理员, 不存在返回 null
     */
    User selectByLoginName(String loginName);
}
