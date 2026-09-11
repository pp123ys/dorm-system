package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StatService;
import net.wanhe.dorm.service.impl.StatServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.text.SimpleDateFormat;
import java.util.List;

public class StatController {

    private StatService statService = new StatServiceImpl();

    private static final SimpleDateFormat TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public int print() {
        System.out.println("--查询统计--");
        System.out.println("1.楼栋占用概览");
        System.out.println("2.房间住宿名单");
        System.out.println("3.空床位清单");
        System.out.println("4.学生住宿信息");
        System.out.println("5.入住退住流水");
        System.out.println("6.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 楼栋占用概览
     */
    public void overview() {
        List<Building> list = statService.buildingOverview();
        if (list.isEmpty()) {
            System.out.println("暂无楼栋数据");
            return;
        }
        int[] w = {8, 12, 6, 8, 8, 6, 6, 8};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "房间数", "总床位", "已住", "空床", "占用率"}, w);
        AlignUtil.printLine(w);
        for (Building b : list) {
            String rate = b.getBedCount() == 0 ? "0%" : (b.getOccupiedCount() * 100 / b.getBedCount()) + "%";
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    b.getName(),
                    b.getSex(),
                    String.valueOf(b.getRoomCount()),
                    String.valueOf(b.getBedCount()),
                    String.valueOf(b.getOccupiedCount()),
                    String.valueOf(b.getFreeCount()),
                    rate}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 房间住宿名单
     */
    public void roster() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        try {
            List<Student> list = statService.roomRoster(roomId);
            if (list.isEmpty()) {
                System.out.println("该房间暂无在住学生");
                return;
            }
            int[] w = {10, 10, 6, 16};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"学号", "姓名", "性别", "电话"}, w);
            AlignUtil.printLine(w);
            for (Student s : list) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(s.getNo()),
                        s.getName(),
                        s.getSex(),
                        s.getPhone() == null ? "" : s.getPhone()}, w);
            }
            AlignUtil.printLine(w);
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 空床位清单(0 表示全部楼栋)
     */
    public void freeBeds() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id(0表示全部楼栋):", 0, 999999999);
        Integer filter = buildingId == 0 ? null : buildingId;
        List<Bed> list = statService.freeBeds(filter);
        if (list.isEmpty()) {
            System.out.println("暂无空床位");
            return;
        }
        System.out.println("空床位数:" + list.size());
        int[] w = {8, 12, 10, 6};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"床位id", "楼栋", "房间号", "床号"}, w);
        AlignUtil.printLine(w);
        for (Bed b : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    b.getBuildingName(),
                    b.getRoomNo(),
                    String.valueOf(b.getBedNo())}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 学生住宿信息
     */
    public void studentStay() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student s = statService.studentStay(studentNo);
            System.out.println("学号:" + s.getNo());
            System.out.println("姓名:" + s.getName());
            System.out.println("性别:" + s.getSex());
            System.out.println("电话:" + (s.getPhone() == null ? "" : s.getPhone()));
            System.out.println("住宿位置:" + s.location());
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 入住退住流水(0 表示全部学生)
     */
    public void checkinHistory() {
        int studentNo = ScannerUtil.nextInt("请输入学号(0表示全部学生):", 0, 999999999);
        Integer filter = studentNo == 0 ? null : studentNo;
        List<Checkin> list = statService.checkinHistory(filter);
        if (list.isEmpty()) {
            System.out.println("暂无流水记录");
            return;
        }
        System.out.println("流水条数:" + list.size());
        int[] w = {21, 10, 10, 6, 18, 10};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"时间", "学号", "姓名", "操作", "位置", "操作人"}, w);
        AlignUtil.printLine(w);
        for (Checkin c : list) {
            AlignUtil.printRow(new String[]{
                    c.getCreateTime() == null ? "" : TIME.format(c.getCreateTime()),
                    String.valueOf(c.getStudentNo()),
                    c.getStudentName(),
                    c.getAction(),
                    c.location(),
                    c.getOperator()}, w);
        }
        AlignUtil.printLine(w);
    }
}
