package net.wanhe.edusystem.dao;

import net.wanhe.edusystem.pojo.User;

public interface UserDao {

    /*
     * 根据用户名查询用户
     */
    User selectByLoginName(String loginName);

    /*
     * 添加用户
     */
    void insert(User user);

}
