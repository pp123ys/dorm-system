# 寓安 · 学生宿舍管理系统

Java 8 语法 + 原生 JDBC + MySQL。同一份 DAO / Service 业务代码支撑**两个入口**：

| 入口 | 形态 | 目录 | 启动方式 |
|---|---|---|---|
| **控制台版** | 命令行交互菜单，纯 `javac` 脚本工程 | `src/` + `build.bat` / `run.bat` | 双击 `run.bat` |
| **Web 版** | 前后端分离，Spring Boot REST + Vue3 界面 | `web/` + `web-frontend/` | `build-web.bat` → `java -jar` → `npm run dev` |

功能：管理员登录 → 楼栋管理 → 房间与床位管理（自动生成床位、扩容缩容、停用）→ 学生管理 →
入住退住办理（严格校验 + 事务 + 流水）→ 查询统计（占用概览、住宿名单、空床位、住宿信息、流水）。

---

## 环境要求（本机实测版本）

| 项 | 要求 | 说明 |
|---|---|---|
| JDK | **1.8**（本机 `1.8.0_191`） | 源码只用 Java 8 语法，实测可编译运行 |
| MySQL | **5.5.28**（本机实测） | `db/schema.sql` 刻意避开 5.6+ 语法（见下） |
| 驱动 | `lib/mysql-connector-java-8.0.30.jar` | 已随项目提供 |
| 控制台版附加依赖 | `lib/spring-*.jar` 5 个 | 由 `fetch-lib.bat` 自动补齐，见「控制台版」一节 |
| Web 版构建 | Maven 3.9.x + Node.js 20+ | 本机 Maven 3.9.14、Node 24.14.1 |
| 编码 | 源码 / 脚本 / 输入统一 UTF-8 | 控制台启动**必须**带 `-Dfile.encoding=UTF-8` |

> **两个与 MySQL 5.5 有关的坑（已修复）**：
> 1. `t_checkin.create_time` 必须用 `TIMESTAMP`——MySQL 5.5 的 `DATETIME` 不支持
>    `DEFAULT CURRENT_TIMESTAMP`（5.6.5 才支持），用 `DATETIME` 会直接建表失败。
> 2. 示例数据改为幂等写法（`WHERE NOT EXISTS`），脚本可重复执行且不会重复键报错。

---

## 数据库

连接信息硬编码在 `src/net/wanhe/dormsystem/util/JdbcUtil.java` 与 `web/src/main/resources/application.yml`：
`localhost:3306` / 库 `dorm_system` / `root` / `123456`。

```powershell
cmd /c "mysql --host=127.0.0.1 --user=root -p123456 --default-character-set=utf8mb4 < db\schema.sql"
```

**6 张表**：`t_user`（管理员）、`t_building`（楼栋）、`t_room`（房间）、`t_bed`（床位）、
`t_student`（学生 + 入住关系 `bed_id`）、`t_checkin`（入住退住流水，只增不改）。

**示例数据**：管理员 `admin/123456`；1号楼（男）、2号楼（女）；每栋 3 间房、每间 4 床；5 名学生（初始均未入住）。

> 脚本可重复执行：会 drop 并重建 6 张宿舍表，`t_user` 用条件插入保留。

---

## 控制台版

```bat
build.bat        :: 编译 src 到 net\，成功输出 build ok
run.bat          :: 交互式运行（admin / 123456）
```

- `build.bat` 会先调用 `fetch-lib.bat`：`JdbcUtil` 改为使用 Spring 的
  `DataSourceUtils`/`DataSourceTransactionManager`，使同一份 DAO+Service 代码在控制台模式
  （无 Spring 容器）与 Web 模式（Spring 管事务）下都能工作。这 5 个 Spring jar 体积大、不入库，
  脚本会从本地 Maven 仓库自动水合到 `lib\`（已存在则跳过，可用 `fetch-lib.bat -force` 强制重拷）。
- **`-Dfile.encoding=UTF-8` 不能省**：本机 JDK 8 默认 GBK，不指定会导致中文菜单乱码、
  中文输入落库乱码。`run.bat` 里已内置。

### 验收（实测 54 class = 54 源文件）

```powershell
cmd /c "mysql --host=127.0.0.1 --user=root -p123456 --default-character-set=utf8mb4 < db\schema.sql"
.\build.bat
# 用输入脚本喂给程序（注意：不要去重定向 run.bat，见 BUILD.md 4.1 节）
cmd /c "java -Dfile.encoding=UTF-8 -cp `".;net;lib\*`" net.wanhe.dormsystem.Run < 输入.txt"
```

通过标准：出现 `登录成功`、`办理入住成功`、`谢谢使用`，且不出现 `Exception`。

---

## Web 版（前后端分离）

```
浏览器 ──HTTP/JSON──► Spring Boot 2.7.18 (Tomcat 8080, /api) ──JDBC──► MySQL
   Vue3 前端                复用既有 DAO + Service                dorm_system
   Vite 3000（/api 反代到 8080）
```

```bat
:: 1) 后端
build-web.bat                                       :: Maven 打包，产出 web\target\dorm-web-1.0.0.jar
java -Dfile.encoding=UTF-8 -jar web\target\dorm-web-1.0.0.jar

:: 2) 前端
cd web-frontend
npm install
npm run dev                                         :: 浏览器打开 http://localhost:3000
```

- **29 个 REST 接口**，除 `/api/health`、`/api/auth/login` 外都需要请求头 `X-Token`。
- 鉴权：登录返回 token 存服务端内存，前端存 localStorage；token 失效自动跳登录页。
- 接口验收脚本（**49 项断言，可重复运行**，会自动把数据库恢复到种子状态）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File web\acceptance.ps1
```

> `build-web.bat` 存在的意义：本机 Maven 默认用 JRE 当 JVM（找不到 `javac`），
> 脚本内部把 `JAVA_HOME` 指向 JDK，无需改机器级环境变量。

详细说明（接口清单、页面清单、踩坑记录、已知限制）见 **[WEB.md](WEB.md)**。

---

## 用 IDEA 打开（可选）

Web 版是标准 Maven 工程，IDEA 直接 `File → Open` 选 `web/pom.xml` 即可。
控制台版是纯脚本工程，需要 IDE 调试时：

1. `File → Open`，选项目根目录
2. `Project Structure → Project` → SDK 选本机 **JDK 1.8**（源码只用了 Java 8 语法）
3. 右键 `src` → `Mark Directory as → Sources Root`
4. `Project Structure → Libraries → + → Java` → 选 `lib/` 下的 jar（至少 mysql 驱动）

之后可直接运行 `net.wanhe.dormsystem.Run`。注意控制台运行时仍需 `-Dfile.encoding=UTF-8`：
在运行配置的 **VM options** 里填上，否则中文会异常。

---

## 目录结构

```
build.bat / run.bat          控制台版编译与运行脚本
fetch-lib.bat               从本地 Maven 仓库补齐控制台版运行所需的 Spring jar
build-web.bat               Web 版 Maven 打包（自动设置 JAVA_HOME 指向 JDK）
db/schema.sql               建库建表 + 示例数据（可重复执行，兼容 MySQL 5.5）
lib/                        MySQL 驱动 + Spring jar（Spring jar 已 gitignore，由 fetch-lib.bat 生成）
src/net/wanhe/dormsystem/   共享业务代码：Run + system/controller/service/dao/pojo/exception/util
web/                        Web 后端（Spring Boot）：pom.xml + src/main + acceptance.ps1
web-frontend/               Web 前端（Vue3 + Vite + Element Plus）
BUILD.md                    控制台版环境说明、三个故障根因与修法、验收记录
WEB.md                      Web 版架构、接口清单、踩坑记录、已知限制
net/ out/ web/target/ web-frontend/dist/   构建产物（均已 gitignore）
```

---

## 常见问题

| 现象 | 原因与处理 |
|---|---|
| `javac: 找不到文件 ...Run.java` | 旧版 `build.bat` 的 argfile 编码问题（路径含中文时必现），已修复；若用旧脚本请更新 |
| `mvn` 报 `No compiler is provided... JRE rather than a JDK` | 用 `build-web.bat`，不要直接 `mvn`（本机 Maven 默认 JVM 是 JRE） |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 用 `-cp ".;net;lib\*"` 运行，或重新 `build-web.bat` |
| 控制台中文乱码 | 必须带 `-Dfile.encoding=UTF-8` |
| 日志里中文乱码 | 读取时用 `Get-Content -Encoding UTF8` |
| `NoSuchElementException: No line found` | 输入脚本行数不够（每个提示对应一行）；或忘了重置数据库导致菜单走向与脚本不符 |
| 提示输入过长 | 列长度上限：楼栋名 ≤50、房间号 ≤20、姓名 ≤50、电话 ≤20、备注 ≤100 |
| 数据库导入报 `ERROR 1067` | 表结构用了 MySQL 5.6+ 语法；本仓库的 `schema.sql` 已改为 5.5 兼容 |
| 想回到初始状态 | 重新执行 `db\schema.sql` |

---

## 文档

- 控制台版环境与故障修复记录：`BUILD.md`
- Web 版架构、接口、踩坑与限制：`WEB.md`
- Web 版接口验收脚本：`web/acceptance.ps1`

## 已知限制（设计阶段的有意取舍）

- 只有单一管理员账号，无角色/权限分级（未做宿管员、学生自助端）
- 换宿用「退住 + 入住」两步完成，没有独立换宿菜单
- 不含违纪扣分、宿舍检查、报修、值日等扩展模块
- 数据库账号密码硬编码（教学项目取舍，便于零配置运行）
- Web 版 token 存服务端内存，后端重启即失效；列表接口未分页；未做移动端适配
  （详见 `WEB.md` 第六节）
- 仓库不含 IDE 配置（`.idea/` 已 gitignore）：命令行构建完全不受影响
