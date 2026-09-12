package net.wanhe.dormsystem.service;

import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.exception.StuException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Building;
import net.wanhe.dormsystem.pojo.Checkin;
import net.wanhe.dormsystem.pojo.Student;

import java.util.List;

public interface StatService {

    /*
     * 楼栋占用概览
     */
    List<Building> buildingOverview();

    /*
     * 某房间在住学生名单, 房间不存在时抛异常
     */
    List<Student> roomRoster(int roomId) throws RoomException;

    /*
     * 空床位清单, buildingId 为 null 表示全部楼栋
     */
    List<Bed> freeBeds(Integer buildingId);

    /*
     * 学生住宿信息, 学生不存在时抛异常
     */
    Student studentStay(int studentNo) throws StuException;

    /*
     * 入住退住流水, studentNo 为 null 表示全部学生
     */
    List<Checkin> checkinHistory(Integer studentNo);
}
