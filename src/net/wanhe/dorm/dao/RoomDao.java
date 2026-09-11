package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Room;

import java.util.List;

public interface RoomDao {

    /*
     * 某楼栋下的房间列表, 带楼栋名与已住人数(展示字段)
     */
    List<Room> selectByBuildingId(int buildingId);

    Room selectById(int id);

    /*
     * 同楼栋内按房间号查询, 用于唯一性校验, 不存在返回 null
     */
    Room selectByBuildingAndNo(int buildingId, String roomNo);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Room room);

    /*
     * 全字段更新(按id), 用于改状态
     */
    int update(Room room);

    /*
     * 只更新床位数
     */
    int updateCapacity(int roomId, int capacity);

    int delete(int id);

    /*
     * 该楼栋下的房间数, 用于删除楼栋前的校验
     */
    int countByBuildingId(int buildingId);

    /*
     * 该房间的在住学生数
     */
    int countOccupied(int roomId);
}
