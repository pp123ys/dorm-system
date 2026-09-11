package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StayService;
import net.wanhe.dorm.service.impl.StayServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.LoginContext;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class StayController {

    private StayService stayService = new StayServiceImpl();

    public int print() {
        System.out.println("--入住退住办理--");
        System.out.println("1.办理入住");
        System.out.println("2.办理退住");
        System.out.println("3.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 办理入住: 学号 -> 有空床的楼栋 -> 该楼栋空床位 -> 选床位
     */
    public void checkIn() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student stu = stayService.checkInTarget(studentNo);
            System.out.println("学生:" + stu.getNo() + " " + stu.getName() + " " + stu.getSex());
            List<Building> buildings = stayService.buildingsWithFreeBed();
            if (buildings.isEmpty()) {
                System.out.println("暂无有空床的楼栋");
                return;
            }
            int[] w = {8, 12, 6, 8};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "空床数"}, w);
            AlignUtil.printLine(w);
            for (Building b : buildings) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(b.getId()),
                        b.getName(),
                        b.getSex(),
                        String.valueOf(b.getFreeCount())}, w);
            }
            AlignUtil.printLine(w);
            int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
            List<Bed> beds = stayService.freeBeds(buildingId);
            if (beds.isEmpty()) {
                System.out.println("该楼栋暂无空床位");
                return;
            }
            int[] bw = {8, 10, 6};
            AlignUtil.printLine(bw);
            AlignUtil.printRow(new String[]{"床位id", "房间号", "床号"}, bw);
            AlignUtil.printLine(bw);
            for (Bed b : beds) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(b.getId()),
                        b.getRoomNo(),
                        String.valueOf(b.getBedNo())}, bw);
            }
            AlignUtil.printLine(bw);
            int bedId = ScannerUtil.nextInt("请输入床位id:");
            stayService.checkIn(studentNo, bedId, LoginContext.getCurrentUser());
            System.out.println("办理入住成功");
        } catch (StayException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 办理退住
     */
    public void checkOut() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student stu = stayService.stayInfo(studentNo);
            System.out.println("学生:" + stu.getNo() + " " + stu.getName() + " 当前:" + stu.location());
            stayService.checkOut(studentNo, LoginContext.getCurrentUser());
            System.out.println("办理退住成功");
        } catch (StayException e) {
            System.out.println(e.getMessage());
        }
    }
}
