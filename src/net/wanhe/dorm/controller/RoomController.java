package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.service.RoomService;
import net.wanhe.dorm.service.impl.RoomServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class RoomController {

    private RoomService roomService = new RoomServiceImpl();

    public int print() {
        System.out.println("--房间床位管理--");
        System.out.println("1.按楼栋查看房间");
        System.out.println("2.查看某房间床位详情");
        System.out.println("3.新增房间");
        System.out.println("4.调整房间容量");
        System.out.println("5.删除房间");
        System.out.println("6.停用/启用房间");
        System.out.println("7.停用/启用床位");
        System.out.println("8.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 按楼栋查看房间(带已住/空床)
     */
    public void findRooms() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
        try {
            List<Room> list = roomService.listByBuilding(buildingId);
            if (list.isEmpty()) {
                System.out.println("该楼栋下暂无房间");
                return;
            }
            int[] w = {8, 10, 10, 8, 6, 8};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"房间id", "楼栋", "房间号", "床位数", "已住", "状态"}, w);
            AlignUtil.printLine(w);
            for (Room r : list) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(r.getId()),
                        AlignUtil.truncate(r.getBuildingName(), 10),
                        AlignUtil.truncate(r.getRoomNo(), 10),
                        String.valueOf(r.getCapacity()),
                        String.valueOf(r.getOccupiedCount()),
                        r.getStatus()}, w);
            }
            AlignUtil.printLine(w);
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 查看某房间的床位详情
     */
    public void findBeds() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        List<Bed> list = roomService.listBeds(roomId);
        if (list.isEmpty()) {
            System.out.println("该房间下暂无床位");
            return;
        }
        int[] w = {8, 6, 8, 16};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"床位id", "床号", "状态", "在住学生"}, w);
        AlignUtil.printLine(w);
        for (Bed b : list) {
            String occupant = b.isOccupied() ? b.getStudentName() + "(" + b.getStudentNo() + ")" : "空";
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    String.valueOf(b.getBedNo()),
                    b.getStatus(),
                    AlignUtil.truncate(occupant, 16)}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增房间(自动生成床位)
     */
    public void add() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
        String roomNo = ScannerUtil.nextNonEmpty("请输入房间号:");
        int capacity = ScannerUtil.nextInt("请输入床位数:", 1, 20);
        Room room = new Room(buildingId, roomNo, capacity, "正常");
        try {
            roomService.add(room);
            System.out.println("新增成功, 已自动生成" + capacity + "个床位");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 调整房间容量(自动补床/删空床)
     */
    public void updateCapacity() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        int capacity = ScannerUtil.nextInt("请输入新的床位数:", 1, 20);
        try {
            roomService.updateCapacity(roomId, capacity);
            System.out.println("调整成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除房间
     */
    public void del() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        try {
            roomService.delete(roomId);
            System.out.println("删除成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 停用/启用房间
     */
    public void updateRoomStatus() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        String status = readStatus();
        try {
            roomService.updateRoomStatus(roomId, status);
            System.out.println("操作成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 停用/启用床位
     */
    public void updateBedStatus() {
        int bedId = ScannerUtil.nextInt("请输入床位id:");
        String status = readStatus();
        try {
            roomService.updateBedStatus(bedId, status);
            System.out.println("操作成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 读取状态: 1.正常 2.停用
     */
    private String readStatus() {
        int s = ScannerUtil.nextInt("请输入状态(1.正常 2.停用):", 1, 2);
        return s == 1 ? "正常" : "停用";
    }
}
