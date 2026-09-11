package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

/*
 * 跨表聚合查询专用 Dao
 * 单表的增删改查留在各自的 Dao 里, 不在这里重复
 */
public interface StatDao {

    /*
     * 楼栋占用概览: 房间数/可分配床位数/已住人数
     */
    List<Building> selectBuildingOverview();

    /*
     * 还有空床位的楼栋(办理入住时先列这个)
     */
    List<Building> selectBuildingsWithFreeBed();

    /*
     * 某房间的在住学生名单
     */
    List<Student> selectStudentsByRoom(int roomId);
}
