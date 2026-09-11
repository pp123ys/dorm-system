package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.service.BuildingService;
import net.wanhe.dorm.service.impl.BuildingServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class BuildingController {

    private BuildingService buildingService = new BuildingServiceImpl();

    public int print() {
        System.out.println("--楼栋管理--");
        System.out.println("1.查看楼栋");
        System.out.println("2.新增楼栋");
        System.out.println("3.修改楼栋");
        System.out.println("4.删除楼栋");
        System.out.println("5.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 查看楼栋
     */
    public void find() {
        List<Building> list = buildingService.list();
        if (list.isEmpty()) {
            System.out.println("暂无楼栋数据");
            return;
        }
        int[] w = {8, 12, 6, 6, 16};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "楼层", "备注"}, w);
        AlignUtil.printLine(w);
        for (Building b : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    AlignUtil.truncate(b.getName(), 12),
                    b.getSex(),
                    String.valueOf(b.getFloors()),
                    AlignUtil.truncate(b.getRemark() == null ? "" : b.getRemark(), 16)}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增楼栋
     */
    public void add() {
        String name = ScannerUtil.nextNonEmpty("请输入楼栋名称:");
        String sex = ScannerUtil.nextNonEmpty("请输入楼栋类型(男/女):");
        int floors = ScannerUtil.nextInt("请输入楼层数:", 1, 100);
        String remark = ScannerUtil.nextLine("请输入备注(可空):");
        Building building = new Building(name, sex, floors, remark);
        try {
            buildingService.add(building);
            System.out.println("新增成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 修改楼栋
     */
    public void update() {
        int id = ScannerUtil.nextInt("请输入要修改的楼栋id:");
        String name = ScannerUtil.nextNonEmpty("请输入新的楼栋名称:");
        String sex = ScannerUtil.nextNonEmpty("请输入新的楼栋类型(男/女):");
        int floors = ScannerUtil.nextInt("请输入新的楼层数:", 1, 100);
        String remark = ScannerUtil.nextLine("请输入新的备注(可空):");
        Building building = new Building(name, sex, floors, remark);
        building.setId(id);
        try {
            buildingService.update(building);
            System.out.println("修改成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除楼栋
     */
    public void del() {
        int id = ScannerUtil.nextInt("请输入要删除的楼栋id:");
        try {
            buildingService.delete(id);
            System.out.println("删除成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }
}
