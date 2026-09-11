package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StuService;
import net.wanhe.dorm.service.impl.StuServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class StuController {

    private StuService stuService = new StuServiceImpl();

    public int print() {
        System.out.println("--学生管理--");
        System.out.println("1.查看学生");
        System.out.println("2.新增学生");
        System.out.println("3.修改学生");
        System.out.println("4.删除学生");
        System.out.println("5.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 查看学生(带住宿位置)
     */
    public void find() {
        List<Student> list = stuService.list();
        if (list.isEmpty()) {
            System.out.println("暂无学生数据");
            return;
        }
        int[] w = {10, 10, 6, 6, 16, 18};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"学号", "姓名", "性别", "年龄", "电话", "住宿位置"}, w);
        AlignUtil.printLine(w);
        for (Student s : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(s.getNo()),
                    s.getName(),
                    s.getSex(),
                    s.getAge() == null ? "" : String.valueOf(s.getAge()),
                    s.getPhone() == null ? "" : s.getPhone(),
                    s.location()}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增学生
     */
    public void add() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        String name = ScannerUtil.nextNonEmpty("请输入姓名:");
        String sex = ScannerUtil.nextNonEmpty("请输入性别(男/女):");
        int age = ScannerUtil.nextInt("请输入年龄(10~100):", 10, 100);
        String phone = ScannerUtil.nextLine("请输入电话(可空):");
        Student student = new Student(no, name, sex, age, phone);
        try {
            stuService.add(student);
            System.out.println("新增成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 修改学生(学号只用于定位, 不可改)
     */
    public void update() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        String name = ScannerUtil.nextNonEmpty("请输入新的姓名:");
        String sex = ScannerUtil.nextNonEmpty("请输入新的性别(男/女):");
        int age = ScannerUtil.nextInt("请输入新的年龄(10~100):", 10, 100);
        String phone = ScannerUtil.nextLine("请输入新的电话(可空):");
        Student student = new Student(no, name, sex, age, phone);
        try {
            stuService.update(student);
            System.out.println("修改成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除学生
     */
    public void del() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            stuService.delete(no);
            System.out.println("删除成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }
}
