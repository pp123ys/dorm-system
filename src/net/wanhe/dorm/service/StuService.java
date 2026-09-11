package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StuService {

    List<Student> list();

    void add(Student student) throws StuException;

    /*
     * 按学号定位修改, 学号本身不可改
     */
    void update(Student student) throws StuException;

    /*
     * 按学号删除(在住学生不允许删除)
     */
    void delete(int no) throws StuException;
}
