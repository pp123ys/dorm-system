package net.wanhe.edusystem.system;

import net.wanhe.edusystem.controller.StuController;

/*
 * 学生系统
 * 三层架构
 * Controller 用户交互(获取数据、展示数据)
 * Service 业务逻辑
 * Dao Data Access Object数据处理(增删改查)
 */
public class StuSystem {

    StuController stuController = new StuController();

    /*
     * 运行流程
     */
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
                    stuController.del();
                    break;
                case 4:
                    f = false;
                    break;
                default:
                    System.out.println("请输入正确的选项");
            }
        }

    }


}