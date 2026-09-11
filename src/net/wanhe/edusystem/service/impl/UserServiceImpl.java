package net.wanhe.edusystem.service.impl;

import net.wanhe.edusystem.dao.UserDao;
import net.wanhe.edusystem.exception.UserException;
import net.wanhe.edusystem.pojo.User;
import net.wanhe.edusystem.service.UserService;

public class UserServiceImpl implements UserService {

    private UserDao userDao;

    public UserServiceImpl() {
        /*
         * 反射
         * 在架构过程中 我的代码中知道了要创建对象的类名 但是不能直接使用该名字创建对象
         */
        try {
            Class c = Class.forName("net.wanhe.edusystem.dao.impl.UserDaoImpl");
            userDao = (UserDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建UserDao失败", e);
        }
    }

    @Override
    public void regist(String loginName, String password, String repassword) throws UserException {
        if(!password.equals(repassword)){
            throw new UserException("两次密码不一致");
        }
        User user = userDao.selectByLoginName(loginName);
        if(user != null){
            throw new UserException("该账号已注册");
        }
        User u = new User(loginName,password);
        userDao.insert(u);
    }

    @Override
    public void login(String loginName, String password) throws UserException {
        User user = userDao.selectByLoginName(loginName);
        if(user == null || !user.getPassword().equals(password)){
            throw new UserException("用户名或密码错误");
        }
    }
}
