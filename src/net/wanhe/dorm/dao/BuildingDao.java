package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Building;

import java.util.List;

public interface BuildingDao {

    List<Building> selectAll();

    Building selectById(int id);

    /*
     * 按名称查询, 不存在返回 null
     */
    Building selectByName(String name);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Building building);

    /*
     * 修改(按id), 返回受影响行数
     */
    int update(Building building);

    int delete(int id);
}
