package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.BuildingController;

/*
 * 楼栋管理菜单
 */
public class BuildingSystem {

    private BuildingController buildingController = new BuildingController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = buildingController.print();
            switch (c) {
                case 1:
                    buildingController.find();
                    break;
                case 2:
                    buildingController.add();
                    break;
                case 3:
                    buildingController.update();
                    break;
                case 4:
                    buildingController.del();
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
