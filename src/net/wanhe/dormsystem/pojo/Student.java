package net.wanhe.dormsystem.pojo;

public class Student {

    private int id;

    private int no;

    private String name;

    private String sex;

    private Integer age;

    private String phone;

    //入住的床位id, null 表示未入住
    private Integer bedId;

    //以下三个字段仅查询展示用(LEFT JOIN 带出), 不参与 insert/update
    private String buildingName;

    private String roomNo;

    private Integer bedNo;

    public Student() {
    }

    public Student(int no, String name, String sex, Integer age, String phone) {
        this.no = no;
        this.name = name;
        this.sex = sex;
        this.age = age;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
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

    //是否已入住
    public boolean isCheckedIn() {
        return bedId != null;
    }

    //住宿位置文字, 未入住返回 "未入住"
    public String location() {
        if (bedId == null) {
            return "未入住";
        }
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
