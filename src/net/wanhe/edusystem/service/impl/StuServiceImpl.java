package net.wanhe.edusystem.service.impl;

import net.wanhe.edusystem.dao.ClazzDao;
import net.wanhe.edusystem.dao.StuDao;
import net.wanhe.edusystem.exception.StuException;
import net.wanhe.edusystem.pojo.Clazz;
import net.wanhe.edusystem.pojo.Student;
import net.wanhe.edusystem.service.StuService;

import java.util.List;

public class StuServiceImpl implements StuService {

    StuDao stuDao;

    ClazzDao clazzDao;

    public StuServiceImpl() {
        /*
         * 反射 通过类名创建Dao对象
         * 服务层与Dao实现类解耦
         */
        try {
            Class c1 = Class.forName("net.wanhe.edusystem.dao.impl.StuDaoImpl");
            stuDao = (StuDao) c1.newInstance();
            Class c2 = Class.forName("net.wanhe.edusystem.dao.impl.ClazzDaoImpl");
            clazzDao = (ClazzDao) c2.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建Dao失败", e);
        }
    }

    @Override
    public List<Student> find() {
        return stuDao.select();
    }

    @Override
    public void add(int no, String name, int age, String phone,String clazzName) throws StuException {
        Student s = stuDao.selectByNo(no);
        if(s != null){
            throw new StuException("该学号已被使用");
        }
        //获取班级信息
        Clazz c = clazzDao.selectByName(clazzName);
        List<Student> stus = stuDao.selectByClazzName(clazzName);
        if(c == null){
            throw new StuException("班级不存在");
        }
        if(stus.size() == c.getCount()){
            throw new StuException("班级已满");
        }
        Student stu = new Student(no,name,age,phone,c);
        stuDao.insert(stu);
    }

    @Override
    public void del(int no) throws StuException {
        Student s = stuDao.selectByNo(no);
        if(s == null){
            throw new StuException("该学号不存在");
        }
        stuDao.delete(no);

    }
}
