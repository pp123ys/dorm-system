package net.wanhe.dormsystem.service.impl;

import net.wanhe.dormsystem.dao.StuDao;
import net.wanhe.dormsystem.exception.StuException;
import net.wanhe.dormsystem.pojo.Student;
import net.wanhe.dormsystem.service.StuService;

import java.util.List;

public class StuServiceImpl implements StuService {

    private StuDao stuDao;

    public StuServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dormsystem.dao.impl.StuDaoImpl");
            stuDao = (StuDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建StuDao失败", e);
        }
    }

    @Override
    public List<Student> list() {
        return stuDao.selectAll();
    }

    @Override
    public void add(Student student) throws StuException {
        checkName(student.getName());
        checkSex(student.getSex());
        checkAge(student.getAge());
        checkPhone(student.getPhone());
        if (stuDao.selectByNo(student.getNo()) != null) {
            throw new StuException("该学号已存在");
        }
        stuDao.insert(student);
    }

    @Override
    public void update(Student student) throws StuException {
        Student old = stuDao.selectByNo(student.getNo());
        if (old == null) {
            throw new StuException("该学号的学生不存在");
        }
        checkName(student.getName());
        checkSex(student.getSex());
        checkAge(student.getAge());
        checkPhone(student.getPhone());
        stuDao.update(student);
    }

    @Override
    public void delete(int no) throws StuException {
        Student old = stuDao.selectByNo(no);
        if (old == null) {
            throw new StuException("该学号的学生不存在");
        }
        if (old.isCheckedIn()) {
            throw new StuException("该学生正在" + old.location() + ", 请先办理退住");
        }
        stuDao.delete(old.getId());
    }

    private void checkName(String name) throws StuException {
        if (name == null || name.trim().isEmpty()) {
            throw new StuException("姓名不能为空");
        }
        if (name.length() > 50) {
            throw new StuException("姓名不能超过50个字符");
        }
    }

    private void checkSex(String sex) throws StuException {
        if (!"男".equals(sex) && !"女".equals(sex)) {
            throw new StuException("性别只能是 男 或 女");
        }
    }

    private void checkAge(Integer age) throws StuException {
        if (age != null && (age < 10 || age > 100)) {
            throw new StuException("年龄需在10~100之间");
        }
    }

    private void checkPhone(String phone) throws StuException {
        if (phone != null && phone.length() > 20) {
            throw new StuException("电话不能超过20个字符");
        }
    }
}
