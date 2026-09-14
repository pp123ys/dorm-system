package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Student;

/*
 * 学生视图：POJO 里的 location() 不是 getter，不会被 Jackson 序列化，
 * 这里显式暴露 location 字符串，省得前端自己拼"1号楼101房1床"。
 */
public class StudentView {

    private final int id;
    private final int no;
    private final String name;
    private final String sex;
    private final Integer age;
    private final String phone;
    private final Integer bedId;
    private final String buildingName;
    private final String roomNo;
    private final Integer bedNo;
    private final String location;
    private final boolean checkedIn;

    public StudentView(Student s) {
        this.id = s.getId();
        this.no = s.getNo();
        this.name = s.getName();
        this.sex = s.getSex();
        this.age = s.getAge();
        this.phone = s.getPhone();
        this.bedId = s.getBedId();
        this.buildingName = s.getBuildingName();
        this.roomNo = s.getRoomNo();
        this.bedNo = s.getBedNo();
        this.location = s.location();
        this.checkedIn = s.isCheckedIn();
    }

    public int getId() {
        return id;
    }

    public int getNo() {
        return no;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }

    public Integer getAge() {
        return age;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getBedId() {
        return bedId;
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

    public boolean isCheckedIn() {
        return checkedIn;
    }
}
