package net.wanhe.dormsystem.pojo;

public class Room {

    private int id;

    private int buildingId;

    private String roomNo;

    private int capacity;

    private String status;

    //以下两个字段仅查询展示用, 不参与 insert/update
    private String buildingName;

    private int occupiedCount;

    public Room() {
    }

    public Room(int buildingId, String roomNo, int capacity, String status) {
        this.buildingId = buildingId;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    //空床位数(展示用)
    public int getFreeCount() {
        return capacity - occupiedCount;
    }
}
