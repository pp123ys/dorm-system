package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Checkin;

import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * 流水记录视图：把 create_time 输出成 yyyy-MM-dd HH:mm:ss 字符串，
 * 避免前端处理时区/格式差异；同时带出可读的"位置"文案。
 */
public class CheckinView {

    private final int id;
    private final int studentNo;
    private final String studentName;
    private final String action;
    private final String buildingName;
    private final String roomNo;
    private final Integer bedNo;
    private final String location;
    private final String operator;
    private final String createTime;

    public CheckinView(Checkin c, SimpleDateFormat fmt) {
        this.id = c.getId();
        this.studentNo = c.getStudentNo();
        this.studentName = c.getStudentName();
        this.action = c.getAction();
        this.buildingName = c.getBuildingName();
        this.roomNo = c.getRoomNo();
        this.bedNo = c.getBedNo();
        this.operator = c.getOperator();
        Date t = c.getCreateTime();
        this.createTime = t == null ? "" : fmt.format(t);
        this.location = (c.getBuildingName() == null ? "" : c.getBuildingName())
                + (c.getRoomNo() == null ? "" : c.getRoomNo())
                + (c.getBedNo() == null ? "" : "房" + c.getBedNo() + "床");
    }

    public int getId() {
        return id;
    }

    public int getStudentNo() {
        return studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getAction() {
        return action;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public Integer getBedNo() {
        return bedNo;
    }

    public String getLocation() {
        return location;
    }

    public String getOperator() {
        return operator;
    }

    public String getCreateTime() {
        return createTime;
    }
}
