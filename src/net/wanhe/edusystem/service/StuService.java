package net.wanhe.edusystem.service;

import net.wanhe.edusystem.exception.StuException;
import net.wanhe.edusystem.pojo.Student;

import java.util.List;

public interface StuService {

    /*
     * 查看学生信息
     */
    List<Student> find();

    /*
     * 添加学生
     */
    void add(int no, String name, int age, String phone,String clazzName) throws StuException;

    /*
     * 删除学生
     */
    void del(int no) throws StuException;
}
