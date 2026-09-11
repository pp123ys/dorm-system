package net.wanhe.edusystem;

import net.wanhe.edusystem.system.ClazzSystem;
import net.wanhe.edusystem.system.StuSystem;
import net.wanhe.edusystem.system.UserSystem;
import net.wanhe.edusystem.util.ScannerUtil;

import java.util.Scanner;

public class Run {

    public static void main(String[] args) {
        UserSystem us = new UserSystem();
        us.run();
        boolean f = true;
        while(f){
            int c = print();
            switch(c){
                case 1:
                    ClazzSystem cs = new ClazzSystem();
                    cs.run();
                    break;
                case 2:
                    StuSystem ss = new StuSystem();
                    ss.run();
                    break;
                case 3:
                    f = false;
                    System.out.println("谢谢使用");
            }
        }
    }

    public static int print(){
        //统一使用共享的Scanner 避免多个Scanner抢占System.in的缓冲
        Scanner sc = ScannerUtil.SC;
        System.out.println("--教育系统--");
        System.out.println("1.班级管理");
        System.out.println("2.学生管理");
        System.out.println("3.退出");
        System.out.println("请选择:");
        return sc.nextInt();
    }

}
