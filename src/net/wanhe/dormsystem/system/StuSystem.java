package net.wanhe.dormsystem.system;

import net.wanhe.dormsystem.controller.StuController;

/*
 * 学生管理菜单
 */
public class StuSystem {

    private StuController stuController = new StuController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = stuController.print();
            switch (c) {
                case 1:
                    stuController.find();
                    break;
                case 2:
                    stuController.add();
                    break;
                case 3:
                    stuController.update();
                    break;
                case 4:
                    stuController.del();
                    break;
                case 5:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
