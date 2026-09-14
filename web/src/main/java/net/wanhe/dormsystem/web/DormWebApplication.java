package net.wanhe.dormsystem.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/*
 * Web 版启动类。
 *
 * 注意包名：这里在 net.wanhe.dormsystem.web 下，
 * 所以 Spring 默认的组件扫描范围是 net.wanhe.dormsystem.web.**，
 * 不会扫描到 net.wanhe.dormsystem.controller —— 那是控制台版的菜单控制器，
 * 它们没有 Spring 注解，保持原样即可，互不干扰。
 */
@SpringBootApplication
@EnableTransactionManagement
public class DormWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(DormWebApplication.class, args);
    }
}
