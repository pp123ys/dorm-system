package net.wanhe.dorm;

import net.wanhe.dorm.util.ScannerUtil;

public class Run {

    public static void main(String[] args) {
        boolean f = true;
        while (f) {
            int c = print();
            switch (c) {
                case 1:
                    System.out.println("该功能尚未实现");
                    break;
                case 2:
                    System.out.println("该功能尚未实现");
                    break;
                case 3:
                    System.out.println("该功能尚未实现");
                    break;
                case 4:
                    System.out.println("该功能尚未实现");
                    break;
                case 5:
                    System.out.println("该功能尚未实现");
                    break;
                case 6:
                    f = false;
                    System.out.println("谢谢使用");
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }

    public static int print() {
        System.out.println("--学生宿舍管理系统--");
        System.out.println("1.楼栋管理");
        System.out.println("2.房间床位管理");
        System.out.println("3.学生管理");
        System.out.println("4.入住退住办理");
        System.out.println("5.查询统计");
        System.out.println("6.退出系统");
        return ScannerUtil.nextInt("请选择:");
    }
}
