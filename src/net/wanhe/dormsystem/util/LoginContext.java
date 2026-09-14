package net.wanhe.dormsystem.util;

/*
 * 保存当前登录的管理员登录名, 供入住/退住流水记录操作人。
 *
 * 改造说明：原来用静态 String 字段。控制台版是单线程的，静态字段没问题；
 * 但 Web 版由 Tomcat 线程池并发处理请求，静态字段会导致不同请求之间串号，
 * 因此改为 ThreadLocal —— 对外 API（set/get/clear）保持不变，
 * 控制台版用法与行为完全一致。
 */
public class LoginContext {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(String loginName) {
        CURRENT_USER.set(loginName);
    }

    public static String getCurrentUser() {
        return CURRENT_USER.get();
    }

    /*
     * 清除当前线程的登录名。Web 版在请求结束时必须调用：
     * Tomcat 线程是复用的，不清理会把上一个请求的登录名带到下一个请求。
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
