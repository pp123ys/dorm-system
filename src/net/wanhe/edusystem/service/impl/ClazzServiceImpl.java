package net.wanhe.edusystem.service.impl;

import net.wanhe.edusystem.dao.ClazzDao;
import net.wanhe.edusystem.exception.ClazzException;
import net.wanhe.edusystem.pojo.Clazz;
import net.wanhe.edusystem.service.ClazzService;

import java.util.List;

public class ClazzServiceImpl implements ClazzService {

    private ClazzDao clazzDao;

    public ClazzServiceImpl() {
        /*
         * 反射 通过类名创建Dao对象
         * 服务层与Dao实现类解耦
         */
        try {
            Class c = Class.forName("net.wanhe.edusystem.dao.impl.ClazzDaoImpl");
            clazzDao = (ClazzDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建ClazzDao失败", e);
        }
    }

    @Override
    public List<Clazz> find() {
        return clazzDao.select();
    }

    @Override
    public void add(String name, int count) throws ClazzException {
        Clazz c = clazzDao.selectByName(name);
        if(c != null){
            throw new ClazzException("该班级已存在");
        }
        Clazz clazz = new Clazz(name,count);
        clazzDao.insert(clazz);
    }

    @Override
    public void del(String name) throws ClazzException {
        Clazz c = clazzDao.selectByName(name);
        if(c == null){
            throw new ClazzException("该班级不存在");
        }
        clazzDao.delete(name);
    }
}
