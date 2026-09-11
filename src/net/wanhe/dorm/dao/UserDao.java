package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.User;

public interface UserDao {

    /*
     * 按登录名查询管理员, 不存在返回 null
     */
    User selectByLoginName(String loginName);
}
