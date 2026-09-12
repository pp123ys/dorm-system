package net.wanhe.dormsystem.util;

/*
 * 保存当前登录的管理员登录名, 供入住/退住流水记录操作人
 * 控制台程序是单线程的, 用静态字段即可
 */
public class LoginContext {

    private static String currentUser;

    public static void setCurrentUser(String loginName) {
        currentUser = loginName;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
