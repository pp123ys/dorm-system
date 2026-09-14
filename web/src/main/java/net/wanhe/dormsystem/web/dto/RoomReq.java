package net.wanhe.dormsystem.web.dto;

/*
 * 新增房间请求体（容量决定自动生成几个床位）
 */
public class RoomReq {

    private Integer buildingId;
    private String roomNo;
    private Integer capacity;

    public Integer getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Integer buildingId) {
        this.buildingId = buildingId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
