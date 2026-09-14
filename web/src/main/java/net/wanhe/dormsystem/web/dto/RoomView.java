package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Room;

/*
 * 房间视图：补上 POJO 里不是 getter 的 freeCount（空床数）。
 */
public class RoomView {

    private final int id;
    private final int buildingId;
    private final String buildingName;
    private final String roomNo;
    private final int capacity;
    private final String status;
    private final int occupiedCount;
    private final int freeCount;

    public RoomView(Room r) {
        this.id = r.getId();
        this.buildingId = r.getBuildingId();
        this.buildingName = r.getBuildingName();
        this.roomNo = r.getRoomNo();
        this.capacity = r.getCapacity();
        this.status = r.getStatus();
        this.occupiedCount = r.getOccupiedCount();
        this.freeCount = r.getFreeCount();
    }

    public int getId() {
        return id;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getStatus() {
        return status;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public int getFreeCount() {
        return freeCount;
    }
}
