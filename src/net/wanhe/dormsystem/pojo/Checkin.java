package net.wanhe.dormsystem.pojo;

import java.util.Date;

/*
 * 入住退住流水
 * 楼栋名称/房间号/床位号/学号/姓名都是冗余留痕:
 * 日后房间或学生被删除, 历史记录依然可读
 */
public class Checkin {

    private int id;

    private int studentId;

    private int studentNo;

    private String studentName;

    //操作: 入住/退住
    private String action;

    private String buildingName;

    private String roomNo;

    private Integer bedNo;

    private String operator;

    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(int studentNo) {
        this.studentNo = studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getBedNo() {
        return bedNo;
    }

    public void setBedNo(Integer bedNo) {
        this.bedNo = bedNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    //住宿位置文字, 用于流水展示
    public String location() {
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
