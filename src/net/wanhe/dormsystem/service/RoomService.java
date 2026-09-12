package net.wanhe.dormsystem.service;

import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.pojo.Bed;
import net.wanhe.dormsystem.pojo.Room;

import java.util.List;

public interface RoomService {

    List<Room> listByBuilding(int buildingId) throws RoomException;

    List<Bed> listBeds(int roomId);

    /*
     * 新增房间并自动生成 1~capacity 号床位
     */
    void add(Room room) throws RoomException;

    void updateCapacity(int roomId, int newCapacity) throws RoomException;

    void updateRoomStatus(int roomId, String status) throws RoomException;

    void updateBedStatus(int bedId, String status) throws RoomException;

    /*
     * 删除房间并连带删除其床位(房间内有在住学生时拒绝)
     */
    void delete(int roomId) throws RoomException;
}
