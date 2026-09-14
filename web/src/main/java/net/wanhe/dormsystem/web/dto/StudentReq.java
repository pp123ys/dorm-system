package net.wanhe.dormsystem.web.dto;

import net.wanhe.dormsystem.pojo.Student;

/*
 * 学生新增/修改请求体。学号只用于定位，不允许改（与既有 Service 语义一致）。
 */
public class StudentReq {

    private Integer no;
    private String name;
    private String sex;
    private Integer age;
    private String phone;

    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
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

    public Student toPojo(int no) {
        return new Student(no, name, sex, age, phone);
    }
}
