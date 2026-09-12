package net.wanhe.dormsystem.service.impl;

import net.wanhe.dormsystem.dao.UserDao;
import net.wanhe.dormsystem.exception.UserException;
import net.wanhe.dormsystem.pojo.User;
import net.wanhe.dormsystem.service.UserService;

public class UserServiceImpl implements UserService {

    private UserDao userDao;

    public UserServiceImpl() {
        /*
         * 反射: 通过类名创建Dao对象
         * 服务层与Dao实现类解耦(与现有 UserServiceImpl/ClazzServiceImpl 写法一致)
         */
        try {
            Class c = Class.forName("net.wanhe.dormsystem.dao.impl.UserDaoImpl");
            userDao = (UserDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建UserDao失败", e);
        }
    }

    @Override
    public User login(String loginName, String password) throws UserException {
        User user = userDao.selectByLoginName(loginName);
        if (user == null || !user.getPassword().equals(password)) {
            throw new UserException("用户名或密码错误");
        }
        return user;
    }
}
