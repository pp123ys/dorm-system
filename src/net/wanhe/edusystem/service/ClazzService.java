package net.wanhe.edusystem.service;

import net.wanhe.edusystem.exception.ClazzException;
import net.wanhe.edusystem.pojo.Clazz;

import java.util.List;

public interface ClazzService {
    /*
     * 获取班级
     */
    List<Clazz> find();

    /*
     * 添加班级
     */
    void add(String name, int count) throws ClazzException;

    /*
     * 删除班级
     */
    void del(String name) throws ClazzException;
}
