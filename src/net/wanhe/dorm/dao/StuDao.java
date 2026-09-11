package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StuDao {

    /*
     * 全部学生(按学号), 带住宿位置展示字段
     */
    List<Student> selectAll();

    Student selectById(int id);

    /*
     * 按学号查询(业务主键), 不存在返回 null
     */
    Student selectByNo(int no);

    /*
     * 按床位查询在住学生, 该床位无人住返回 null
     */
    Student selectByBedId(int bedId);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Student student);

    /*
     * 只更新 姓名/性别/年龄/电话(按学号定位), 不碰 bed_id
     */
    int update(Student student);

    int delete(int id);

    /*
     * 更新入住床位, bedId 为 null 表示退住
     */
    int updateBedId(int studentId, Integer bedId);
}
