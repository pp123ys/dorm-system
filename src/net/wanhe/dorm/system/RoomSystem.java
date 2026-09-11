package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.RoomController;

/*
 * 房间床位管理菜单
 */
public class RoomSystem {

    private RoomController roomController = new RoomController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = roomController.print();
            switch (c) {
                case 1:
                    roomController.findRooms();
                    break;
                case 2:
                    roomController.findBeds();
                    break;
                case 3:
                    roomController.add();
                    break;
                case 4:
                    roomController.updateCapacity();
                    break;
                case 5:
                    roomController.del();
                    break;
                case 6:
                    roomController.updateRoomStatus();
                    break;
                case 7:
                    roomController.updateBedStatus();
                    break;
                case 8:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
