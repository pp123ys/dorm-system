package net.wanhe.dormsystem;

import net.wanhe.dormsystem.system.BuildingSystem;
import net.wanhe.dormsystem.system.RoomSystem;
import net.wanhe.dormsystem.system.StatSystem;
import net.wanhe.dormsystem.system.StaySystem;
import net.wanhe.dormsystem.system.StuSystem;
import net.wanhe.dormsystem.system.UserSystem;
import net.wanhe.dormsystem.util.ScannerUtil;

import java.util.NoSuchElementException;

public class Run {

    public static void main(String[] args) {
        //登录阶段也可能遇到数据库故障: 提示后干净退出, 不打印堆栈
        //(登录时数据库不可用, 重试没有意义, 因此这里直接结束)
        try {
            new UserSystem().run();
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RuntimeException e) {
            System.out.println("登录失败(数据库连接异常?):" + e.getMessage());
            return;
        }
        boolean f = true;
        while (f) {
            //菜单读取放在 try 外面: 输入耗尽(EOF)应当直接退出, 而不是被兜住后死循环
            int c = print();
            try {
                switch (c) {
                    case 1:
                        new BuildingSystem().run();
                        break;
                    case 2:
                        new RoomSystem().run();
                        break;
                    case 3:
                        new StuSystem().run();
                        break;
                    case 4:
                        new StaySystem().run();
                        break;
                    case 5:
                        new StatSystem().run();
                        break;
                    case 6:
                        f = false;
                        System.out.println("谢谢使用");
                        break;
                    default:
                        System.out.println("请选择正确的选项");
                }
            } catch (NoSuchElementException e) {
                //输入耗尽: 直接抛出, 让脚本类验收能立刻失败(避免掩盖"脚本行数不够")
                throw e;
            } catch (RuntimeException e) {
                //兜底: 数据库/DAO 等运行时故障只提示并回到菜单, 不让整个程序崩掉
                System.out.println("操作失败:" + e.getMessage());
            }
        }
    }

    public static int print() {
        System.out.println("--寓安 · 学生宿舍管理系统--");
        System.out.println("1.楼栋管理");
        System.out.println("2.房间床位管理");
        System.out.println("3.学生管理");
        System.out.println("4.入住退住办理");
        System.out.println("5.查询统计");
        System.out.println("6.退出系统");
        return ScannerUtil.nextInt("请选择:");
    }
}
