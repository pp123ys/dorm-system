package net.wanhe.edusystem.util;

import java.util.Scanner;

/*
 * 统一键盘输入的工具类
 * 整个程序共用一个 Scanner,避免多个 Scanner 同时读取 System.in 时
 * 各自缓冲抢占数据,导致后续读取不到输入内容
 */
public class ScannerUtil {

    public static final Scanner SC = new Scanner(System.in);

}
