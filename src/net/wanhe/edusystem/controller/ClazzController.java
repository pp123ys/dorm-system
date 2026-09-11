package net.wanhe.edusystem.controller;

import net.wanhe.edusystem.exception.ClazzException;
import net.wanhe.edusystem.pojo.Clazz;
import net.wanhe.edusystem.service.ClazzService;
import net.wanhe.edusystem.service.impl.ClazzServiceImpl;
import net.wanhe.edusystem.util.ScannerUtil;

import java.util.List;
import java.util.Scanner;

public class ClazzController {

    //统一使用共享的Scanner 避免多个Scanner抢占System.in的缓冲
    private Scanner sc = ScannerUtil.SC;

    private ClazzService clazzService = new ClazzServiceImpl();

    public int print() {
        System.out.println("--班级管理--");
        System.out.println("1.查看班级");
        System.out.println("2.添加班级");
        System.out.println("3.删除班级");
        System.out.println("4.返回上一级");
        System.out.println("请选择:");
        return sc.nextInt();
    }

    public void find() {
        List<Clazz> clazzes = clazzService.find();
        for(Clazz c : clazzes){
            System.out.println("名称:" + c.getName()
                    + "   座位数:" + c.getCount());
        }
    }

    public void add() {
        System.out.println("请输入名称:");
        String name = sc.next();
        System.out.println("请输入座位数:");
        int count = sc.nextInt();
        try {
            clazzService.add(name,count);
            System.out.println("添加成功");
        } catch (ClazzException e) {
            System.out.println(e.getMessage());
        }
    }

    public void del() {
        System.out.println("请输入名称:");
        String name = sc.next();
        try {
            clazzService.del(name);
            System.out.println("删除成功");
        } catch (ClazzException e) {
            System.out.println(e.getMessage());
        }
    }
}
