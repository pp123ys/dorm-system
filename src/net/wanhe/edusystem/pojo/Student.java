package net.wanhe.edusystem.pojo;

import java.io.Serializable;

public class Student implements Serializable,Comparable {

    private int no;

    private String name;

    private int age;

    private String phone;

    private Clazz c;

    public Student(){

    }

    public Student(int no, String name, int age, String phone) {
        this.no = no;
        this.name = name;
        this.age = age;
        this.phone = phone;
    }

    public Student(int no, String name, int age, String phone, Clazz c) {
        this.no = no;
        this.name = name;
        this.age = age;
        this.phone = phone;
        this.c = c;
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Clazz getC() {
        return c;
    }

    public void setC(Clazz c) {
        this.c = c;
    }

    /*
     * 实现当前类的比较规则 按照学号从小到大
     */
    @Override
    public int compareTo(Object o) {
        //判断o是不是一个Student
        if(!(o instanceof Student)){
            throw new RuntimeException("类型错误");
        }
        Student s = (Student) o;
        if(this.no > s.getNo()){
            return 1;
        }
        if(this.no < s.getNo()){
            return -1;
        }
        return 0;
    }
}
