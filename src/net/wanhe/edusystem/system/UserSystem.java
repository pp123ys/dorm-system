package net.wanhe.edusystem.system;

import net.wanhe.edusystem.controller.UserController;

public class UserSystem {

    UserController userController = new UserController();

    public void run(){
        boolean f = true;
        while(f){
            int c = userController.print();
            switch(c){
                case 1:
                    userController.regist();
                    break;
                case 2:
                    f = !userController.login();
                    break;
                default:
                    System.out.println("请输入正确的选项");
            }
        }



    }

}
