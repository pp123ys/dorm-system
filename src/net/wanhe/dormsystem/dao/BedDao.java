package net.wanhe.dormsystem.dao;

import net.wanhe.dormsystem.pojo.Bed;

import java.util.List;

public interface BedDao {

    /*
     * 某房间的全部床位(按床号), 带楼栋/房间/在住学生展示字段
     */
    List<Bed> selectByRoomId(int roomId);

    /*
     * 按id查询, 带展示字段(占用判断依赖它)
     */
    Bed selectById(int id);

    /*
     * 可分配的床位: 床位正常 + 房间正常 + 无人住
     * buildingId 为 null 表示查全部楼栋
     */
    List<Bed> selectFreeBeds(Integer buildingId);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Bed bed);

    int delete(int id);

    /*
     * 删除某房间的全部床位(删房间时用)
     */
    int deleteByRoomId(int roomId);

    /*
     * 删除某房间 bed_no 大于 keepCount 且无人住的床位(缩容时用)
     */
    int deleteFreeBedsAbove(int roomId, int keepCount);

    /*
     * 某房间 bed_no 大于 keepCount 的床位上的在住学生数(缩容前的校验)
     */
    int countOccupiedAbove(int roomId, int keepCount);

    int updateStatus(int id, String status);
}
