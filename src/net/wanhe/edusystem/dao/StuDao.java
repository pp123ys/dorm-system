package net.wanhe.edusystem.dao;

import net.wanhe.edusystem.pojo.Student;

import java.util.List;

public interface StuDao {

    /*
     * 获取信息
     */
    List<Student> select();

    /*
     * 根据学号查询
     */
    Student selectByNo(int no);

    /*
     * 添加
     */
    void insert(Student stu);

    /*
     * 删除
     */
    void delete(int no);

    /*
     * 查询某个班级所有学生信息
     */
    List<Student> selectByClazzName(String clazzName);
}
