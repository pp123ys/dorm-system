package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StayService {

    /*
     * 还有空床位的楼栋
     */
    List<Building> buildingsWithFreeBed();

    /*
     * 某楼栋的可分配床位(床位正常+房间正常+无人住)
     */
    List<Bed> freeBeds(int buildingId);

    /*
     * 查学生住宿信息, 学生不存在时抛异常
     */
    Student stayInfo(int studentNo) throws StayException;

    /*
     * 入住前校验: 学生不存在或已入住时抛异常, 否则返回学生
     * 先拦一道, 避免让用户白选一轮楼栋与床位
     */
    Student checkInTarget(int studentNo) throws StayException;

    /*
     * 办理入住: 全部校验通过后, 同事务内 改bed_id + 写入住流水
     */
    void checkIn(int studentNo, int bedId, String operator) throws StayException;

    /*
     * 办理退住: 同事务内 清bed_id + 写退住流水(记录退住前的位置)
     */
    void checkOut(int studentNo, String operator) throws StayException;
}
