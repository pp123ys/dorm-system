package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.StatController;

/*
 * 查询统计菜单
 */
public class StatSystem {

    private StatController statController = new StatController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = statController.print();
            switch (c) {
                case 1:
                    statController.overview();
                    break;
                case 2:
                    statController.roster();
                    break;
                case 3:
                    statController.freeBeds();
                    break;
                case 4:
                    statController.studentStay();
                    break;
                case 5:
                    statController.checkinHistory();
                    break;
                case 6:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
