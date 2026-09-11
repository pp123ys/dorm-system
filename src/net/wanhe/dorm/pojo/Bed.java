package net.wanhe.dorm.pojo;

public class Bed {

    private int id;

    private int roomId;

    private int bedNo;

    private String status;

    //以下四个字段仅查询展示用(LEFT JOIN 带出), 不参与 insert/update
    private String buildingName;

    private String roomNo;

    private Integer studentNo;

    private String studentName;

    public Bed() {
    }

    public Bed(int roomId, int bedNo, String status) {
        this.roomId = roomId;
        this.bedNo = bedNo;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getBedNo() {
        return bedNo;
    }

    public void setBedNo(int bedNo) {
        this.bedNo = bedNo;
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

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(Integer studentNo) {
        this.studentNo = studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    //该床位是否已有人住(由查询带出的 studentNo 判断)
    public boolean isOccupied() {
        return studentNo != null;
    }

    //住宿位置文字, 用于提示信息
    //注意: 仅对 BedDao 查询结果(LEFT JOIN 带出了 buildingName/roomNo)调用安全;
    //服务层手工 new 出来的 Bed(如新增房间时生成床位)这些字段为 null, 不要对其调用
    public String location() {
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
