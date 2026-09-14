package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Bed;

/*
 * 床位视图：POJO 的 isOccupied() 会被序列化成 occupied，
 * 但 location() 不是 getter，这里补上可读位置文案。
 */
public class BedView {

    private final int id;
    private final int roomId;
    private final String buildingName;
    private final String roomNo;
    private final int bedNo;
    private final String status;
    private final boolean occupied;
    private final Integer studentNo;
    private final String studentName;

    public BedView(Bed b) {
        this.id = b.getId();
        this.roomId = b.getRoomId();
        this.buildingName = b.getBuildingName();
        this.roomNo = b.getRoomNo();
        this.bedNo = b.getBedNo();
        this.status = b.getStatus();
        this.occupied = b.isOccupied();
        this.studentNo = b.getStudentNo();
        this.studentName = b.getStudentName();
    }

    public int getId() {
        return id;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public int getBedNo() {
        return bedNo;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Integer getStudentNo() {
        return studentNo;
    }

    public String getStudentName() {
        return studentName;
    }
}
