package net.wanhe.edusystem.system;

import net.wanhe.edusystem.controller.ClazzController;

public class ClazzSystem {

    private ClazzController clazzController = new ClazzController();

    public void run(){
        boolean f = true;
        while(f){
            int c = clazzController.print();
            switch(c){
                case 1:
                    clazzController.find();
                    break;
                case 2:
                    clazzController.add();
                    break;
                case 3:
                    clazzController.del();
                    break;
                case 4:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }

    }

}
