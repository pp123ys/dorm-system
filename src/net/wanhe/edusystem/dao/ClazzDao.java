package net.wanhe.edusystem.dao;

import net.wanhe.edusystem.pojo.Clazz;

import java.util.List;

public interface ClazzDao {

    /*
     * 获取班级
     */
    List<Clazz> select();

    /*
     * 根据名称查询班级
     */
    Clazz selectByName(String name);

    /*
     * 添加班级
     */
    void insert(Clazz clazz);

    /*
     * 删除班级
     */
    void delete(String name);
}
