package net.wanhe.dormsystem.util;

import java.util.Scanner;

/*
 * 统一键盘输入的工具类
 * 整个程序共用一个 Scanner,避免多个 Scanner 同时读取 System.in 时
 * 各自缓冲抢占数据,导致后续读取不到输入内容
 * 所有读取都基于 nextLine(): 输入非数字时提示重试而不是直接抛异常
 */
public class ScannerUtil {

    public static final Scanner SC = new Scanner(System.in);

    /*
     * 读取整数, 输入不是数字时提示并重试
     */
    public static int nextInt(String prompt) {
        while (true) {
            System.out.println(prompt);
            String line = SC.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("输入不能为空");
                continue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("请输入数字");
            }
        }
    }

    /*
     * 读取指定范围的整数
     */
    public static int nextInt(String prompt, int min, int max) {
        while (true) {
            int value = nextInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("请输入 " + min + "~" + max + " 之间的数字");
        }
    }

    /*
     * 读取非空字符串
     */
    public static String nextNonEmpty(String prompt) {
        while (true) {
            System.out.println(prompt);
            String line = SC.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("输入不能为空");
        }
    }

    /*
     * 读取一行, 允许为空: 空输入返回 null
     */
    public static String nextLine(String prompt) {
        System.out.println(prompt);
        String line = SC.nextLine().trim();
        return line.isEmpty() ? null : line;
    }
}
