package net.wanhe.dormsystem.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * 单端口部署（build-all.bat 把前端产物嵌入 jar）时的 SPA 深链接兜底。
 *
 * 背景：前端用 vue-router 的 history 模式。按 F5 刷新 /students 这类子页面时，
 * 浏览器会直接向服务器要这个路径；Spring Boot 找不到对应的静态资源就返回 404。
 * 这里把没有匹配到控制器、也不是静态资源的路径转回 index.html，
 * 交给前端路由重新渲染，刷新子页面就不会白屏。
 *
 * 说明：
 * - 只接管没有任何映射的路径。Spring MVC 的映射优先级为
 *   @RequestMapping 控制器 > 静态资源 > 本兜底，所以 /api/** 与真实静态资源不受影响。
 * - 只对非 /api 路径生效：/api 下不存在的接口应该老老实实返回 404，
 *   否则前端会把一个 HTML 页面当 JSON 解析，反而更难排查。
 * - 未嵌入前端产物时（即未跑 build-all.bat），static/index.html 不存在，
 *   转发会失败并返回 500；那种情况请用 build-web.bat + Vite 开发服务器（见 WEB.md）。
 */
@Controller
public class SpaForwardController {

    @RequestMapping(value = {"/{path:[^\\.]*}"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
