package net.wanhe.dormsystem.pojo;

public class Building {

    private int id;

    private String name;

    private String sex;

    private int floors;

    private String remark;

    //以下三个字段仅查询展示用(占用概览), 不参与 insert/update
    private int roomCount;

    private int bedCount;

    private int occupiedCount;

    public Building() {
    }

    public Building(String name, String sex, int floors, String remark) {
        this.name = name;
        this.sex = sex;
        this.floors = floors;
        this.remark = remark;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getFloors() {
        return floors;
    }

    public void setFloors(int floors) {
        this.floors = floors;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    //空床数 = 可分配床位数 - 已住人数(展示用)
    public int getFreeCount() {
        return bedCount - occupiedCount;
    }
}
