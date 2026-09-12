package net.wanhe.dormsystem.system;

import net.wanhe.dormsystem.controller.StayController;

/*
 * 入住退住办理菜单
 */
public class StaySystem {

    private StayController stayController = new StayController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = stayController.print();
            switch (c) {
                case 1:
                    stayController.checkIn();
                    break;
                case 2:
                    stayController.checkOut();
                    break;
                case 3:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
