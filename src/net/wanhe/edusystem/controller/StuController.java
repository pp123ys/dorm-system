package net.wanhe.edusystem.controller;

import net.wanhe.edusystem.exception.StuException;
import net.wanhe.edusystem.pojo.Student;
import net.wanhe.edusystem.service.StuService;
import net.wanhe.edusystem.service.impl.StuServiceImpl;
import net.wanhe.edusystem.util.ScannerUtil;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/*
 * 负责学生相关业务中用户交互的部分
 */
public class StuController {

    //统一使用共享的Scanner 避免多个Scanner抢占System.in的缓冲
    Scanner sc = ScannerUtil.SC;

    StuService stuService = new StuServiceImpl();

    /*
     * 展示菜单
     */
    public int print() {
        System.out.println("--学生管理--");
        System.out.println("1.查看学生信息");
        System.out.println("2.添加学生");
        System.out.println("3.删除学生");
        System.out.println("4.返回上一级");
        System.out.println("请选择:");
        int c = sc.nextInt();
        return c;
    }

    /*
     * 展示学生信息
     */
    public void find(){
        //需要按照指定的格式将数据展示给用户看
        List<Student> stus = stuService.find();
        //按照学号从小到大排序(Student实现了Comparable)
        Collections.sort(stus);
        for (Student s : stus) {
            System.out.println("学号:" + s.getNo()
                    + "  姓名:" + s.getName()
                    + "  年龄:" + s.getAge()
                    + "  电话:" + s.getPhone()
                    + "  班级:" + s.getC().getName());
        }
    }

    /*
     * 添加学生
     */
    public void add(){
        System.out.println("请输入学号:");
        int no = sc.nextInt();
        System.out.println("请输入姓名:");
        String name = sc.next();
        System.out.println("请输入年龄:");
        int age = sc.nextInt();
        System.out.println("请输入电话:");
        String phone = sc.next();
        System.out.println("请输入班级名称");
        String clazzName = sc.next();
        try {
            stuService.add(no,name,age,phone,clazzName);
            System.out.println("添加成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除学生
     */
    public void del(){
        System.out.println("请输入学号:");
        int no = sc.nextInt();
        try {
            stuService.del(no);
            System.out.println("删除成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

}
