# 寓安 · 学生宿舍管理系统 —— Web 版（前后端分离）

本文说明 Web 版的架构、启动方式、接口清单与已知限制。

> 控制台版（`build.bat` / `run.bat` / `src` 下的 `Run.java`）**完全保留**，与 Web 版并存，两者共用同一套
> `dorm_system` 数据库和同一份 DAO/Service 业务代码。控制台版说明见 `BUILD.md`。

---

## 一、总体架构

```
┌──────────────┐   HTTP/JSON     ┌─────────────────────┐   JDBC    ┌──────────────┐
│  浏览器       │ ──────────────► │  Spring Boot 2.7.18 │ ────────► │ MySQL 5.5.28 │
│  Vue3 前端    │ ◄────────────── │  Tomcat 8080 /api   │ ◄──────── │ dorm_system  │
└──────────────┘   X-Token 鉴权  └─────────────────────┘   Hikari  └──────────────┘
       │                                  │
   Vite 3000 开发服务器              复用既有 DAO + Service
   （/api 反向代理到 8080）          （net.wanhe.dormsystem.*）
```

| 层 | 技术 | 说明 |
|---|---|---|
| 前端 | Vue 3.5 + Vite 8 + Element Plus 2.14 + vue-router 5 + axios | 目录 `web-frontend/` |
| 后端 | Spring Boot 2.7.18（Spring MVC + 内嵌 Tomcat 9，JDK 1.8） | 目录 `web/`，打 fat jar |
| 数据访问 | **复用既有原生 JDBC DAO** | 未引入 MyBatis/JPA |
| 数据库 | MySQL 5.5.28，库 `dorm_system`，6 张表 | 与控制台版共用 |
| 鉴权 | 简化版 token（服务端内存 + 前端 localStorage） | 见"已知限制" |

---

## 二、启动步骤

### 1. 准备数据库（只需一次，可重复执行）

```bat
mysql -h 127.0.0.1 -uroot -p123456 --default-character-set=utf8mb4 < db\schema.sql
```

### 2. 构建并启动后端

```bat
build-web.bat
```

产出 `web\target\dorm-web-1.0.0.jar`，然后启动：

```bat
java -Dfile.encoding=UTF-8 -jar web\target\dorm-web-1.0.0.jar
```

看到 `Started DormWebApplication` 即成功，接口在 `http://localhost:8080`。

> **`-Dfile.encoding=UTF-8` 不能省**：本机 JDK 是 1.8.0_191，默认编码 GBK，
> 不指定会导致中文菜单/日志乱码、中文数据写入乱码。

### 3. 启动前端

```bat
cd web-frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:3000`，用 `admin / 123456` 登录。

### 4. 一键验收（后端接口）

后端启动后执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File web\acceptance.ps1
```

覆盖 47 项断言：健康检查、鉴权（401/伪造 token）、5 大模块 CRUD、事务（房间+床位、入住+流水）、
中文编码、以及各类反例（重名、重复学号、重复入住、床位被占、性别不符等）。

---

## 三、接口清单（29 个）

统一响应体：`{"code":0,"message":"ok","data":...}`；`code=1` 表示业务校验失败，`message` 是可直接展示的中文原因。

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 健康 | GET | `/api/health` | 免鉴权，含数据库连通状态 |
| 鉴权 | POST | `/api/auth/login` | 返回 `{token, loginName}` |
| 鉴权 | POST | `/api/auth/logout` | 注销当前 token |
| 鉴权 | GET | `/api/auth/me` | 当前登录用户 |
| 楼栋 | GET | `/api/buildings` | 列表 |
| 楼栋 | GET | `/api/buildings/{id}` | 单个 |
| 楼栋 | POST | `/api/buildings` | 新增 |
| 楼栋 | PUT | `/api/buildings/{id}` | 修改 |
| 楼栋 | DELETE | `/api/buildings/{id}` | 删除（有房间时拒绝） |
| 房间 | GET | `/api/buildings/{id}/rooms` | 某楼栋房间 |
| 房间 | POST | `/api/rooms` | 新增（同事务自动生成床位） |
| 房间 | PUT | `/api/rooms/{id}/capacity` | 改容量（扩容补床/缩容删空床） |
| 房间 | PUT | `/api/rooms/{id}/status` | 正常/停用 |
| 房间 | DELETE | `/api/rooms/{id}` | 删除（连带床位，有在住学生时拒绝） |
| 床位 | GET | `/api/rooms/{roomId}/beds` | 床位列表（含入住情况） |
| 床位 | PUT | `/api/beds/{id}/status` | 正常/停用 |
| 学生 | GET | `/api/students` | 列表（含住宿位置） |
| 学生 | POST | `/api/students` | 新增 |
| 学生 | PUT | `/api/students/{no}` | 修改（学号不可改） |
| 学生 | DELETE | `/api/students/{no}` | 删除（在住学生拒绝） |
| 住宿 | GET | `/api/stays/free-buildings` | 有空床的楼栋 |
| 住宿 | GET | `/api/stays/free-beds?buildingId=` | 可分配床位（省略=全部楼栋） |
| 住宿 | GET | `/api/stays/{studentNo}` | 学生住宿信息 |
| 住宿 | GET | `/api/stays/{studentNo}/check-in-target` | 入住前预检 |
| 住宿 | POST | `/api/stays/check-in` | 办理入住（改床位+写流水，同事务） |
| 住宿 | POST | `/api/stays/check-out` | 办理退住（清床位+写流水，同事务） |
| 统计 | GET | `/api/stats/overview` | 楼栋占用概览 |
| 统计 | GET | `/api/stats/rooms/{roomId}/roster` | 房间住宿名单 |
| 统计 | GET | `/api/stats/free-beds?buildingId=` | 空床位清单 |
| 统计 | GET | `/api/stats/students/{no}` | 学生住宿信息 |
| 统计 | GET | `/api/stats/checkins?studentNo=` | 入住退住流水（时间已格式化） |

除 `/api/health` 与 `/api/auth/login` 外，全部需要请求头 `X-Token`（也兼容 `Authorization: Bearer <token>`）。

---

## 四、前端页面

| 路由 | 页面 | 功能 |
|---|---|---|
| `/login` | 登录页 | 表单校验、错误提示、免登录直达 |
| `/dashboard` | 数据概览 | 楼栋数/总床位/已住/空床卡片 + 占用率进度条 |
| `/buildings` | 楼栋管理 | 表格 + 新增/编辑弹窗 + 删除二次确认 |
| `/rooms` | 房间床位管理 | 选楼栋 → 房间表格 → 床位抽屉（改床位状态）、改容量、停用 |
| `/students` | 学生管理 | 表格（含住宿位置）+ 搜索 + 增删改 |
| `/stays` | 入住退住办理 | 入住 4 步流程（学号→楼栋→床位→确认）；退住先查后确认 |
| `/stats` | 查询统计 | 5 个 Tab：占用概览、住宿名单、空床位、住宿信息、流水 |

---

## 五、改造要点与踩到的坑（重要，便于以后维护）

### 1. 复用既有 Service 而不破坏控制台版

- `util/JdbcUtil.java` 改为**双模式**：未注入 `DataSource` 时用硬编码连接信息自建
  `DriverManagerDataSource`（控制台行为不变）；Web 版由 `config/DataSourceInitializer`
  在启动时把 Spring 的连接池交进去。连接统一走 `DataSourceUtils`，因此自动加入 Spring 事务。
- `util/LoginContext.java` 由静态字段改为 `ThreadLocal`：控制台单线程无影响，Web 并发下必须避免串号。
- 事务边界放在 `web/service/TxService.java`（`@Transactional(rollbackFor = Exception.class)`），
  它委托既有 `RoomServiceImpl` / `StayServiceImpl`，**没有改动这两处业务代码**。

### 2. 业务异常为什么必须"解包"（已用 javap 确认）

Spring 5.3.31 的 `InvocableHandlerMethod.doInvoke` 对**受检异常**会包装成
`IllegalStateException("Invocation failure", cause)` 再抛出；而既有业务异常
（`UserException`/`BuildingException`/`RoomException`/`StuException`/`StayException`）全是受检异常，
导致 `@ExceptionHandler(UserException.class)` 匹配不到，会落到兜底变成 500、用户看不到中文提示。
（Spring 6 已改为解包后再抛；本项目受 JDK 1.8 限制只能用 5.3.x。）
`web/common/GlobalExceptionHandler.java` 里的 `unwrap()` 会剥掉两层
（`IllegalStateException` → `InvocationTargetException` → 业务异常）后按真实类型分发。

### 3. 两个源码根

`web/pom.xml` 用 `<sourceDirectory>../src</sourceDirectory>` 复用仓库根目录已有的 54 个类，
并用 `build-helper-maven-plugin` 把 `web/src/main/java` 作为第二个源码根补回来
（**显式设置 sourceDirectory 后 Maven 不再默认包含 `src/main/java`**，漏了这一步 Web 层类不会被编译）。
同时用 `maven-compiler-plugin` 排除 `controller/**`、`system/**`、`Run.java`：这些是控制台专用，
其中 `net.wanhe.dormsystem.controller.BuildingController` 与 Web 控制器**同名**（不同包），
一起编译会报"类名与文件名不匹配"。

### 4. 错误码约定

- **401**：未登录 / token 失效 → 前端清 localStorage 并跳登录页
- **200 + code=1**：业务校验失败（含"用户名或密码错误"）→ 前端用 `ElMessage.error` 展示 `message`
- **400**：参数不合法
- **500**：服务内部错误，详情只进后端日志

---

## 六、已知限制

1. **token 存内存**：后端重启后所有登录态失效，需重新登录；不支持多实例/集群。
   要持久化可换 JWT 或 Redis。
2. **单管理员**：沿用 `t_user` 与 `admin/123456`，没有角色与权限分级。
3. **无分页**：学生/流水等列表一次全量返回。数据量大时需加分页。
4. **前端未做移动端适配**，按 PC 管理后台设计。
5. **开发态为两个端口**（3000 + 8080）；生产部署可 `npm run build` 后把 `dist` 拷到
   `web/src/main/resources/static/`，重新 `build-web.bat` 即可单端口访问（8080 直接出页面）。
6. **连接池**：Web 版用 HikariCP（`application.yml` 可调）；控制台版仍是每次新建连接的
   `DriverManagerDataSource`，与之保持一致的行为，未强行统一。

---

## 七、常见问题

| 现象 | 原因与处理 |
|---|---|
| `mvn` 报 `No compiler is provided... JRE rather than a JDK` | 本机 Maven 默认用 JRE；用 `build-web.bat`（内部设置 `JAVA_HOME` 指向 JDK） |
| 启动报 `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 用的是旧 jar；重新 `build-web.bat` |
| 页面能开但接口全 401 | 未登录或 token 失效（后端重启过），重新登录 |
| 端口 8080/3000 被占用 | 改 `web/src/main/resources/application.yml` 的 `server.port` 与 `web-frontend/vite.config.js` 的端口/代理 |
| 中文变问号 | 启动 JVM 必须带 `-Dfile.encoding=UTF-8`；数据库导入用 `--default-character-set=utf8mb4` |
| 前端 `npm install` 慢 | 用 `npm install --registry=https://registry.npmmirror.com` |
| `build-web.bat` 报无法删除 jar | 后端服务还在运行占用文件，先停掉再构建 |
