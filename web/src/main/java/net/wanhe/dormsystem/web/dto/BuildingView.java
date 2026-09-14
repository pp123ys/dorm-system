package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Building;

/*
 * 楼栋视图：补上 POJO 里不是 getter 的 freeCount（空床数）。
 */
public class BuildingView {

    private final int id;
    private final String name;
    private final String sex;
    private final int floors;
    private final String remark;
    private final int roomCount;
    private final int bedCount;
    private final int occupiedCount;
    private final int freeCount;

    public BuildingView(Building b) {
        this.id = b.getId();
        this.name = b.getName();
        this.sex = b.getSex();
        this.floors = b.getFloors();
        this.remark = b.getRemark();
        this.roomCount = b.getRoomCount();
        this.bedCount = b.getBedCount();
        this.occupiedCount = b.getOccupiedCount();
        this.freeCount = b.getFreeCount();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public int getFloors() {
        return floors;
    }

    public String getRemark() {
        return remark;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public int getBedCount() {
        return bedCount;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public int getFreeCount() {
        return freeCount;
    }
}
