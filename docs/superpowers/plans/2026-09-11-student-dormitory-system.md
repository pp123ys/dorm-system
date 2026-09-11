# 学生宿舍管理系统 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `D:\text\edusystem` 现有控制台「教育系统」原地改造为「学生宿舍管理系统」：楼栋/房间/床位管理、学生管理、入住退住办理（含流水）、查询统计，保持原有四层分层风格与脚本构建方式。

**Architecture:** 包名从 `net.wanhe.edusystem` 迁移到 `net.wanhe.dorm`，沿用 `System`（菜单循环）→ `Controller`（交互展示）→ `Service`（业务校验）→ `Dao`（JDBC）四层。住关系只存在 `t_student.bed_id`；入住/退住「改床位 + 写流水」用 `JdbcUtil` 的 `ThreadLocal` 事务保证原子性。

**Tech Stack:** Java（源码只用 Java 8 语法，本机用 JDK 21 编译）、原生 JDBC + MySQL Connector/J 8.0.30、MySQL 服务端 5.7.34、`javac`/`java` 脚本构建（无 Maven/Gradle）、无测试框架（用控制台输入脚本 + SQL 断言做验收）。

**规格来源：** `docs/superpowers/specs/2026-09-11-student-dormitory-system-design.md`（已评审通过）

---

## 全局约定

这些约定在每个任务里都成立，实现时不要各写各的：

1. **输入统一走 `ScannerUtil`**，禁止直接 `sc.nextInt()`（JDK 的 `nextInt` 遇到字母会抛 `InputMismatchException` 把程序打崩）。所有读取都用 `nextLine()` 实现，`nextNonEmpty` 用于必填文本。
2. **日志与输出里出现的中文提示文案**按各任务给出的原文写，验收脚本会按这些文案做断言。
3. **Dao 的 `insert` 返回自增主键**（`Statement.RETURN_GENERATED_KEYS`），取不到时返回 0；`update`/`delete` 返回受影响行数。
4. **每个 DaoImpl 写一个私有 `map(ResultSet)` 方法**，同一个查询列只映射一次，避免各查询重复写字段赋值。
5. **Service 接口方法声明 `throws XxxException`**（异常类沿用现有风格 `extends Throwable`），Controller 只 `catch (XxxException e)` 后 `System.out.println(e.getMessage())`。
6. **ServiceImpl 用反射创建 Dao**（`Class.forName("net.wanhe.dorm.dao.impl.XxxDaoImpl")`），与现有 `UserServiceImpl`/`ClazzServiceImpl` 写法保持一致。
7. **事务模板**（只用于入住/退住/新增房间/调整容量这类多写操作）：
   ```java
   try {
       JdbcUtil.beginTransaction();
       // ... 多个 Dao 写操作 ...
       JdbcUtil.commit();
   } catch (RuntimeException e) {
       JdbcUtil.rollbackQuietly();
       throw e;
   }
   ```
8. **验证命令统一用 `cmd /c` 做输入重定向**（PowerShell 不支持 `<`），日志用 `Get-Content -Encoding UTF8` 读，否则中文会因默认 ANSI 编码显示成乱码。
9. **SQL 校验命令前置一行** `$env:MYSQL_PWD="123456"`，然后 `mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="..."`。
10. **每个任务结束都要提交**（`git add -A` + `git commit`）。若 `git commit` 报 "Please tell me who you are"，先执行 Task 1 的 Step 3 配置身份。

### 各模块定位用的键（全局统一，避免脚本写错）

| 对象 | 定位键 |
|---|---|
| 学生 | **学号**（业务主键，不可修改） |
| 楼栋 / 房间 / 床位 | 各自的 **id** |

### 输入脚本约定

- 每个任务的验收脚本放在 `test-inputs/` 目录，UTF-8 无 BOM，每行一个输入（程序所有读取都是整行）。
- **必须用 `write` 工具创建这些脚本**。若改用 PowerShell 生成，绝不能用 `Set-Content -Encoding UTF8`（PowerShell 5.1 会写入 BOM，BOM 会让第一行变成 `\uFEFF6`，`Integer.parseInt` 直接失败）；必须写成：
  ```powershell
  [System.IO.File]::WriteAllText("$PWD\test-inputs\xxx.txt", "6`n", (New-Object System.Text.UTF8Encoding($false)))
  ```
- 每个脚本**开头固定两行** `admin` / `123456`（登录），**结尾固定 `6`**（退出系统）。
- 中间是「主菜单编号 → 子菜单编号 → 各提示的应答」严格顺序，多空行会错位。

---

## 文件结构

### 新建

| 路径 | 职责 |
|---|---|
| `src/net/wanhe/dorm/Run.java` | 入口：登录 → 主菜单循环 |
| `src/net/wanhe/dorm/system/UserSystem.java` | 登录流程（失败重试） |
| `src/net/wanhe/dorm/system/BuildingSystem.java` | 楼栋菜单循环 |
| `src/net/wanhe/dorm/system/RoomSystem.java` | 房间床位菜单循环 |
| `src/net/wanhe/dorm/system/StuSystem.java` | 学生菜单循环 |
| `src/net/wanhe/dorm/system/StaySystem.java` | 入住退住菜单循环 |
| `src/net/wanhe/dorm/system/StatSystem.java` | 查询统计菜单循环 |
| `src/net/wanhe/dorm/controller/UserController.java` | 登录交互 |
| `src/net/wanhe/dorm/controller/BuildingController.java` | 楼栋交互与表格展示 |
| `src/net/wanhe/dorm/controller/RoomController.java` | 房间床位交互与展示 |
| `src/net/wanhe/dorm/controller/StuController.java` | 学生交互与展示 |
| `src/net/wanhe/dorm/controller/StayController.java` | 入住退住交互与展示 |
| `src/net/wanhe/dorm/controller/StatController.java` | 统计查询交互与展示 |
| `src/net/wanhe/dorm/service/*.java`（6 个接口） | 业务接口，声明校验与 `throws` |
| `src/net/wanhe/dorm/service/impl/*.java`（6 个实现） | 校验规则落地、事务边界 |
| `src/net/wanhe/dorm/dao/*.java`（7 个接口） | 数据访问接口 |
| `src/net/wanhe/dorm/dao/impl/*.java`（7 个实现） | JDBC 实现 |
| `src/net/wanhe/dorm/pojo/*.java`（6 个） | `User` `Building` `Room` `Bed` `Student` `Checkin` |
| `src/net/wanhe/dorm/exception/*.java`（5 个） | `UserException` `BuildingException` `RoomException` `StuException` `StayException` |
| `src/net/wanhe/dorm/util/JdbcUtil.java` | 连接 + `ThreadLocal` 事务 |
| `src/net/wanhe/dorm/util/ScannerUtil.java` | 带校验的控制台输入 |
| `src/net/wanhe/dorm/util/AlignUtil.java` | 中文表格对齐（中文算 2 宽） |
| `src/net/wanhe/dorm/util/LoginContext.java` | 保存当前登录管理员名 |
| `test-inputs/*.txt` | 各任务验收输入脚本 |
| `.gitignore` | 构建产物/日志/IDE 目录 |

### 重写

`db/schema.sql`、`build.bat`、`run.bat`、`test_input.txt`

### 删除

`src/net/wanhe/edusystem/**`（整个旧包）、`WriteInput*.java`、根目录与 `src/` 下的 `.class`、`run1.log`、`run2.log`、`net/`（旧编译产物）

### 保留

`lib/mysql-connector-java-8.0.30.jar`、`edusystem.iml`、`data-backup-0908/`（留在磁盘，加进 `.gitignore`，待用户确认后再删）

---

## Task 1: 版本控制初始化（基线）

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: 初始化仓库**

```powershell
git init
```

Expected: `Initialized empty Git repository in D:/text/edusystem/.git/`

- [ ] **Step 2: 写 `.gitignore`**

创建 `D:\text\edusystem\.gitignore`：

```gitignore
# 编译产物(只锚定根目录的 net/; 不加 / 锚会误伤源码树 src/net/)
/net/
*.class
filelist.txt

# 运行日志与输出(根锚定, 避免误伤其他深度的同名目录)
*.log
/out/

# IDE
.idea/

# 改造前的旧序列化数据备份(保留在磁盘, 不入库)
/data-backup-0908/
```

- [ ] **Step 3: 确认提交身份**（已有全局配置的跳过）

```powershell
git config user.name
git config user.email
```

任一条输出为空时执行：

```powershell
git config user.name "edusystem-dev"
git config user.email "dev@example.com"
```

- [ ] **Step 4: 基线提交**

```powershell
git add -A
git commit -m "chore: 改造前基线(教育系统控制台版)"
git log --oneline
```

Expected: 出现一条 `chore: 改造前基线(教育系统控制台版)` 提交。

---

## Task 2: 数据库脚本 `db/schema.sql`（宿舍版）

**Files:**
- Rewrite: `db/schema.sql`

依据规格 §5。本机服务端是 **MySQL 5.7**，示例数据床位必须逐条写，不能用生成式语法。

- [ ] **Step 1: 重写 `db/schema.sql`**

```sql
-- 学生宿舍管理系统 数据库脚本 (rg01)
-- 服务端为 MySQL 5.7, 不使用 8.0 专属语法

CREATE DATABASE IF NOT EXISTS rg01 DEFAULT CHARSET utf8mb4;
USE rg01;
SET NAMES utf8mb4;

-- ============ 管理员表 (沿用原有结构) ============
CREATE TABLE IF NOT EXISTS t_user (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  login_name VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(50) NOT NULL COMMENT '密码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '管理员表';

-- ============ 清理旧结构与旧表 ============
DROP TABLE IF EXISTS t_clazz;
DROP TABLE IF EXISTS t_checkin;
DROP TABLE IF EXISTS t_student;
DROP TABLE IF EXISTS t_bed;
DROP TABLE IF EXISTS t_room;
DROP TABLE IF EXISTS t_building;

-- ============ 楼栋表 ============
CREATE TABLE t_building (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  name VARCHAR(50) NOT NULL UNIQUE COMMENT '楼栋名称',
  sex VARCHAR(4) NOT NULL COMMENT '楼栋类型: 男/女',
  floors INT NOT NULL DEFAULT 6 COMMENT '楼层数',
  remark VARCHAR(100) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '楼栋表';

-- ============ 房间表 ============
CREATE TABLE t_room (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  building_id INT NOT NULL COMMENT '所属楼栋id',
  room_no VARCHAR(20) NOT NULL COMMENT '房间号',
  capacity INT NOT NULL COMMENT '床位数',
  status VARCHAR(10) NOT NULL DEFAULT '正常' COMMENT '状态: 正常/停用',
  UNIQUE KEY uk_room (building_id, room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '房间表';

-- ============ 床位表 (不含 student_id, 占用关系存在 t_student.bed_id) ============
CREATE TABLE t_bed (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  room_id INT NOT NULL COMMENT '所属房间id',
  bed_no INT NOT NULL COMMENT '床位号, 从1开始',
  status VARCHAR(10) NOT NULL DEFAULT '正常' COMMENT '状态: 正常/停用',
  UNIQUE KEY uk_bed (room_id, bed_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '床位表';

-- ============ 学生表 (bed_id 为 NULL 表示未入住) ============
CREATE TABLE t_student (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  no INT NOT NULL COMMENT '学号',
  name VARCHAR(50) NOT NULL COMMENT '姓名',
  sex VARCHAR(4) NOT NULL COMMENT '性别: 男/女',
  age INT COMMENT '年龄',
  phone VARCHAR(20) COMMENT '电话',
  bed_id INT NULL COMMENT '入住的床位id, NULL表示未入住',
  UNIQUE KEY uk_student_no (no),
  UNIQUE KEY uk_student_bed (bed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生表';

-- ============ 入住退住流水表 (位置与学生信息冗余存储, 保证历史可读) ============
CREATE TABLE t_checkin (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  student_id INT NOT NULL COMMENT '学生id',
  student_no INT NOT NULL COMMENT '学号(冗余留痕)',
  student_name VARCHAR(50) NOT NULL COMMENT '姓名(冗余留痕)',
  action VARCHAR(10) NOT NULL COMMENT '操作: 入住/退住',
  building_name VARCHAR(50) COMMENT '楼栋名称(冗余留痕)',
  room_no VARCHAR(20) COMMENT '房间号(冗余留痕)',
  bed_no INT COMMENT '床位号(冗余留痕)',
  operator VARCHAR(50) NOT NULL COMMENT '操作人登录名',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '入住退住流水表';

-- ============ 初始化数据 ============
-- 管理员 admin / 123456 (条件插入: 保证脚本可重复执行, 已有 t_user 数据时不会主键冲突)
INSERT INTO t_user (login_name, password)
SELECT 'admin', '123456' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE login_name = 'admin');

-- 楼栋
INSERT INTO t_building (id, name, sex, floors, remark) VALUES
(1, '1号楼', '男', 6, '男生宿舍'),
(2, '2号楼', '女', 6, '女生宿舍');

-- 房间
INSERT INTO t_room (id, building_id, room_no, capacity, status) VALUES
(1, 1, '101', 4, '正常'),
(2, 1, '102', 4, '正常'),
(3, 1, '103', 4, '正常'),
(4, 2, '201', 4, '正常'),
(5, 2, '202', 4, '正常'),
(6, 2, '203', 4, '正常');

-- 床位: 6 间房 x 4 床
INSERT INTO t_bed (room_id, bed_no, status) VALUES
(1, 1, '正常'), (1, 2, '正常'), (1, 3, '正常'), (1, 4, '正常'),
(2, 1, '正常'), (2, 2, '正常'), (2, 3, '正常'), (2, 4, '正常'),
(3, 1, '正常'), (3, 2, '正常'), (3, 3, '正常'), (3, 4, '正常'),
(4, 1, '正常'), (4, 2, '正常'), (4, 3, '正常'), (4, 4, '正常'),
(5, 1, '正常'), (5, 2, '正常'), (5, 3, '正常'), (5, 4, '正常'),
(6, 1, '正常'), (6, 2, '正常'), (6, 3, '正常'), (6, 4, '正常');

-- 学生: 初始全部未入住(bed_id 为 NULL), 流水表初始为空, 两者严格一致
INSERT INTO t_student (no, name, sex, age, phone, bed_id) VALUES
(2025001, '张三', '男', 18, '13800000001', NULL),
(2025002, '李四', '男', 19, '13800000002', NULL),
(2025003, '王五', '男', 20, '13800000003', NULL),
(2025004, '赵敏', '女', 19, '13800000004', NULL),
(2025005, '周芷', '女', 18, '13800000005', NULL);
```

- [ ] **Step 2: 执行脚本建库建表**

```powershell
$env:MYSQL_PWD="123456"
cmd /c "mysql --host=localhost --user=root --default-character-set=utf8mb4 < db\schema.sql"
```

Expected: 无输出、无报错（`mysql` 静默执行成功）。

- [ ] **Step 3: 校验表与数据量**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="show tables; select count(*) as buildings from t_building; select count(*) as rooms from t_room; select count(*) as beds from t_bed; select count(*) as students from t_student; select count(*) as checkins from t_checkin;"
```

Expected:

```
t_bed  t_building  t_checkin  t_room  t_student  t_user
buildings=2  rooms=6  beds=24  students=5  checkins=0
```

- [ ] **Step 4: 校验中文没有乱码**

```powershell
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select id,name,sex from t_building; select no,name,sex,bed_id from t_student;"
```

Expected: 输出 `1号楼 男` / `2号楼 女`，学生姓名显示 `张三 李四 王五 赵敏 周芷`，`bed_id` 全为 `NULL`（乱码则说明脚本文件编码不是 UTF-8）。

- [ ] **Step 5: 提交**

```powershell
git add db/schema.sql
git commit -m "feat(db): 宿舍版建库建表脚本(6表+初始化数据)"
```

---

## Task 3: 骨架切换（旧包下线 + 工具层 + 异常层 + 构建脚本）

**Files:**
- Delete: `src/net/wanhe/edusystem/**`、`src/WriteInput*.class`、`WriteInput*.java`、`WriteInput*.class`、`run1.log`、`run2.log`、`net/`
- Create: `src/net/wanhe/dorm/util/{JdbcUtil,ScannerUtil,AlignUtil,LoginContext}.java`
- Create: `src/net/wanhe/dorm/exception/{UserException,BuildingException,RoomException,StuException,StayException}.java`
- Create: `src/net/wanhe/dorm/Run.java`（暂不带登录，五个模块先打印「该功能尚未实现」）
- Rewrite: `build.bat`、`run.bat`

- [ ] **Step 1: 删除旧包与遗留文件**

注意：`WriteInput*.java` 在 `src\` 下（根目录只有 3 个 `.class`），命令按实际路径写：

```powershell
Remove-Item -Recurse -Force src\net\wanhe\edusystem
Remove-Item -Force src\WriteInput.java, src\WriteInput2.java, src\WriteInput3.java
Remove-Item -Force src\WriteInput.class, src\WriteInput2.class, src\WriteInput3.class
Remove-Item -Force WriteInput.class, WriteInput2.class, WriteInput3.class
Remove-Item -Force run1.log, run2.log
Remove-Item -Recurse -Force net
```

Expected: 无报错；`Get-ChildItem src\net\wanhe` 下只剩 `dorm`（此时尚未创建，可为空或不存在）。

- [ ] **Step 1b: `.gitignore` 补齐锚定（编排者补充，质量审查指出）**

把 `.gitignore` 里 `out/`、`data-backup-0908/` 两条改成根锚定 `/out/`、`/data-backup-0908/`（与 Task 1 修正 `/net/` 同理：未锚定的目录规则会匹配任意深度，误伤同名子目录）。

- [ ] **Step 2: 写 `util/JdbcUtil.java`**

```java
package net.wanhe.dorm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * JDBC 工具类: 集中处理 连接数据库 / 关闭资源 / 事务
 * 事务连接用 ThreadLocal 持有, 事务中的各 Dao 自动复用同一个连接,
 * 因此 Dao 的方法签名不需要为事务做任何改动
 */
public class JdbcUtil {

    //连接数据库的固定信息
    private static final String URL = "jdbc:mysql://localhost:3306/rg01?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    //当前线程的事务连接, 为 null 表示当前不在事务中
    private static final ThreadLocal<Connection> TX = new ThreadLocal<>();

    static {
        //1.注册驱动 (MySQL 8 的驱动类名是 com.mysql.cj.jdbc.Driver)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 2.获取一个对数据库的连接对象
     * 事务中返回的是同一个连接, 保证多条SQL在同一个事务里
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = TX.get();
        if (conn != null) {
            return conn;
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /*
     * 开启事务
     * 不支持嵌套: 已有事务时直接报错, 避免旧连接被孤儿化导致静默丢失
     */
    public static void beginTransaction() {
        if (TX.get() != null) {
            throw new IllegalStateException("事务已开启, 不支持嵌套开启事务");
        }
        try {
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            conn.setAutoCommit(false);
            TX.set(conn);
        } catch (SQLException e) {
            throw new RuntimeException("开启事务失败", e);
        }
    }

    /*
     * 提交事务并归还连接
     */
    public static void commit() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("提交事务失败", e);
        } finally {
            TX.remove();
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 回滚事务并归还连接
     * 回滚自身失败只打印堆栈, 不掩盖真正的业务异常
     */
    public static void rollbackQuietly() {
        Connection conn = TX.get();
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            TX.remove();
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 8.关闭资源
     * 事务中的连接不在这里关闭, 由 commit / rollbackQuietly 统一归还
     */
    public static void close(ResultSet rs, PreparedStatement state, Connection conn) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (state != null) {
                state.close();
            }
            if (conn != null && TX.get() == null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

- [ ] **Step 3: 写 `util/ScannerUtil.java`**

```java
package net.wanhe.dorm.util;

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
```

- [ ] **Step 4: 写 `util/AlignUtil.java`**

```java
package net.wanhe.dorm.util;

/*
 * 控制台表格对齐工具
 * 中文等全角字符在控制台占 2 个字符宽, 直接 String.format 会错位,
 * 这里统一按显示宽度补齐
 */
public class AlignUtil {

    /*
     * 计算字符串的显示宽度: 全角/CJK 字符算 2, 其他算 1
     */
    public static int width(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += isFullWidth(s.charAt(i)) ? 2 : 1;
        }
        return w;
    }

    private static boolean isFullWidth(char c) {
        return c >= 0x1100 && (
                c <= 0x115F
                        || c == 0x2329 || c == 0x232A
                        || (c >= 0x2E80 && c <= 0xA4CF && c != 0x303F)
                        || (c >= 0xAC00 && c <= 0xD7A3)
                        || (c >= 0xF900 && c <= 0xFAFF)
                        || (c >= 0xFE30 && c <= 0xFE6F)
                        || (c >= 0xFF00 && c <= 0xFF60)
                        || (c >= 0xFFE0 && c <= 0xFFE6));
    }

    /*
     * 右侧补空格到显示宽度 w
     */
    public static String padRight(String s, int w) {
        StringBuilder sb = new StringBuilder(s == null ? "" : s);
        while (width(sb.toString()) < w) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /*
     * 打印一行表格: 每列 "| " + 补齐内容 + " ", 末尾再补一个 "|"
     */
    public static void printRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append("| ").append(padRight(cells[i], widths[i])).append(' ');
        }
        System.out.println(sb.append('|').toString());
    }

    /*
     * 打印分隔线: 每列宽度 = 列宽 + 2, 与 printRow 的占位严格对应
     */
    public static void printLine(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int w : widths) {
            sb.append('+');
            for (int i = 0; i < w + 2; i++) {
                sb.append('-');
            }
        }
        System.out.println(sb.append('+').toString());
    }
}
```

- [ ] **Step 5: 写 `util/LoginContext.java`**

```java
package net.wanhe.dorm.util;

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
```

- [ ] **Step 6: 写 5 个异常类**

`src/net/wanhe/dorm/exception/UserException.java`：

```java
package net.wanhe.dorm.exception;

public class UserException extends Throwable {

    public UserException() {
    }

    public UserException(String message) {
        super(message);
    }

    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserException(Throwable cause) {
        super(cause);
    }

    public UserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
```

其余四个类**内容与上面完全一致**，只把类名和构造器名换成对应名字，四份分别写在：

- `exception/BuildingException.java` → `BuildingException`
- `exception/RoomException.java` → `RoomException`
- `exception/StuException.java` → `StuException`
- `exception/StayException.java` → `StayException`

（包名都是 `net.wanhe.dorm.exception`，都 `extends Throwable`，与现有 `ClazzException` 风格一致。）

- [ ] **Step 7: 写 `Run.java`（本阶段版本：无登录，五模块占位）**

```java
package net.wanhe.dorm;

import net.wanhe.dorm.util.ScannerUtil;

public class Run {

    public static void main(String[] args) {
        boolean f = true;
        while (f) {
            int c = print();
            switch (c) {
                case 1:
                    System.out.println("该功能尚未实现");
                    break;
                case 2:
                    System.out.println("该功能尚未实现");
                    break;
                case 3:
                    System.out.println("该功能尚未实现");
                    break;
                case 4:
                    System.out.println("该功能尚未实现");
                    break;
                case 5:
                    System.out.println("该功能尚未实现");
                    break;
                case 6:
                    f = false;
                    System.out.println("谢谢使用");
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }

    public static int print() {
        System.out.println("--学生宿舍管理系统--");
        System.out.println("1.楼栋管理");
        System.out.println("2.房间床位管理");
        System.out.println("3.学生管理");
        System.out.println("4.入住退住办理");
        System.out.println("5.查询统计");
        System.out.println("6.退出系统");
        return ScannerUtil.nextInt("请选择:");
    }
}
```

- [ ] **Step 8: 重写 `build.bat`**

```bat
@echo off
cd /d %~dp0
rem compile all java files under src\net\wanhe\dorm into net\
dir /s /b src\net\wanhe\dorm\*.java > filelist.txt
javac -encoding UTF-8 -cp "lib\*" -d net @filelist.txt
if errorlevel 1 goto :failed
del filelist.txt
echo build ok
goto :eof
:failed
del filelist.txt
echo build failed
exit /b 1
```

- [ ] **Step 9: 重写 `run.bat`**

注意：**去掉了 `run.bat` 内部自带的 `< test_input.txt` 重定向**，改为由调用方显式重定向，否则每个任务的验收脚本都会被这个内置重定向覆盖掉。同时补上 `stdin/stdout/stderr` 编码参数（JDK 18+ 起 `file.encoding` 不再决定标准流编码，不补会在管道重定向时中文乱码）。

另外**两个 `.bat` 都不保留 `chcp 65001 >nul`**（执行中发现的 Windows 批处理陷阱）：批文件的 stdin 被重定向时，`chcp` 会把后续命令的 stdin 重置回控制台，导致 `run.bat < input.txt` 的输入被丢弃（java 立刻读到 EOF）；编码由上面的 `-D*.encoding=UTF-8` 参数保证，与 `chcp` 无关。交互式运行想看中文可在自己的控制台先执行 `chcp 65001`。

```bat
@echo off
cd /d %~dp0
call build.bat
java -Dfile.encoding=UTF-8 -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp ".;net;lib\*" net.wanhe.dorm.Run
```

- [ ] **Step 10: 编译**

```powershell
.\build.bat
```

Expected: 输出 `build ok`，退出码 0。若出现 `error: package net.wanhe.dorm.util does not exist` 说明文件路径或包名写错。

- [ ] **Step 11: 跑通空壳菜单**

先用 `write` 工具创建 `test-inputs/skeleton.txt`，内容就一行：

```
6
```

再执行：

```powershell
New-Item -ItemType Directory -Force out | Out-Null
cmd /c "run.bat < test-inputs\skeleton.txt > out\skeleton.log 2>&1"
Get-Content out\skeleton.log -Encoding UTF8
```

Expected:

```
build ok
--学生宿舍管理系统--
1.楼栋管理
2.房间床位管理
3.学生管理
4.入住退住办理
5.查询统计
6.退出系统
请选择:
谢谢使用
```

（`谢谢使用` 上面那行 `请选择:` 是菜单提示本身；程序读到 `6` 后退出，不再有输入。）

- [ ] **Step 12: 提交**

```powershell
git add -A
git commit -m "refactor: 包迁移到 net.wanhe.dorm, 下线旧 edusystem 包与遗留文件"
```

---

## Task 4: POJO 六类

**Files:**
- Create: `src/net/wanhe/dorm/pojo/{User,Building,Room,Bed,Student,Checkin}.java`

说明：**不再 `implements Serializable`**（旧项目实现它是为了文件序列化存储，现在数据全在 MySQL，序列化已无意义）。展示辅助字段在类里用注释标出「仅查询展示用」，它们不参与 insert/update。

- [ ] **Step 1: 写 `pojo/User.java`**

```java
package net.wanhe.dorm.pojo;

public class User {

    private int id;

    private String loginName;

    private String password;

    public User() {
    }

    public User(String loginName, String password) {
        this.loginName = loginName;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

- [ ] **Step 2: 写 `pojo/Building.java`**

```java
package net.wanhe.dorm.pojo;

public class Building {

    private int id;

    private String name;

    private String sex;

    private int floors;

    private String remark;

    //以下三个字段仅查询展示用(占用概览), 不参与 insert/update
    private int roomCount;

    private int bedCount;

    private int occupiedCount;

    public Building() {
    }

    public Building(String name, String sex, int floors, String remark) {
        this.name = name;
        this.sex = sex;
        this.floors = floors;
        this.remark = remark;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getFloors() {
        return floors;
    }

    public void setFloors(int floors) {
        this.floors = floors;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(int roomCount) {
        this.roomCount = roomCount;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    //空床数 = 可分配床位数 - 已住人数(展示用)
    public int getFreeCount() {
        return bedCount - occupiedCount;
    }
}
```

- [ ] **Step 3: 写 `pojo/Room.java`**

```java
package net.wanhe.dorm.pojo;

public class Room {

    private int id;

    private int buildingId;

    private String roomNo;

    private int capacity;

    private String status;

    //以下两个字段仅查询展示用, 不参与 insert/update
    private String buildingName;

    private int occupiedCount;

    public Room() {
    }

    public Room(int buildingId, String roomNo, int capacity, String status) {
        this.buildingId = buildingId;
        this.roomNo = roomNo;
        this.capacity = capacity;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(int occupiedCount) {
        this.occupiedCount = occupiedCount;
    }

    //空床位数(展示用)
    public int getFreeCount() {
        return capacity - occupiedCount;
    }
}
```

- [ ] **Step 4: 写 `pojo/Bed.java`**

```java
package net.wanhe.dorm.pojo;

public class Bed {

    private int id;

    private int roomId;

    private int bedNo;

    private String status;

    //以下四个字段仅查询展示用(LEFT JOIN 带出), 不参与 insert/update
    private String buildingName;

    private String roomNo;

    private Integer studentNo;

    private String studentName;

    public Bed() {
    }

    public Bed(int roomId, int bedNo, String status) {
        this.roomId = roomId;
        this.bedNo = bedNo;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getBedNo() {
        return bedNo;
    }

    public void setBedNo(int bedNo) {
        this.bedNo = bedNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(Integer studentNo) {
        this.studentNo = studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    //该床位是否已有人住(由查询带出的 studentNo 判断)
    public boolean isOccupied() {
        return studentNo != null;
    }

    //住宿位置文字, 用于提示信息
    public String location() {
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
```

- [ ] **Step 5: 写 `pojo/Student.java`**

```java
package net.wanhe.dorm.pojo;

public class Student {

    private int id;

    private int no;

    private String name;

    private String sex;

    private Integer age;

    private String phone;

    //入住的床位id, null 表示未入住
    private Integer bedId;

    //以下三个字段仅查询展示用(LEFT JOIN 带出), 不参与 insert/update
    private String buildingName;

    private String roomNo;

    private Integer bedNo;

    public Student() {
    }

    public Student(int no, String name, String sex, Integer age, String phone) {
        this.no = no;
        this.name = name;
        this.sex = sex;
        this.age = age;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getBedId() {
        return bedId;
    }

    public void setBedId(Integer bedId) {
        this.bedId = bedId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getBedNo() {
        return bedNo;
    }

    public void setBedNo(Integer bedNo) {
        this.bedNo = bedNo;
    }

    //是否已入住
    public boolean isCheckedIn() {
        return bedId != null;
    }

    //住宿位置文字, 未入住返回 "未入住"
    public String location() {
        if (bedId == null) {
            return "未入住";
        }
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
```

- [ ] **Step 6: 写 `pojo/Checkin.java`**

```java
package net.wanhe.dorm.pojo;

import java.util.Date;

/*
 * 入住退住流水
 * 楼栋名称/房间号/床位号/学号/姓名都是冗余留痕:
 * 日后房间或学生被删除, 历史记录依然可读
 */
public class Checkin {

    private int id;

    private int studentId;

    private int studentNo;

    private String studentName;

    //操作: 入住/退住
    private String action;

    private String buildingName;

    private String roomNo;

    private Integer bedNo;

    private String operator;

    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(int studentNo) {
        this.studentNo = studentNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public Integer getBedNo() {
        return bedNo;
    }

    public void setBedNo(Integer bedNo) {
        this.bedNo = bedNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    //住宿位置文字, 用于流水展示
    public String location() {
        return buildingName + roomNo + "房" + bedNo + "床";
    }
}
```

- [ ] **Step 7: 编译并把骨架跑通一遍**

```powershell
.\build.bat
```

Expected: `build ok`（POJO 目前还没人使用，编译只验证语法）。

- [ ] **Step 8: 提交**

```powershell
git add src/net/wanhe/dorm/pojo
git commit -m "feat(pojo): 宿舍系统六个实体类"
```

---

## Task 5: 登录模块

**Files:**
- Create: `dao/UserDao.java`、`dao/impl/UserDaoImpl.java`、`service/UserService.java`、`service/impl/UserServiceImpl.java`、`controller/UserController.java`、`system/UserSystem.java`
- Modify: `Run.java`（main 开头加登录）
- Create: `test-inputs/login.txt`、`test-inputs/login-fail.txt`

- [ ] **Step 1: 写 `dao/UserDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.User;

public interface UserDao {

    /*
     * 按登录名查询管理员, 不存在返回 null
     */
    User selectByLoginName(String loginName);
}
```

- [ ] **Step 2: 写 `dao/impl/UserDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.UserDao;
import net.wanhe.dorm.pojo.User;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDaoImpl implements UserDao {

    @Override
    public User selectByLoginName(String loginName) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            //1.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //2.定义SQL语句并预编译
            String sql = "select id,login_name,password from t_user where login_name = ?";
            state = conn.prepareStatement(sql);
            //3.为?赋值
            state.setString(1, loginName);
            //4.执行查询
            rs = state.executeQuery();
            //5.处理结果集
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setLoginName(rs.getString("login_name"));
                user.setPassword(rs.getString("password"));
                return user;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("UserDao.selectByLoginName失败", e);
        } finally {
            //6.关闭资源
            JdbcUtil.close(rs, state, conn);
        }
    }
}
```

- [ ] **Step 3: 写 `service/UserService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.UserException;
import net.wanhe.dorm.pojo.User;

public interface UserService {

    /*
     * 登录, 用户名或密码错误时抛 UserException
     */
    User login(String loginName, String password) throws UserException;
}
```

- [ ] **Step 4: 写 `service/impl/UserServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.UserDao;
import net.wanhe.dorm.exception.UserException;
import net.wanhe.dorm.pojo.User;
import net.wanhe.dorm.service.UserService;

public class UserServiceImpl implements UserService {

    private UserDao userDao;

    public UserServiceImpl() {
        /*
         * 反射: 通过类名创建Dao对象
         * 服务层与Dao实现类解耦(与现有 UserServiceImpl/ClazzServiceImpl 写法一致)
         */
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.UserDaoImpl");
            userDao = (UserDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建UserDao失败", e);
        }
    }

    @Override
    public User login(String loginName, String password) throws UserException {
        User user = userDao.selectByLoginName(loginName);
        if (user == null || !user.getPassword().equals(password)) {
            throw new UserException("用户名或密码错误");
        }
        return user;
    }
}
```

- [ ] **Step 5: 写 `controller/UserController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.UserException;
import net.wanhe.dorm.pojo.User;
import net.wanhe.dorm.service.UserService;
import net.wanhe.dorm.service.impl.UserServiceImpl;
import net.wanhe.dorm.util.LoginContext;
import net.wanhe.dorm.util.ScannerUtil;

public class UserController {

    private UserService userService = new UserServiceImpl();

    /*
     * 登录: 成功返回 true 并记录当前登录名, 失败返回 false 由 UserSystem 重试
     */
    public boolean login() {
        String loginName = ScannerUtil.nextNonEmpty("请输入用户名:");
        String password = ScannerUtil.nextNonEmpty("请输入密码:");
        try {
            User user = userService.login(loginName, password);
            LoginContext.setCurrentUser(user.getLoginName());
            System.out.println("登录成功");
            return true;
        } catch (UserException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
```

- [ ] **Step 6: 写 `system/UserSystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.UserController;

/*
 * 登录流程: 失败就一直重新输入, 成功才返回
 */
public class UserSystem {

    private UserController userController = new UserController();

    public void run() {
        while (!userController.login()) {
            System.out.println("请重新登录");
        }
    }
}
```

- [ ] **Step 7: 在 `Run.java` 里接上登录**

在 `Run.java` 的 import 区加一行：

```java
import net.wanhe.dorm.system.UserSystem;
```

并在 `main` 方法开头（`boolean f = true;` 之前）插入：

```java
        UserSystem us = new UserSystem();
        us.run();
```

- [ ] **Step 8: 编译**

```powershell
.\build.bat
```

Expected: `build ok`

- [ ] **Step 9: 写成功用例脚本 `test-inputs/login.txt`**

内容（用户名 / 密码 / 主菜单选 6 退出，共 3 行）：

```
admin
123456
6
```

- [ ] **Step 10: 写失败重试用例脚本 `test-inputs/login-fail.txt`**

```
admin
wrong
admin
123456
6
```

- [ ] **Step 11: 跑两个脚本**

```powershell
cmd /c "run.bat < test-inputs\login.txt > out\login.log 2>&1"
cmd /c "run.bat < test-inputs\login-fail.txt > out\login-fail.log 2>&1"
Get-Content out\login.log -Encoding UTF8
Get-Content out\login-fail.log -Encoding UTF8
```

Expected（`login.log`）：`登录成功` → 主菜单 → `谢谢使用`。
Expected（`login-fail.log`）：`用户名或密码错误` → `请重新登录` → `请输入用户名:` → `登录成功` → 主菜单 → `谢谢使用`。

- [ ] **Step 12: 提交**

```powershell
git add -A
git commit -m "feat(user): 管理员登录模块"
```

---

## Task 6: 楼栋管理模块

**Files:**
- Create: `dao/BuildingDao.java`、`dao/impl/BuildingDaoImpl.java`、`service/BuildingService.java`、`service/impl/BuildingServiceImpl.java`、`controller/BuildingController.java`、`system/BuildingSystem.java`
- Modify: `Run.java`（case 1）
- Create: `test-inputs/building.txt`

**注意**：「有房间的楼栋不允许删除」这条校验依赖 `RoomDao`，在 **Task 7** 加上（那时 RoomDao 才存在）。本任务先做其余校验与 CRUD。

- [ ] **Step 1: 写 `dao/BuildingDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Building;

import java.util.List;

public interface BuildingDao {

    List<Building> selectAll();

    Building selectById(int id);

    /*
     * 按名称查询, 不存在返回 null
     */
    Building selectByName(String name);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Building building);

    /*
     * 修改(按id), 返回受影响行数
     */
    int update(Building building);

    int delete(int id);
}
```

- [ ] **Step 2: 写 `dao/impl/BuildingDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BuildingDaoImpl implements BuildingDao {

    //查询列, 各查询共用
    private static final String COLS = "id,name,sex,floors,remark";

    @Override
    public List<Building> selectAll() {
        List<Building> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building order by id");
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectAll失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Building selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building where id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Building selectByName(String name) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + " from t_building where name = ?");
            state.setString(1, name);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.selectByName失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Building building) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_building(name,sex,floors,remark) values(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setString(1, building.getName());
            state.setString(2, building.getSex());
            state.setInt(3, building.getFloors());
            state.setString(4, building.getRemark());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Building building) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_building set name=?,sex=?,floors=?,remark=? where id=?");
            state.setString(1, building.getName());
            state.setString(2, building.getSex());
            state.setInt(3, building.getFloors());
            state.setString(4, building.getRemark());
            state.setInt(5, building.getId());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.update失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_building where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BuildingDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Building map(ResultSet rs) throws SQLException {
        Building b = new Building();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        b.setSex(rs.getString("sex"));
        b.setFloors(rs.getInt("floors"));
        b.setRemark(rs.getString("remark"));
        return b;
    }
}
```

- [ ] **Step 3: 写 `service/BuildingService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;

import java.util.List;

public interface BuildingService {

    List<Building> list();

    Building get(int id);

    void add(Building building) throws BuildingException;

    void update(Building building) throws BuildingException;

    void delete(int id) throws BuildingException;
}
```

- [ ] **Step 4: 写 `service/impl/BuildingServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.service.BuildingService;

import java.util.List;

public class BuildingServiceImpl implements BuildingService {

    private BuildingDao buildingDao;

    public BuildingServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
            buildingDao = (BuildingDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建BuildingDao失败", e);
        }
    }

    @Override
    public List<Building> list() {
        return buildingDao.selectAll();
    }

    @Override
    public Building get(int id) {
        return buildingDao.selectById(id);
    }

    @Override
    public void add(Building building) throws BuildingException {
        checkName(building.getName());
        checkSex(building.getSex());
        checkFloors(building.getFloors());
        if (buildingDao.selectByName(building.getName()) != null) {
            throw new BuildingException("该楼栋名称已存在");
        }
        buildingDao.insert(building);
    }

    @Override
    public void update(Building building) throws BuildingException {
        Building old = buildingDao.selectById(building.getId());
        if (old == null) {
            throw new BuildingException("该楼栋不存在");
        }
        checkName(building.getName());
        checkSex(building.getSex());
        checkFloors(building.getFloors());
        Building sameName = buildingDao.selectByName(building.getName());
        if (sameName != null && sameName.getId() != building.getId()) {
            throw new BuildingException("该楼栋名称已存在");
        }
        buildingDao.update(building);
    }

    @Override
    public void delete(int id) throws BuildingException {
        Building old = buildingDao.selectById(id);
        if (old == null) {
            throw new BuildingException("该楼栋不存在");
        }
        buildingDao.delete(id);
    }

    private void checkName(String name) throws BuildingException {
        if (name == null || name.trim().isEmpty()) {
            throw new BuildingException("楼栋名称不能为空");
        }
    }

    private void checkSex(String sex) throws BuildingException {
        if (!"男".equals(sex) && !"女".equals(sex)) {
            throw new BuildingException("楼栋类型只能是 男 或 女");
        }
    }

    private void checkFloors(int floors) throws BuildingException {
        if (floors < 1) {
            throw new BuildingException("楼层数必须大于0");
        }
    }
}
```

- [ ] **Step 5: 写 `controller/BuildingController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.BuildingException;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.service.BuildingService;
import net.wanhe.dorm.service.impl.BuildingServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class BuildingController {

    private BuildingService buildingService = new BuildingServiceImpl();

    public int print() {
        System.out.println("--楼栋管理--");
        System.out.println("1.查看楼栋");
        System.out.println("2.新增楼栋");
        System.out.println("3.修改楼栋");
        System.out.println("4.删除楼栋");
        System.out.println("5.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 查看楼栋
     */
    public void find() {
        List<Building> list = buildingService.list();
        if (list.isEmpty()) {
            System.out.println("暂无楼栋数据");
            return;
        }
        int[] w = {6, 12, 6, 6, 16};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "楼层", "备注"}, w);
        AlignUtil.printLine(w);
        for (Building b : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    b.getName(),
                    b.getSex(),
                    String.valueOf(b.getFloors()),
                    b.getRemark() == null ? "" : b.getRemark()}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增楼栋
     */
    public void add() {
        String name = ScannerUtil.nextNonEmpty("请输入楼栋名称:");
        String sex = ScannerUtil.nextNonEmpty("请输入楼栋类型(男/女):");
        int floors = ScannerUtil.nextInt("请输入楼层数:", 1, 100);
        String remark = ScannerUtil.nextLine("请输入备注(可空):");
        Building building = new Building(name, sex, floors, remark);
        try {
            buildingService.add(building);
            System.out.println("新增成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 修改楼栋
     */
    public void update() {
        int id = ScannerUtil.nextInt("请输入要修改的楼栋id:");
        String name = ScannerUtil.nextNonEmpty("请输入新的楼栋名称:");
        String sex = ScannerUtil.nextNonEmpty("请输入新的楼栋类型(男/女):");
        int floors = ScannerUtil.nextInt("请输入新的楼层数:", 1, 100);
        String remark = ScannerUtil.nextLine("请输入新的备注(可空):");
        Building building = new Building(name, sex, floors, remark);
        building.setId(id);
        try {
            buildingService.update(building);
            System.out.println("修改成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除楼栋
     */
    public void del() {
        int id = ScannerUtil.nextInt("请输入要删除的楼栋id:");
        try {
            buildingService.delete(id);
            System.out.println("删除成功");
        } catch (BuildingException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

- [ ] **Step 6: 写 `system/BuildingSystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.BuildingController;

/*
 * 楼栋管理菜单
 */
public class BuildingSystem {

    private BuildingController buildingController = new BuildingController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = buildingController.print();
            switch (c) {
                case 1:
                    buildingController.find();
                    break;
                case 2:
                    buildingController.add();
                    break;
                case 3:
                    buildingController.update();
                    break;
                case 4:
                    buildingController.del();
                    break;
                case 5:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
```

- [ ] **Step 7: 在 `Run.java` 里接上楼栋菜单**

加 import：

```java
import net.wanhe.dorm.system.BuildingSystem;
```

把 `case 1:` 里的 `System.out.println("该功能尚未实现");` 换成：

```java
                    new BuildingSystem().run();
```

- [ ] **Step 8: 编译**

```powershell
.\build.bat
```

Expected: `build ok`

- [ ] **Step 9: 写脚本 `test-inputs/building.txt`**（按提示顺序逐行）

```
admin
123456
1
2
3号楼
男
8
测试楼栋
1
3
3
1号楼
男
8
重名测试
3
3
3号楼
男
10
修改后的备注
4
999
4
3
1
5
6
```

逐行含义：登录 → 主菜单1(楼栋) → 2(新增:3号楼/男/8层/测试楼栋) → 1(查看) → 3(修改:id=3, 名称填成已存在的`1号楼` → 期望报重名) → 3(再次修改:id=3, 改为`3号楼`/10层/修改后的备注) → 4(删除:id=999 → 期望不存在) → 4(删除:id=3 → 成功) → 1(查看) → 5(返回) → 6(退出)。

- [ ] **Step 10: 跑脚本并检查关键字**

```powershell
cmd /c "run.bat < test-inputs\building.txt > out\building.log 2>&1"
Get-Content out\building.log -Encoding UTF8
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "新增成功" -SimpleMatch
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "该楼栋名称已存在" -SimpleMatch
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "修改成功" -SimpleMatch
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "该楼栋不存在" -SimpleMatch
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "删除成功" -SimpleMatch
Get-Content out\building.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
```

Expected: 前 5 条各命中 1 次；最后一条 `Exception` **无命中**（有命中说明程序崩了，必须修掉再继续）。

- [ ] **Step 11: SQL 复核**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select id,name,sex,floors,remark from t_building order by id;"
```

Expected: 只剩两行 `1号楼 男 6 男生宿舍`、`2号楼 女 6 女生宿舍`（新增的 3 号楼已被删掉，修改与删除都落库了）。

- [ ] **Step 12: 提交**

```powershell
git add -A
git commit -m "feat(building): 楼栋管理模块(增删改查)"
```

---

## Task 7: 房间与床位管理模块

**Files:**
- Create: `dao/RoomDao.java`、`dao/impl/RoomDaoImpl.java`、`dao/BedDao.java`、`dao/impl/BedDaoImpl.java`
- Create: `service/RoomService.java`、`service/impl/RoomServiceImpl.java`、`controller/RoomController.java`、`system/RoomSystem.java`
- Modify: `Run.java`（case 2）、`service/impl/BuildingServiceImpl.java`（补「有房间的楼栋禁止删除」）
- Create: `test-inputs/room.txt`、`test-inputs/building-guard.txt`

关键不变量：**`t_bed` 中该房间的床位数恒等于 `t_room.capacity`**。靠两条规则维持——新增/扩容只从「最大床号+1」往后补，缩容只从「床号最大端」删空床，且床号大于新容量的床位有人住时直接拒绝缩容。

- [ ] **Step 1: 写 `dao/RoomDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Room;

import java.util.List;

public interface RoomDao {

    /*
     * 某楼栋下的房间列表, 带楼栋名与已住人数(展示字段)
     */
    List<Room> selectByBuildingId(int buildingId);

    Room selectById(int id);

    /*
     * 同楼栋内按房间号查询, 用于唯一性校验, 不存在返回 null
     */
    Room selectByBuildingAndNo(int buildingId, String roomNo);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Room room);

    /*
     * 全字段更新(按id), 用于改状态
     */
    int update(Room room);

    /*
     * 只更新床位数
     */
    int updateCapacity(int roomId, int capacity);

    int delete(int id);

    /*
     * 该楼栋下的房间数, 用于删除楼栋前的校验
     */
    int countByBuildingId(int buildingId);

    /*
     * 该房间的在住学生数
     */
    int countOccupied(int roomId);
}
```

- [ ] **Step 2: 写 `dao/impl/RoomDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomDaoImpl implements RoomDao {

    //房间列 + 楼栋名
    private static final String COLS = "r.id, r.building_id, r.room_no, r.capacity, r.status, b.name as building_name";

    //已住人数: 住在这个房间任意床位上的学生数
    private static final String OCCUPIED =
            "(select count(*) from t_student s where s.bed_id in"
                    + " (select bd.id from t_bed bd where bd.room_id = r.id)) as occupied_count";

    private static final String FROM = " from t_room r left join t_building b on b.id = r.building_id";

    @Override
    public List<Room> selectByBuildingId(int buildingId) {
        List<Room> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM
                    + " where r.building_id = ? order by r.room_no");
            state.setInt(1, buildingId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectByBuildingId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Room selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM + " where r.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Room selectByBuildingAndNo(int buildingId, String roomNo) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + ", " + OCCUPIED + FROM
                    + " where r.building_id = ? and r.room_no = ?");
            state.setInt(1, buildingId);
            state.setString(2, roomNo);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.selectByBuildingAndNo失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Room room) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_room(building_id,room_no,capacity,status) values(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, room.getBuildingId());
            state.setString(2, room.getRoomNo());
            state.setInt(3, room.getCapacity());
            state.setString(4, room.getStatus());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Room room) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "update t_room set building_id=?,room_no=?,capacity=?,status=? where id=?");
            state.setInt(1, room.getBuildingId());
            state.setString(2, room.getRoomNo());
            state.setInt(3, room.getCapacity());
            state.setString(4, room.getStatus());
            state.setInt(5, room.getId());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.update失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int updateCapacity(int roomId, int capacity) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_room set capacity=? where id=?");
            state.setInt(1, capacity);
            state.setInt(2, roomId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.updateCapacity失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_room where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int countByBuildingId(int buildingId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select count(*) from t_room where building_id = ?");
            state.setInt(1, buildingId);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.countByBuildingId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int countOccupied(int roomId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "select count(*) from t_student s join t_bed bd on bd.id = s.bed_id where bd.room_id = ?");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("RoomDao.countOccupied失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Room map(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setId(rs.getInt("id"));
        r.setBuildingId(rs.getInt("building_id"));
        r.setRoomNo(rs.getString("room_no"));
        r.setCapacity(rs.getInt("capacity"));
        r.setStatus(rs.getString("status"));
        r.setBuildingName(rs.getString("building_name"));
        r.setOccupiedCount(rs.getInt("occupied_count"));
        return r;
    }
}
```

- [ ] **Step 3: 写 `dao/BedDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Bed;

import java.util.List;

public interface BedDao {

    /*
     * 某房间的全部床位(按床号), 带楼栋/房间/在住学生展示字段
     */
    List<Bed> selectByRoomId(int roomId);

    /*
     * 按id查询, 带展示字段(占用判断依赖它)
     */
    Bed selectById(int id);

    /*
     * 可分配的床位: 床位正常 + 房间正常 + 无人住
     * buildingId 为 null 表示查全部楼栋
     */
    List<Bed> selectFreeBeds(Integer buildingId);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Bed bed);

    int delete(int id);

    /*
     * 删除某房间的全部床位(删房间时用)
     */
    int deleteByRoomId(int roomId);

    /*
     * 删除某房间 bed_no 大于 keepCount 且无人住的床位(缩容时用)
     */
    int deleteFreeBedsAbove(int roomId, int keepCount);

    /*
     * 某房间 bed_no 大于 keepCount 的床位上的在住学生数(缩容前的校验)
     */
    int countOccupiedAbove(int roomId, int keepCount);

    int updateStatus(int id, String status);
}
```

- [ ] **Step 4: 写 `dao/impl/BedDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BedDaoImpl implements BedDao {

    //床位列 + 楼栋名/房间号/在住学生(LEFT JOIN 带出, 用于展示与占用判断)
    private static final String COLS = "bd.id, bd.room_id, bd.bed_no, bd.status, r.room_no,"
            + " bl.name as building_name, s.no as student_no, s.name as student_name";

    private static final String JOINS = " from t_bed bd"
            + " join t_room r on r.id = bd.room_id"
            + " join t_building bl on bl.id = r.building_id"
            + " left join t_student s on s.bed_id = bd.id";

    @Override
    public List<Bed> selectByRoomId(int roomId) {
        List<Bed> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS
                    + " where bd.room_id = ? order by bd.bed_no");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectByRoomId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Bed selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where bd.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public List<Bed> selectFreeBeds(Integer buildingId) {
        List<Bed> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            String sql = "select " + COLS + JOINS
                    + " where bd.status = '正常' and r.status = '正常' and s.id is null";
            if (buildingId != null) {
                sql = sql + " and r.building_id = ?";
            }
            sql = sql + " order by bl.name, r.room_no, bd.bed_no";
            state = conn.prepareStatement(sql);
            if (buildingId != null) {
                state.setInt(1, buildingId);
            }
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.selectFreeBeds失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Bed bed) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_bed(room_id,bed_no,status) values(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, bed.getRoomId());
            state.setInt(2, bed.getBedNo());
            state.setString(3, bed.getStatus());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_bed where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int deleteByRoomId(int roomId) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_bed where room_id = ?");
            state.setInt(1, roomId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.deleteByRoomId失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int deleteFreeBedsAbove(int roomId, int keepCount) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete bd from t_bed bd"
                    + " left join t_student s on s.bed_id = bd.id"
                    + " where bd.room_id = ? and bd.bed_no > ? and s.id is null");
            state.setInt(1, roomId);
            state.setInt(2, keepCount);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.deleteFreeBedsAbove失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int countOccupiedAbove(int roomId, int keepCount) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select count(*) from t_bed bd"
                    + " join t_student s on s.bed_id = bd.id"
                    + " where bd.room_id = ? and bd.bed_no > ?");
            state.setInt(1, roomId);
            state.setInt(2, keepCount);
            rs = state.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("BedDao.countOccupiedAbove失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int updateStatus(int id, String status) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_bed set status = ? where id = ?");
            state.setString(1, status);
            state.setInt(2, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("BedDao.updateStatus失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     * student_no 可能为 NULL, getInt 会返回 0, 这里用 wasNull 还原成 null
     */
    private Bed map(ResultSet rs) throws SQLException {
        Bed b = new Bed();
        b.setId(rs.getInt("id"));
        b.setRoomId(rs.getInt("room_id"));
        b.setBedNo(rs.getInt("bed_no"));
        b.setStatus(rs.getString("status"));
        b.setRoomNo(rs.getString("room_no"));
        b.setBuildingName(rs.getString("building_name"));
        int studentNo = rs.getInt("student_no");
        b.setStudentNo(rs.wasNull() ? null : studentNo);
        b.setStudentName(rs.getString("student_name"));
        return b;
    }
}
```

- [ ] **Step 5: 写 `service/RoomService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Room;

import java.util.List;

public interface RoomService {

    List<Room> listByBuilding(int buildingId) throws RoomException;

    List<Bed> listBeds(int roomId);

    /*
     * 新增房间并自动生成 1~capacity 号床位
     */
    void add(Room room) throws RoomException;

    void updateCapacity(int roomId, int newCapacity) throws RoomException;

    void updateRoomStatus(int roomId, String status) throws RoomException;

    void updateBedStatus(int bedId, String status) throws RoomException;

    /*
     * 删除房间并连带删除其床位(房间内有在住学生时拒绝)
     */
    void delete(int roomId) throws RoomException;
}
```

- [ ] **Step 6: 写 `service/impl/RoomServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.service.RoomService;
import net.wanhe.dorm.util.JdbcUtil;

import java.util.List;

public class RoomServiceImpl implements RoomService {

    private RoomDao roomDao;
    private BedDao bedDao;
    private BuildingDao buildingDao;

    public RoomServiceImpl() {
        roomDao = (RoomDao) newDao("net.wanhe.dorm.dao.impl.RoomDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dorm.dao.impl.BedDaoImpl");
        buildingDao = (BuildingDao) newDao("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
    }

    /*
     * 反射创建Dao, 服务层与Dao实现类解耦
     */
    private static Object newDao(String className) {
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建" + className + "失败", e);
        }
    }

    @Override
    public List<Room> listByBuilding(int buildingId) throws RoomException {
        if (buildingDao.selectById(buildingId) == null) {
            throw new RoomException("楼栋不存在");
        }
        return roomDao.selectByBuildingId(buildingId);
    }

    @Override
    public List<Bed> listBeds(int roomId) {
        return bedDao.selectByRoomId(roomId);
    }

    @Override
    public void add(Room room) throws RoomException {
        if (buildingDao.selectById(room.getBuildingId()) == null) {
            throw new RoomException("楼栋不存在");
        }
        checkRoomNo(room.getRoomNo());
        checkCapacity(room.getCapacity());
        if (roomDao.selectByBuildingAndNo(room.getBuildingId(), room.getRoomNo()) != null) {
            throw new RoomException("该楼栋下已存在房间:" + room.getRoomNo());
        }
        room.setStatus("正常");
        try {
            //房间与床位要么都成功, 要么都回滚
            JdbcUtil.beginTransaction();
            int roomId = roomDao.insert(room);
            for (int i = 1; i <= room.getCapacity(); i++) {
                bedDao.insert(new Bed(roomId, i, "正常"));
            }
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("新增房间失败, 已回滚", e);
        }
    }

    @Override
    public void updateCapacity(int roomId, int newCapacity) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        checkCapacity(newCapacity);
        int occupied = roomDao.countOccupied(roomId);
        if (newCapacity < occupied) {
            throw new RoomException("新床位数不能少于已住人数" + occupied);
        }
        int occupiedAbove = bedDao.countOccupiedAbove(roomId, newCapacity);
        if (occupiedAbove > 0) {
            throw new RoomException("床号大于" + newCapacity + "的床位上有" + occupiedAbove + "名在住学生, 请先办理退住");
        }
        List<Bed> beds = bedDao.selectByRoomId(roomId);
        try {
            JdbcUtil.beginTransaction();
            roomDao.updateCapacity(roomId, newCapacity);
            if (newCapacity > beds.size()) {
                int maxBedNo = 0;
                for (Bed b : beds) {
                    if (b.getBedNo() > maxBedNo) {
                        maxBedNo = b.getBedNo();
                    }
                }
                //从最大床号往后补, 保证床号连续
                for (int i = maxBedNo + 1; i <= newCapacity; i++) {
                    bedDao.insert(new Bed(roomId, i, "正常"));
                }
            } else if (newCapacity < beds.size()) {
                //只删无人住的床位, 且上面已确认床号大于新容量的床位都空着
                bedDao.deleteFreeBedsAbove(roomId, newCapacity);
            }
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("调整房间容量失败, 已回滚", e);
        }
    }

    @Override
    public void updateRoomStatus(int roomId, String status) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        checkStatus(status);
        if ("停用".equals(status) && roomDao.countOccupied(roomId) > 0) {
            throw new RoomException("房间内还有在住学生, 不能停用");
        }
        room.setStatus(status);
        roomDao.update(room);
    }

    @Override
    public void updateBedStatus(int bedId, String status) throws RoomException {
        Bed bed = bedDao.selectById(bedId);
        if (bed == null) {
            throw new RoomException("床位不存在");
        }
        checkStatus(status);
        if ("停用".equals(status) && bed.isOccupied()) {
            throw new RoomException("该床位上有在住学生, 不能停用");
        }
        bedDao.updateStatus(bedId, status);
    }

    @Override
    public void delete(int roomId) throws RoomException {
        Room room = roomDao.selectById(roomId);
        if (room == null) {
            throw new RoomException("房间不存在");
        }
        int occupied = roomDao.countOccupied(roomId);
        if (occupied > 0) {
            throw new RoomException("房间内还有" + occupied + "名在住学生, 不能删除");
        }
        try {
            JdbcUtil.beginTransaction();
            bedDao.deleteByRoomId(roomId);
            roomDao.delete(roomId);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new RoomException("删除房间失败, 已回滚", e);
        }
    }

    private void checkRoomNo(String roomNo) throws RoomException {
        if (roomNo == null || roomNo.trim().isEmpty()) {
            throw new RoomException("房间号不能为空");
        }
    }

    private void checkCapacity(int capacity) throws RoomException {
        if (capacity < 1 || capacity > 20) {
            throw new RoomException("床位数必须在1~20之间");
        }
    }

    private void checkStatus(String status) throws RoomException {
        if (!"正常".equals(status) && !"停用".equals(status)) {
            throw new RoomException("状态只能是 正常 或 停用");
        }
    }
}
```

- [ ] **Step 7: 写 `controller/RoomController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.service.RoomService;
import net.wanhe.dorm.service.impl.RoomServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class RoomController {

    private RoomService roomService = new RoomServiceImpl();

    public int print() {
        System.out.println("--房间床位管理--");
        System.out.println("1.按楼栋查看房间");
        System.out.println("2.查看某房间床位详情");
        System.out.println("3.新增房间");
        System.out.println("4.调整房间容量");
        System.out.println("5.删除房间");
        System.out.println("6.停用/启用房间");
        System.out.println("7.停用/启用床位");
        System.out.println("8.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 按楼栋查看房间(带已住/空床)
     */
    public void findRooms() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
        try {
            List<Room> list = roomService.listByBuilding(buildingId);
            if (list.isEmpty()) {
                System.out.println("该楼栋下暂无房间");
                return;
            }
            int[] w = {8, 10, 10, 8, 6, 8};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"房间id", "楼栋", "房间号", "床位数", "已住", "状态"}, w);
            AlignUtil.printLine(w);
            for (Room r : list) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(r.getId()),
                        r.getBuildingName(),
                        r.getRoomNo(),
                        String.valueOf(r.getCapacity()),
                        String.valueOf(r.getOccupiedCount()),
                        r.getStatus()}, w);
            }
            AlignUtil.printLine(w);
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 查看某房间的床位详情
     */
    public void findBeds() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        List<Bed> list = roomService.listBeds(roomId);
        if (list.isEmpty()) {
            System.out.println("该房间下暂无床位");
            return;
        }
        int[] w = {8, 6, 8, 16};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"床位id", "床号", "状态", "在住学生"}, w);
        AlignUtil.printLine(w);
        for (Bed b : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    String.valueOf(b.getBedNo()),
                    b.getStatus(),
                    b.isOccupied() ? b.getStudentName() + "(" + b.getStudentNo() + ")" : "空"}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增房间(自动生成床位)
     */
    public void add() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
        String roomNo = ScannerUtil.nextNonEmpty("请输入房间号:");
        int capacity = ScannerUtil.nextInt("请输入床位数:", 1, 20);
        Room room = new Room(buildingId, roomNo, capacity, "正常");
        try {
            roomService.add(room);
            System.out.println("新增成功, 已自动生成" + capacity + "个床位");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 调整房间容量(自动补床/删空床)
     */
    public void updateCapacity() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        int capacity = ScannerUtil.nextInt("请输入新的床位数:", 1, 20);
        try {
            roomService.updateCapacity(roomId, capacity);
            System.out.println("调整成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除房间
     */
    public void del() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        try {
            roomService.delete(roomId);
            System.out.println("删除成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 停用/启用房间
     */
    public void updateRoomStatus() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        String status = readStatus();
        try {
            roomService.updateRoomStatus(roomId, status);
            System.out.println("操作成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 停用/启用床位
     */
    public void updateBedStatus() {
        int bedId = ScannerUtil.nextInt("请输入床位id:");
        String status = readStatus();
        try {
            roomService.updateBedStatus(bedId, status);
            System.out.println("操作成功");
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 读取状态: 1.正常 2.停用
     */
    private String readStatus() {
        int s = ScannerUtil.nextInt("请输入状态(1.正常 2.停用):", 1, 2);
        return s == 1 ? "正常" : "停用";
    }
}
```

- [ ] **Step 8: 写 `system/RoomSystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.RoomController;

/*
 * 房间床位管理菜单
 */
public class RoomSystem {

    private RoomController roomController = new RoomController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = roomController.print();
            switch (c) {
                case 1:
                    roomController.findRooms();
                    break;
                case 2:
                    roomController.findBeds();
                    break;
                case 3:
                    roomController.add();
                    break;
                case 4:
                    roomController.updateCapacity();
                    break;
                case 5:
                    roomController.del();
                    break;
                case 6:
                    roomController.updateRoomStatus();
                    break;
                case 7:
                    roomController.updateBedStatus();
                    break;
                case 8:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
```

- [ ] **Step 9: 在 `Run.java` 里接上房间床位菜单**

加 import：

```java
import net.wanhe.dorm.system.RoomSystem;
```

把 `case 2:` 里的 `System.out.println("该功能尚未实现");` 换成：

```java
                    new RoomSystem().run();
```

- [ ] **Step 10: 给 `BuildingServiceImpl` 补「有房间的楼栋不能删」**

在 import 区加两行：

```java
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.impl.RoomDaoImpl;
```

字段区加：

```java
    private RoomDao roomDao;
```

构造器里，反射创建 `BuildingDao` 的那段 try 之后，追加创建 `RoomDao` 的代码（或把两段合并成一个 `newDao` 帮助方法），等价写法：

```java
    public BuildingServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
            buildingDao = (BuildingDao) c.newInstance();
            Class c2 = Class.forName("net.wanhe.dorm.dao.impl.RoomDaoImpl");
            roomDao = (RoomDao) c2.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建Dao失败", e);
        }
    }
```

把 `delete` 方法整个替换为：

```java
    @Override
    public void delete(int id) throws BuildingException {
        Building old = buildingDao.selectById(id);
        if (old == null) {
            throw new BuildingException("该楼栋不存在");
        }
        int rooms = roomDao.countByBuildingId(id);
        if (rooms > 0) {
            throw new BuildingException("该楼栋下还有" + rooms + "个房间, 请先删除房间");
        }
        buildingDao.delete(id);
    }
```

- [ ] **Step 11: 编译**

```powershell
.\build.bat
```

Expected: `build ok`

- [ ] **Step 12: 写脚本 `test-inputs/room.txt`**

```
admin
123456
2
3
1
104
3
3
1
101
4
1
1
2
7
4
7
5
2
7
4
7
1
2
7
7
25
2
6
7
2
5
7
1
1
8
6
```

逐行含义：登录 → 主菜单2(房间床位) → 3(新增:楼栋1/104/3床, 期望`新增成功, 已自动生成3个床位`) → 3(再新增:楼栋1/101/4床, 期望`该楼栋下已存在房间:101`) → 1(按楼栋查看:楼栋1) → 2(床位详情:房间7, 看到 25/26/27 三张床) → 4(调容量:房间7→5床, 期望`调整成功`) → 2(床位详情:房间7, 看到 5 张床) → 4(调容量:房间7→1床, 期望`调整成功`) → 2(床位详情:房间7, 只剩床号1) → 7(停用床位:床位id=25, 状态2=停用, 期望`操作成功`) → 6(停用房间:房间id=7, 状态2, 期望`操作成功`) → 5(删除房间:房间id=7, 期望`删除成功`) → 1(按楼栋查看:楼栋1, 只剩 101/102/103) → 8(返回) → 6(退出)。

> 床位 id 25 = 新房间(第7号房间)的第 1 张床：种子数据 24 张床占 id 1~24，新建房间的床位从 25 开始。

- [ ] **Step 13: 写脚本 `test-inputs/building-guard.txt`**

```
admin
123456
1
4
1
5
6
```

逐行含义：登录 → 主菜单1(楼栋) → 4(删除楼栋:id=1) → 期望 `该楼栋下还有3个房间, 请先删除房间` → 5(返回) → 6(退出)。

- [ ] **Step 14: 跑两个脚本**

```powershell
cmd /c "run.bat < test-inputs\room.txt > out\room.log 2>&1"
cmd /c "run.bat < test-inputs\building-guard.txt > out\building-guard.log 2>&1"
Get-Content out\room.log -Encoding UTF8
Get-Content out\building-guard.log -Encoding UTF8
Get-Content out\room.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
Get-Content out\building-guard.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
```

Expected（`room.log`）：依次出现 `新增成功, 已自动生成3个床位`、`该楼栋下已存在房间:101`、`调整成功`（两次）、`操作成功`（两次）、`删除成功`；两次 `Exception` 检查都无命中。
Expected（`building-guard.log`）：出现 `该楼栋下还有3个房间, 请先删除房间`；`Exception` 无命中。

- [ ] **Step 15: SQL 复核房间床位不变量**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select (select count(*) from t_bed) as bed_rows, (select sum(capacity) from t_room) as capacity_sum; select id,room_no,capacity,status from t_room order by id;"
```

Expected: `bed_rows = capacity_sum = 24`（新建的 104 房间已被删除，不变量回到初始值）；房间列表为 101/102/103/201/202/203，状态全 `正常`。

- [ ] **Step 16: 提交**

```powershell
git add -A
git commit -m "feat(room): 房间床位管理模块(自动生成床位/扩容缩容/停用)"
```

---

## Task 8: 学生管理模块

**Files:**
- Create: `dao/StuDao.java`、`dao/impl/StuDaoImpl.java`、`service/StuService.java`、`service/impl/StuServiceImpl.java`、`controller/StuController.java`、`system/StuSystem.java`
- Modify: `Run.java`（case 3）
- Create: `test-inputs/student.txt`

- [ ] **Step 1: 写 `dao/StuDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StuDao {

    /*
     * 全部学生(按学号), 带住宿位置展示字段
     */
    List<Student> selectAll();

    Student selectById(int id);

    /*
     * 按学号查询(业务主键), 不存在返回 null
     */
    Student selectByNo(int no);

    /*
     * 按床位查询在住学生, 该床位无人住返回 null
     */
    Student selectByBedId(int bedId);

    /*
     * 新增, 返回自增主键, 取不到返回 0
     */
    int insert(Student student);

    /*
     * 只更新 姓名/性别/年龄/电话(按学号定位), 不碰 bed_id
     */
    int update(Student student);

    int delete(int id);

    /*
     * 更新入住床位, bedId 为 null 表示退住
     */
    int updateBedId(int studentId, Integer bedId);
}
```

- [ ] **Step 2: 写 `dao/impl/StuDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class StuDaoImpl implements StuDao {

    //学生列 + 住宿位置(LEFT JOIN 带出, 未入住时三个字段为 null)
    private static final String COLS = "s.id, s.no, s.name, s.sex, s.age, s.phone, s.bed_id,"
            + " bl.name as building_name, r.room_no, bd.bed_no";

    private static final String JOINS = " from t_student s"
            + " left join t_bed bd on bd.id = s.bed_id"
            + " left join t_room r on r.id = bd.room_id"
            + " left join t_building bl on bl.id = r.building_id";

    @Override
    public List<Student> selectAll() {
        List<Student> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " order by s.no");
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectAll失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectById(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.id = ?");
            state.setInt(1, id);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectById失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectByNo(int no) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.no = ?");
            state.setInt(1, no);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectByNo失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public Student selectByBedId(int bedId) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select " + COLS + JOINS + " where s.bed_id = ?");
            state.setInt(1, bedId);
            rs = state.executeQuery();
            return rs.next() ? map(rs) : null;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.selectByBedId失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int insert(Student student) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "insert into t_student(no,name,sex,age,phone,bed_id) values(?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, student.getNo());
            state.setString(2, student.getName());
            state.setString(3, student.getSex());
            setInteger(state, 4, student.getAge());
            state.setString(5, student.getPhone());
            setInteger(state, 6, student.getBedId());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("StuDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public int update(Student student) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(
                    "update t_student set name=?,sex=?,age=?,phone=? where no=?");
            state.setString(1, student.getName());
            state.setString(2, student.getSex());
            setInteger(state, 3, student.getAge());
            state.setString(4, student.getPhone());
            state.setInt(5, student.getNo());
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.update失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int delete(int id) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("delete from t_student where id = ?");
            state.setInt(1, id);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.delete失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    @Override
    public int updateBedId(int studentId, Integer bedId) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("update t_student set bed_id=? where id=?");
            setInteger(state, 1, bedId);
            state.setInt(2, studentId);
            return state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.updateBedId失败", e);
        } finally {
            JdbcUtil.close(null, state, conn);
        }
    }

    /*
     * Integer 赋值: null 要用 setNull, 否则 setInt 会把 null 当 0
     */
    private void setInteger(PreparedStatement state, int index, Integer value) throws SQLException {
        if (value == null) {
            state.setNull(index, Types.INTEGER);
        } else {
            state.setInt(index, value);
        }
    }

    /*
     * 结果集 -> 对象, 各查询共用
     */
    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setNo(rs.getInt("no"));
        s.setName(rs.getString("name"));
        s.setSex(rs.getString("sex"));
        int age = rs.getInt("age");
        s.setAge(rs.wasNull() ? null : age);
        s.setPhone(rs.getString("phone"));
        int bedId = rs.getInt("bed_id");
        s.setBedId(rs.wasNull() ? null : bedId);
        s.setBuildingName(rs.getString("building_name"));
        s.setRoomNo(rs.getString("room_no"));
        int bedNo = rs.getInt("bed_no");
        s.setBedNo(rs.wasNull() ? null : bedNo);
        return s;
    }
}
```

- [ ] **Step 3: 写 `service/StuService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StuService {

    List<Student> list();

    void add(Student student) throws StuException;

    /*
     * 按学号定位修改, 学号本身不可改
     */
    void update(Student student) throws StuException;

    /*
     * 按学号删除(在住学生不允许删除)
     */
    void delete(int no) throws StuException;
}
```

- [ ] **Step 4: 写 `service/impl/StuServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StuService;

import java.util.List;

public class StuServiceImpl implements StuService {

    private StuDao stuDao;

    public StuServiceImpl() {
        try {
            Class c = Class.forName("net.wanhe.dorm.dao.impl.StuDaoImpl");
            stuDao = (StuDao) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建StuDao失败", e);
        }
    }

    @Override
    public List<Student> list() {
        return stuDao.selectAll();
    }

    @Override
    public void add(Student student) throws StuException {
        checkName(student.getName());
        checkSex(student.getSex());
        checkAge(student.getAge());
        if (stuDao.selectByNo(student.getNo()) != null) {
            throw new StuException("该学号已存在");
        }
        stuDao.insert(student);
    }

    @Override
    public void update(Student student) throws StuException {
        Student old = stuDao.selectByNo(student.getNo());
        if (old == null) {
            throw new StuException("该学号的学生不存在");
        }
        checkName(student.getName());
        checkSex(student.getSex());
        checkAge(student.getAge());
        stuDao.update(student);
    }

    @Override
    public void delete(int no) throws StuException {
        Student old = stuDao.selectByNo(no);
        if (old == null) {
            throw new StuException("该学号的学生不存在");
        }
        if (old.isCheckedIn()) {
            throw new StuException("该学生正在" + old.location() + ", 请先办理退住");
        }
        stuDao.delete(old.getId());
    }

    private void checkName(String name) throws StuException {
        if (name == null || name.trim().isEmpty()) {
            throw new StuException("姓名不能为空");
        }
    }

    private void checkSex(String sex) throws StuException {
        if (!"男".equals(sex) && !"女".equals(sex)) {
            throw new StuException("性别只能是 男 或 女");
        }
    }

    private void checkAge(Integer age) throws StuException {
        if (age != null && (age < 10 || age > 100)) {
            throw new StuException("年龄需在10~100之间");
        }
    }
}
```

- [ ] **Step 5: 写 `controller/StuController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StuService;
import net.wanhe.dorm.service.impl.StuServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class StuController {

    private StuService stuService = new StuServiceImpl();

    public int print() {
        System.out.println("--学生管理--");
        System.out.println("1.查看学生");
        System.out.println("2.新增学生");
        System.out.println("3.修改学生");
        System.out.println("4.删除学生");
        System.out.println("5.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 查看学生(带住宿位置)
     */
    public void find() {
        List<Student> list = stuService.list();
        if (list.isEmpty()) {
            System.out.println("暂无学生数据");
            return;
        }
        int[] w = {10, 10, 6, 6, 16, 18};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"学号", "姓名", "性别", "年龄", "电话", "住宿位置"}, w);
        AlignUtil.printLine(w);
        for (Student s : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(s.getNo()),
                    s.getName(),
                    s.getSex(),
                    s.getAge() == null ? "" : String.valueOf(s.getAge()),
                    s.getPhone() == null ? "" : s.getPhone(),
                    s.location()}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 新增学生
     */
    public void add() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        String name = ScannerUtil.nextNonEmpty("请输入姓名:");
        String sex = ScannerUtil.nextNonEmpty("请输入性别(男/女):");
        int age = ScannerUtil.nextInt("请输入年龄(10~100):", 10, 100);
        String phone = ScannerUtil.nextLine("请输入电话(可空):");
        Student student = new Student(no, name, sex, age, phone);
        try {
            stuService.add(student);
            System.out.println("新增成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 修改学生(学号只用于定位, 不可改)
     */
    public void update() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        String name = ScannerUtil.nextNonEmpty("请输入新的姓名:");
        String sex = ScannerUtil.nextNonEmpty("请输入新的性别(男/女):");
        int age = ScannerUtil.nextInt("请输入新的年龄(10~100):", 10, 100);
        String phone = ScannerUtil.nextLine("请输入新的电话(可空):");
        Student student = new Student(no, name, sex, age, phone);
        try {
            stuService.update(student);
            System.out.println("修改成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 删除学生
     */
    public void del() {
        int no = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            stuService.delete(no);
            System.out.println("删除成功");
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

- [ ] **Step 6: 写 `system/StuSystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.StuController;

/*
 * 学生管理菜单
 */
public class StuSystem {

    private StuController stuController = new StuController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = stuController.print();
            switch (c) {
                case 1:
                    stuController.find();
                    break;
                case 2:
                    stuController.add();
                    break;
                case 3:
                    stuController.update();
                    break;
                case 4:
                    stuController.del();
                    break;
                case 5:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
```

- [ ] **Step 7: 在 `Run.java` 里接上学生菜单**

加 import：

```java
import net.wanhe.dorm.system.StuSystem;
```

把 `case 3:` 里的 `System.out.println("该功能尚未实现");` 换成：

```java
                    new StuSystem().run();
```

- [ ] **Step 8: 编译**

```powershell
.\build.bat
```

Expected: `build ok`

- [ ] **Step 9: 写脚本 `test-inputs/student.txt`**

```
admin
123456
3
1
2
2025006
孙悟空
男
500
20
13900000006
2
2025006
猪八戒
男
21
13900000007
3
2025006
孙悟空
男
22
13900000009
4
9999999
4
2025006
1
5
6
```

逐行含义：登录 → 主菜单3(学生管理) → 1(查看, 5 个初始学生) → 2(新增:2025006/孙悟空/男) → 年龄先给 `500`（期望 `请输入 10~100 之间的数字` 后重试）→ 再给 `20` → 电话 → 期望 `新增成功` → 2(再新增同一学号, 期望`该学号已存在`) → 3(修改:2025006/孙悟空/男/22/新电话, 期望`修改成功`) → 4(删除:9999999, 期望`该学号的学生不存在`) → 4(删除:2025006, 期望`删除成功`) → 1(查看, 只剩 5 个初始学生) → 5(返回) → 6(退出)。

- [ ] **Step 10: 跑脚本并检查**

```powershell
cmd /c "run.bat < test-inputs\student.txt > out\student.log 2>&1"
Get-Content out\student.log -Encoding UTF8
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "请输入 10~100 之间的数字" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "新增成功" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "该学号已存在" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "修改成功" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "该学号的学生不存在" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "删除成功" -SimpleMatch
Get-Content out\student.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
```

Expected: 前 6 条各命中至少 1 次（`请输入 10~100 之间的数字` 证明非法年龄被拦下并重试）；`Exception` 无命中。

- [ ] **Step 11: SQL 复核**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select no,name,sex,age,phone,bed_id from t_student order by no;"
```

Expected: 只剩种子数据的 5 名学生（2025001~2025005），`bed_id` 全为 `NULL`；测试新增的 2025006 已被删除。

- [ ] **Step 12: 提交**

```powershell
git add -A
git commit -m "feat(student): 学生管理模块(增删改查, 学号为主键)"
```

---

## Task 9: 入住退住办理模块（含事务与流水）

**Files:**
- Create: `dao/CheckinDao.java`、`dao/impl/CheckinDaoImpl.java`
- Create: `dao/StatDao.java`、`dao/impl/StatDaoImpl.java`（`StayService` 要用其中「有空床的楼栋」这个查询；三个查询在本任务一次写完，Task 10 只补 service/controller 层）
- Create: `service/StayService.java`、`service/impl/StayServiceImpl.java`、`controller/StayController.java`、`system/StaySystem.java`
- Modify: `Run.java`（case 4）
- Create: `test-inputs/stay.txt`

这是全系统唯一有「多写操作」的地方：**改 `t_student.bed_id` + 写 `t_checkin` 必须在同一事务里**。

- [ ] **Step 1: 写 `dao/CheckinDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Checkin;

import java.util.List;

public interface CheckinDao {

    /*
     * 写一条流水(只增不改不删), 返回自增主键
     */
    int insert(Checkin checkin);

    List<Checkin> selectAll();

    List<Checkin> selectByStudentNo(int studentNo);
}
```

- [ ] **Step 2: 写 `dao/impl/CheckinDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CheckinDaoImpl implements CheckinDao {

    private static final String COLS = "id, student_id, student_no, student_name, action,"
            + " building_name, room_no, bed_no, operator, create_time";

    @Override
    public int insert(Checkin checkin) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            //create_time 交给表上的默认值 CURRENT_TIMESTAMP
            state = conn.prepareStatement(
                    "insert into t_checkin(student_id,student_no,student_name,action,"
                            + "building_name,room_no,bed_no,operator) values(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            state.setInt(1, checkin.getStudentId());
            state.setInt(2, checkin.getStudentNo());
            state.setString(3, checkin.getStudentName());
            state.setString(4, checkin.getAction());
            state.setString(5, checkin.getBuildingName());
            state.setString(6, checkin.getRoomNo());
            if (checkin.getBedNo() == null) {
                state.setNull(7, Types.INTEGER);
            } else {
                state.setInt(7, checkin.getBedNo());
            }
            state.setString(8, checkin.getOperator());
            state.executeUpdate();
            rs = state.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("CheckinDao.insert失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    @Override
    public List<Checkin> selectAll() {
        return select("select " + COLS + " from t_checkin order by id", null);
    }

    @Override
    public List<Checkin> selectByStudentNo(int studentNo) {
        return select("select " + COLS + " from t_checkin where student_no = ? order by id", studentNo);
    }

    /*
     * 两个查询只有 SQL 与有没有参数不同, 收在一个私有方法里
     */
    private List<Checkin> select(String sql, Integer param) {
        List<Checkin> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(sql);
            if (param != null) {
                state.setInt(1, param);
            }
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("CheckinDao.select失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 对象
     */
    private Checkin map(ResultSet rs) throws SQLException {
        Checkin c = new Checkin();
        c.setId(rs.getInt("id"));
        c.setStudentId(rs.getInt("student_id"));
        c.setStudentNo(rs.getInt("student_no"));
        c.setStudentName(rs.getString("student_name"));
        c.setAction(rs.getString("action"));
        c.setBuildingName(rs.getString("building_name"));
        c.setRoomNo(rs.getString("room_no"));
        int bedNo = rs.getInt("bed_no");
        c.setBedNo(rs.wasNull() ? null : bedNo);
        c.setOperator(rs.getString("operator"));
        c.setCreateTime(rs.getTimestamp("create_time"));
        return c;
    }
}
```

- [ ] **Step 3: 写 `dao/StatDao.java`**

```java
package net.wanhe.dorm.dao;

import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

/*
 * 跨表聚合查询专用 Dao
 * 单表的增删改查留在各自的 Dao 里, 不在这里重复
 */
public interface StatDao {

    /*
     * 楼栋占用概览: 房间数/可分配床位数/已住人数
     */
    List<Building> selectBuildingOverview();

    /*
     * 还有空床位的楼栋(办理入住时先列这个)
     */
    List<Building> selectBuildingsWithFreeBed();

    /*
     * 某房间的在住学生名单
     */
    List<Student> selectStudentsByRoom(int roomId);
}
```

- [ ] **Step 4: 写 `dao/impl/StatDaoImpl.java`**

```java
package net.wanhe.dorm.dao.impl;

import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StatDaoImpl implements StatDao {

    //楼栋统计列: 房间数 / 可分配床位数(床位正常且房间正常) / 已住人数
    private static final String BUILDING_COLS = "b.id, b.name, b.sex, b.floors, b.remark,"
            + " (select count(*) from t_room r where r.building_id = b.id) as room_count,"
            + " (select count(*) from t_bed bd join t_room r on r.id = bd.room_id"
            + "     where r.building_id = b.id and bd.status = '正常' and r.status = '正常') as bed_count,"
            + " (select count(*) from t_student s join t_bed bd on bd.id = s.bed_id"
            + "     join t_room r on r.id = bd.room_id"
            + "     where r.building_id = b.id and bd.status = '正常' and r.status = '正常') as occupied_count";

    @Override
    public List<Building> selectBuildingOverview() {
        return selectBuildings("select " + BUILDING_COLS + " from t_building b order by b.id");
    }

    @Override
    public List<Building> selectBuildingsWithFreeBed() {
        return selectBuildings("select " + BUILDING_COLS + " from t_building b"
                + " having bed_count > occupied_count order by b.id");
    }

    @Override
    public List<Student> selectStudentsByRoom(int roomId) {
        List<Student> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement("select s.id, s.no, s.name, s.sex, s.age, s.phone, s.bed_id,"
                    + " bl.name as building_name, r.room_no, bd.bed_no"
                    + " from t_student s"
                    + " join t_bed bd on bd.id = s.bed_id"
                    + " join t_room r on r.id = bd.room_id"
                    + " join t_building bl on bl.id = r.building_id"
                    + " where bd.room_id = ? order by s.no");
            state.setInt(1, roomId);
            rs = state.executeQuery();
            while (rs.next()) {
                list.add(mapStudent(rs));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StatDao.selectStudentsByRoom失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 两个楼栋统计查询只差 SQL, 收在一个私有方法里
     */
    private List<Building> selectBuildings(String sql) {
        List<Building> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            state = conn.prepareStatement(sql);
            rs = state.executeQuery();
            while (rs.next()) {
                Building b = new Building();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setSex(rs.getString("sex"));
                b.setFloors(rs.getInt("floors"));
                b.setRemark(rs.getString("remark"));
                b.setRoomCount(rs.getInt("room_count"));
                b.setBedCount(rs.getInt("bed_count"));
                b.setOccupiedCount(rs.getInt("occupied_count"));
                list.add(b);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("StatDao.selectBuildings失败", e);
        } finally {
            JdbcUtil.close(rs, state, conn);
        }
    }

    /*
     * 结果集 -> 学生对象(带住宿位置)
     */
    private Student mapStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setNo(rs.getInt("no"));
        s.setName(rs.getString("name"));
        s.setSex(rs.getString("sex"));
        int age = rs.getInt("age");
        s.setAge(rs.wasNull() ? null : age);
        s.setPhone(rs.getString("phone"));
        int bedId = rs.getInt("bed_id");
        s.setBedId(rs.wasNull() ? null : bedId);
        s.setBuildingName(rs.getString("building_name"));
        s.setRoomNo(rs.getString("room_no"));
        int bedNo = rs.getInt("bed_no");
        s.setBedNo(rs.wasNull() ? null : bedNo);
        return s;
    }
}
```

- [ ] **Step 5: 写 `service/StayService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StayService {

    /*
     * 还有空床位的楼栋
     */
    List<Building> buildingsWithFreeBed();

    /*
     * 某楼栋的可分配床位(床位正常+房间正常+无人住)
     */
    List<Bed> freeBeds(int buildingId);

    /*
     * 查学生住宿信息, 学生不存在时抛异常
     */
    Student stayInfo(int studentNo) throws StayException;

    /*
     * 入住前校验: 学生不存在或已入住时抛异常, 否则返回学生
     * 先拦一道, 避免让用户白选一轮楼栋与床位
     */
    Student checkInTarget(int studentNo) throws StayException;

    /*
     * 办理入住: 全部校验通过后, 同事务内 改bed_id + 写入住流水
     */
    void checkIn(int studentNo, int bedId, String operator) throws StayException;

    /*
     * 办理退住: 同事务内 清bed_id + 写退住流水(记录退住前的位置)
     */
    void checkOut(int studentNo, String operator) throws StayException;
}
```

- [ ] **Step 6: 写 `service/impl/StayServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.dao.BuildingDao;
import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Room;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StayService;
import net.wanhe.dorm.util.JdbcUtil;

import java.util.List;

public class StayServiceImpl implements StayService {

    private StuDao stuDao;
    private BedDao bedDao;
    private RoomDao roomDao;
    private BuildingDao buildingDao;
    private CheckinDao checkinDao;
    private StatDao statDao;

    public StayServiceImpl() {
        stuDao = (StuDao) newDao("net.wanhe.dorm.dao.impl.StuDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dorm.dao.impl.BedDaoImpl");
        roomDao = (RoomDao) newDao("net.wanhe.dorm.dao.impl.RoomDaoImpl");
        buildingDao = (BuildingDao) newDao("net.wanhe.dorm.dao.impl.BuildingDaoImpl");
        checkinDao = (CheckinDao) newDao("net.wanhe.dorm.dao.impl.CheckinDaoImpl");
        statDao = (StatDao) newDao("net.wanhe.dorm.dao.impl.StatDaoImpl");
    }

    /*
     * 反射创建Dao, 服务层与Dao实现类解耦
     */
    private static Object newDao(String className) {
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建" + className + "失败", e);
        }
    }

    @Override
    public List<Building> buildingsWithFreeBed() {
        return statDao.selectBuildingsWithFreeBed();
    }

    @Override
    public List<Bed> freeBeds(int buildingId) {
        return bedDao.selectFreeBeds(buildingId);
    }

    @Override
    public Student stayInfo(int studentNo) throws StayException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StayException("该学号的学生不存在");
        }
        return stu;
    }

    @Override
    public Student checkInTarget(int studentNo) throws StayException {
        Student stu = stayInfo(studentNo);
        if (stu.isCheckedIn()) {
            throw new StayException("该学生已入住" + stu.location() + ", 请先办理退住");
        }
        return stu;
    }

    @Override
    public void checkIn(int studentNo, int bedId, String operator) throws StayException {
        //1.学生: 存在且未入住
        Student stu = checkInTarget(studentNo);
        //2.床位: 存在且正常
        Bed bed = bedDao.selectById(bedId);
        if (bed == null) {
            throw new StayException("床位不存在");
        }
        if (!"正常".equals(bed.getStatus())) {
            throw new StayException("该床位已停用, 不能分配");
        }
        //3.房间: 存在、正常、未住满
        Room room = roomDao.selectById(bed.getRoomId());
        if (room == null) {
            throw new StayException("床位所属房间不存在");
        }
        if (!"正常".equals(room.getStatus())) {
            throw new StayException("该房间已停用, 不能分配");
        }
        if (roomDao.countOccupied(room.getId()) >= room.getCapacity()) {
            throw new StayException("该房间已住满");
        }
        //4.床位没被别人占
        if (stuDao.selectByBedId(bedId) != null) {
            throw new StayException("该床位已被占用");
        }
        //5.楼栋存在且性别相符
        Building building = buildingDao.selectById(room.getBuildingId());
        if (building == null) {
            throw new StayException("房间所属楼栋不存在");
        }
        if (!building.getSex().equals(stu.getSex())) {
            throw new StayException("性别与楼栋不符, 不能入住");
        }
        //6.同事务: 改床位 + 写入住流水
        try {
            JdbcUtil.beginTransaction();
            stuDao.updateBedId(stu.getId(), bedId);
            Checkin c = new Checkin();
            c.setStudentId(stu.getId());
            c.setStudentNo(stu.getNo());
            c.setStudentName(stu.getName());
            c.setAction("入住");
            c.setBuildingName(building.getName());
            c.setRoomNo(room.getRoomNo());
            c.setBedNo(bed.getBedNo());
            c.setOperator(operator);
            checkinDao.insert(c);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new StayException("办理入住失败, 已回滚", e);
        }
    }

    @Override
    public void checkOut(int studentNo, String operator) throws StayException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StayException("该学号的学生不存在");
        }
        if (!stu.isCheckedIn()) {
            throw new StayException("该学生当前未入住, 无需退住");
        }
        //流水要记录退住前的位置, 便于追溯退的是哪张床
        Bed bed = bedDao.selectById(stu.getBedId());
        try {
            JdbcUtil.beginTransaction();
            stuDao.updateBedId(stu.getId(), null);
            Checkin c = new Checkin();
            c.setStudentId(stu.getId());
            c.setStudentNo(stu.getNo());
            c.setStudentName(stu.getName());
            c.setAction("退住");
            if (bed != null) {
                c.setBuildingName(bed.getBuildingName());
                c.setRoomNo(bed.getRoomNo());
                c.setBedNo(bed.getBedNo());
            }
            c.setOperator(operator);
            checkinDao.insert(c);
            JdbcUtil.commit();
        } catch (RuntimeException e) {
            JdbcUtil.rollbackQuietly();
            throw new StayException("办理退住失败, 已回滚", e);
        }
    }
}
```

- [ ] **Step 7: 写 `controller/StayController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.StayException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StayService;
import net.wanhe.dorm.service.impl.StayServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.LoginContext;
import net.wanhe.dorm.util.ScannerUtil;

import java.util.List;

public class StayController {

    private StayService stayService = new StayServiceImpl();

    public int print() {
        System.out.println("--入住退住办理--");
        System.out.println("1.办理入住");
        System.out.println("2.办理退住");
        System.out.println("3.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 办理入住: 学号 -> 有空床的楼栋 -> 该楼栋空床位 -> 选床位
     */
    public void checkIn() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student stu = stayService.checkInTarget(studentNo);
            System.out.println("学生:" + stu.getNo() + " " + stu.getName() + " " + stu.getSex());
            List<Building> buildings = stayService.buildingsWithFreeBed();
            if (buildings.isEmpty()) {
                System.out.println("暂无有空床的楼栋");
                return;
            }
            int[] w = {8, 12, 6, 8};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "空床数"}, w);
            AlignUtil.printLine(w);
            for (Building b : buildings) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(b.getId()),
                        b.getName(),
                        b.getSex(),
                        String.valueOf(b.getFreeCount())}, w);
            }
            AlignUtil.printLine(w);
            int buildingId = ScannerUtil.nextInt("请输入楼栋id:");
            List<Bed> beds = stayService.freeBeds(buildingId);
            if (beds.isEmpty()) {
                System.out.println("该楼栋暂无空床位");
                return;
            }
            int[] bw = {8, 10, 6};
            AlignUtil.printLine(bw);
            AlignUtil.printRow(new String[]{"床位id", "房间号", "床号"}, bw);
            AlignUtil.printLine(bw);
            for (Bed b : beds) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(b.getId()),
                        b.getRoomNo(),
                        String.valueOf(b.getBedNo())}, bw);
            }
            AlignUtil.printLine(bw);
            int bedId = ScannerUtil.nextInt("请输入床位id:");
            stayService.checkIn(studentNo, bedId, LoginContext.getCurrentUser());
            System.out.println("办理入住成功");
        } catch (StayException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 办理退住
     */
    public void checkOut() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student stu = stayService.stayInfo(studentNo);
            System.out.println("学生:" + stu.getNo() + " " + stu.getName() + " 当前:" + stu.location());
            stayService.checkOut(studentNo, LoginContext.getCurrentUser());
            System.out.println("办理退住成功");
        } catch (StayException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

- [ ] **Step 8: 写 `system/StaySystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.StayController;

/*
 * 入住退住办理菜单
 */
public class StaySystem {

    private StayController stayController = new StayController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = stayController.print();
            switch (c) {
                case 1:
                    stayController.checkIn();
                    break;
                case 2:
                    stayController.checkOut();
                    break;
                case 3:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
```

- [ ] **Step 9: 在 `Run.java` 里接上入住退住菜单**

加 import：

```java
import net.wanhe.dorm.system.StaySystem;
```

把 `case 4:` 里的 `System.out.println("该功能尚未实现");` 换成：

```java
                    new StaySystem().run();
```

- [ ] **Step 10: 编译**

```powershell
.\build.bat
```

Expected: `build ok`

- [ ] **Step 11: 写脚本 `test-inputs/stay.txt`**

```
admin
123456
4
1
2025001
1
1
1
2025001
1
2025004
1
2
1
2025003
1
1
1
2025004
2
13
2
2025005
2
2025001
3
6
```

逐行含义（此时：1号楼 男 101/102/103、2号楼 女 201/202/203，每间 4 床，床位 id 依次 1~12 与 13~24，5 名学生全部未入住）：

| 行 | 输入 | 期望 |
|---|---|---|
| 3 | `4` | 进入入住退住办理 |
| 4-7 | `1` `2025001` `1` `1` | 办理入住：张三(男) → 1号楼 → 床位 id 1 → `办理入住成功` |
| 8-9 | `1` `2025001` | 再次给同一学生办理入住 → `该学生已入住1号楼101房1床, 请先办理退住`（这道校验在选楼栋之前就拦下了，所以只消耗学号这一行输入） |
| 10-13 | `1` `2025004` `1` `2` | 赵敏(女) → 选男楼 1号楼 → 床位 id 2 → `性别与楼栋不符, 不能入住` |
| 14-17 | `1` `2025003` `1` `1` | 王五(男) → 1号楼 → 床位 id 1（已被张三占用）→ `该床位已被占用` |
| 18-21 | `1` `2025004` `2` `13` | 赵敏(女) → 2号楼 → 床位 id 13 → `办理入住成功` |
| 22-23 | `2` `2025005` | 办理退住：周芷未入住 → `该学生当前未入住, 无需退住` |
| 24-25 | `2` `2025001` | 办理退住：张三 → `办理退住成功` |
| 26-27 | `3` `6` | 返回上一级 → 退出系统 |

- [ ] **Step 12: 跑脚本并检查**

```powershell
cmd /c "run.bat < test-inputs\stay.txt > out\stay.log 2>&1"
Get-Content out\stay.log -Encoding UTF8
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "办理入住成功" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "该学生已入住1号楼101房1床, 请先办理退住" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "性别与楼栋不符, 不能入住" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "该床位已被占用" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "该学生当前未入住, 无需退住" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "办理退住成功" -SimpleMatch
Get-Content out\stay.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
```

Expected: `办理入住成功` 命中 2 次，中间 4 条各命中 1 次，`办理退住成功` 命中 1 次，`Exception` 无命中。

- [ ] **Step 13: SQL 复核状态与流水**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select no,name,sex,bed_id from t_student order by no; select id,student_no,student_name,action,building_name,room_no,bed_no,operator from t_checkin order by id;"
```

Expected:

- `t_student`：只有 `赵敏` 的 `bed_id = 13`，其余 4 人为 `NULL`（张三入住后又退住，最终为空）
- `t_checkin`：恰好 **3 行**
  1. `2025001 张三 入住 1号楼 101 1 admin`
  2. `2025004 赵敏 入住 2号楼 201 1 admin`
  3. `2025001 张三 退住 1号楼 101 1 admin`（退住流水的坐标是**退住前**的位置）
- 三条失败的操作用例**没有**产生任何流水行

- [ ] **Step 14: 复核「一床一人」没被破坏**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select bed_id, count(*) as c from t_student where bed_id is not null group by bed_id having c > 1;"
```

Expected: 空结果集（没有任何床位被两个学生同时占用）。

- [ ] **Step 15: 提交**

```powershell
git add -A
git commit -m "feat(stay): 入住退住办理(严格校验+事务+流水)"
```

---

## Task 10: 查询统计模块

**Files:**
- Create: `service/StatService.java`、`service/impl/StatServiceImpl.java`、`controller/StatController.java`、`system/StatSystem.java`
- Modify: `Run.java`（case 5）
- Create: `test-inputs/stat.txt`

DAO 层已在 Task 9 建好，本任务只做 service/controller 与表格展示。

- [ ] **Step 1: 写 `service/StatService.java`**

```java
package net.wanhe.dorm.service;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Student;

import java.util.List;

public interface StatService {

    /*
     * 楼栋占用概览
     */
    List<Building> buildingOverview();

    /*
     * 某房间在住学生名单, 房间不存在时抛异常
     */
    List<Student> roomRoster(int roomId) throws RoomException;

    /*
     * 空床位清单, buildingId 为 null 表示全部楼栋
     */
    List<Bed> freeBeds(Integer buildingId);

    /*
     * 学生住宿信息, 学生不存在时抛异常
     */
    Student studentStay(int studentNo) throws StuException;

    /*
     * 入住退住流水, studentNo 为 null 表示全部学生
     */
    List<Checkin> checkinHistory(Integer studentNo);
}
```

- [ ] **Step 2: 写 `service/impl/StatServiceImpl.java`**

```java
package net.wanhe.dorm.service.impl;

import net.wanhe.dorm.dao.BedDao;
import net.wanhe.dorm.dao.CheckinDao;
import net.wanhe.dorm.dao.RoomDao;
import net.wanhe.dorm.dao.StatDao;
import net.wanhe.dorm.dao.StuDao;
import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StatService;

import java.util.List;

public class StatServiceImpl implements StatService {

    private StatDao statDao;
    private RoomDao roomDao;
    private StuDao stuDao;
    private BedDao bedDao;
    private CheckinDao checkinDao;

    public StatServiceImpl() {
        statDao = (StatDao) newDao("net.wanhe.dorm.dao.impl.StatDaoImpl");
        roomDao = (RoomDao) newDao("net.wanhe.dorm.dao.impl.RoomDaoImpl");
        stuDao = (StuDao) newDao("net.wanhe.dorm.dao.impl.StuDaoImpl");
        bedDao = (BedDao) newDao("net.wanhe.dorm.dao.impl.BedDaoImpl");
        checkinDao = (CheckinDao) newDao("net.wanhe.dorm.dao.impl.CheckinDaoImpl");
    }

    private static Object newDao(String className) {
        try {
            Class c = Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("通过反射创建" + className + "失败", e);
        }
    }

    @Override
    public List<Building> buildingOverview() {
        return statDao.selectBuildingOverview();
    }

    @Override
    public List<Student> roomRoster(int roomId) throws RoomException {
        if (roomDao.selectById(roomId) == null) {
            throw new RoomException("房间不存在");
        }
        return statDao.selectStudentsByRoom(roomId);
    }

    @Override
    public List<Bed> freeBeds(Integer buildingId) {
        //与入住办理共用同一份空床位查询, 不重复实现
        return bedDao.selectFreeBeds(buildingId);
    }

    @Override
    public Student studentStay(int studentNo) throws StuException {
        Student stu = stuDao.selectByNo(studentNo);
        if (stu == null) {
            throw new StuException("该学号的学生不存在");
        }
        return stu;
    }

    @Override
    public List<Checkin> checkinHistory(Integer studentNo) {
        if (studentNo == null) {
            return checkinDao.selectAll();
        }
        return checkinDao.selectByStudentNo(studentNo);
    }
}
```

- [ ] **Step 3: 写 `controller/StatController.java`**

```java
package net.wanhe.dorm.controller;

import net.wanhe.dorm.exception.RoomException;
import net.wanhe.dorm.exception.StuException;
import net.wanhe.dorm.pojo.Bed;
import net.wanhe.dorm.pojo.Building;
import net.wanhe.dorm.pojo.Checkin;
import net.wanhe.dorm.pojo.Student;
import net.wanhe.dorm.service.StatService;
import net.wanhe.dorm.service.impl.StatServiceImpl;
import net.wanhe.dorm.util.AlignUtil;
import net.wanhe.dorm.util.ScannerUtil;

import java.text.SimpleDateFormat;
import java.util.List;

public class StatController {

    private StatService statService = new StatServiceImpl();

    private static final SimpleDateFormat TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public int print() {
        System.out.println("--查询统计--");
        System.out.println("1.楼栋占用概览");
        System.out.println("2.房间住宿名单");
        System.out.println("3.空床位清单");
        System.out.println("4.学生住宿信息");
        System.out.println("5.入住退住流水");
        System.out.println("6.返回上一级");
        return ScannerUtil.nextInt("请选择:");
    }

    /*
     * 楼栋占用概览
     */
    public void overview() {
        List<Building> list = statService.buildingOverview();
        if (list.isEmpty()) {
            System.out.println("暂无楼栋数据");
            return;
        }
        int[] w = {8, 12, 6, 8, 8, 6, 6, 8};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"楼栋id", "楼栋名称", "类型", "房间数", "总床位", "已住", "空床", "占用率"}, w);
        AlignUtil.printLine(w);
        for (Building b : list) {
            String rate = b.getBedCount() == 0 ? "0%" : (b.getOccupiedCount() * 100 / b.getBedCount()) + "%";
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    b.getName(),
                    b.getSex(),
                    String.valueOf(b.getRoomCount()),
                    String.valueOf(b.getBedCount()),
                    String.valueOf(b.getOccupiedCount()),
                    String.valueOf(b.getFreeCount()),
                    rate}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 房间住宿名单
     */
    public void roster() {
        int roomId = ScannerUtil.nextInt("请输入房间id:");
        try {
            List<Student> list = statService.roomRoster(roomId);
            if (list.isEmpty()) {
                System.out.println("该房间暂无在住学生");
                return;
            }
            int[] w = {10, 10, 6, 16};
            AlignUtil.printLine(w);
            AlignUtil.printRow(new String[]{"学号", "姓名", "性别", "电话"}, w);
            AlignUtil.printLine(w);
            for (Student s : list) {
                AlignUtil.printRow(new String[]{
                        String.valueOf(s.getNo()),
                        s.getName(),
                        s.getSex(),
                        s.getPhone() == null ? "" : s.getPhone()}, w);
            }
            AlignUtil.printLine(w);
        } catch (RoomException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 空床位清单(0 表示全部楼栋)
     */
    public void freeBeds() {
        int buildingId = ScannerUtil.nextInt("请输入楼栋id(0表示全部楼栋):", 0, 999999999);
        Integer filter = buildingId == 0 ? null : buildingId;
        List<Bed> list = statService.freeBeds(filter);
        if (list.isEmpty()) {
            System.out.println("暂无空床位");
            return;
        }
        System.out.println("空床位数:" + list.size());
        int[] w = {8, 12, 10, 6};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"床位id", "楼栋", "房间号", "床号"}, w);
        AlignUtil.printLine(w);
        for (Bed b : list) {
            AlignUtil.printRow(new String[]{
                    String.valueOf(b.getId()),
                    b.getBuildingName(),
                    b.getRoomNo(),
                    String.valueOf(b.getBedNo())}, w);
        }
        AlignUtil.printLine(w);
    }

    /*
     * 学生住宿信息
     */
    public void studentStay() {
        int studentNo = ScannerUtil.nextInt("请输入学号:", 1, 999999999);
        try {
            Student s = statService.studentStay(studentNo);
            System.out.println("学号:" + s.getNo());
            System.out.println("姓名:" + s.getName());
            System.out.println("性别:" + s.getSex());
            System.out.println("电话:" + (s.getPhone() == null ? "" : s.getPhone()));
            System.out.println("住宿位置:" + s.location());
        } catch (StuException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * 入住退住流水(0 表示全部学生)
     */
    public void checkinHistory() {
        int studentNo = ScannerUtil.nextInt("请输入学号(0表示全部学生):", 0, 999999999);
        Integer filter = studentNo == 0 ? null : studentNo;
        List<Checkin> list = statService.checkinHistory(filter);
        if (list.isEmpty()) {
            System.out.println("暂无流水记录");
            return;
        }
        System.out.println("流水条数:" + list.size());
        int[] w = {21, 10, 10, 6, 18, 10};
        AlignUtil.printLine(w);
        AlignUtil.printRow(new String[]{"时间", "学号", "姓名", "操作", "位置", "操作人"}, w);
        AlignUtil.printLine(w);
        for (Checkin c : list) {
            AlignUtil.printRow(new String[]{
                    c.getCreateTime() == null ? "" : TIME.format(c.getCreateTime()),
                    String.valueOf(c.getStudentNo()),
                    c.getStudentName(),
                    c.getAction(),
                    c.location(),
                    c.getOperator()}, w);
        }
        AlignUtil.printLine(w);
    }
}
```

- [ ] **Step 4: 写 `system/StatSystem.java`**

```java
package net.wanhe.dorm.system;

import net.wanhe.dorm.controller.StatController;

/*
 * 查询统计菜单
 */
public class StatSystem {

    private StatController statController = new StatController();

    public void run() {
        boolean f = true;
        while (f) {
            int c = statController.print();
            switch (c) {
                case 1:
                    statController.overview();
                    break;
                case 2:
                    statController.roster();
                    break;
                case 3:
                    statController.freeBeds();
                    break;
                case 4:
                    statController.studentStay();
                    break;
                case 5:
                    statController.checkinHistory();
                    break;
                case 6:
                    f = false;
                    break;
                default:
                    System.out.println("请选择正确的选项");
            }
        }
    }
}
```

- [ ] **Step 5: 在 `Run.java` 里接上查询统计菜单**

加 import：

```java
import net.wanhe.dorm.system.StatSystem;
```

把 `case 5:` 里的 `System.out.println("该功能尚未实现");` 换成：

```java
                    new StatSystem().run();
```

- [ ] **Step 6: 编译（此时五个「该功能尚未实现」应全部被替换掉）**

```powershell
.\build.bat
Get-Content src\net\wanhe\dorm\Run.java -Encoding UTF8 | Select-String -Pattern "该功能尚未实现" -SimpleMatch
```

Expected: `build ok`；第二条命令**无输出**（占位文案已全部替换）。

- [ ] **Step 7: 写脚本 `test-inputs/stat.txt`**

```
admin
123456
5
1
2
4
3
0
4
2025004
5
0
6
6
```

逐行含义：登录 → 主菜单5(查询统计) → 1(楼栋占用概览) → 2(房间住宿名单) → 房间id=4(201房) → 3(空床位清单) → 楼栋id=0(全部) → 4(学生住宿信息) → 学号2025004 → 5(流水) → 学号0(全部) → 6(返回) → 6(退出)。

- [ ] **Step 8: 跑脚本并检查**

```powershell
cmd /c "run.bat < test-inputs\stat.txt > out\stat.log 2>&1"
Get-Content out\stat.log -Encoding UTF8
Get-Content out\stat.log -Encoding UTF8 | Select-String -Pattern "空床位数:23" -SimpleMatch
Get-Content out\stat.log -Encoding UTF8 | Select-String -Pattern "流水条数:3" -SimpleMatch
Get-Content out\stat.log -Encoding UTF8 | Select-String -Pattern "住宿位置:2号楼201房1床" -SimpleMatch
Get-Content out\stat.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
```

Expected（接着 Task 9 的状态：赵敏在 2号楼201房1床，其他 4 人未入住，流水 3 条）：

- 概览：`1号楼` 房间数 3、总床位 12、已住 0、空床 12、占用率 0%；`2号楼` 房间数 3、总床位 12、已住 1、空床 11、占用率 8%
- 房间住宿名单（房间 id 4 = 201）：赵敏一行
- `空床位数:23`、`流水条数:3`、`住宿位置:2号楼201房1床` 各命中 1 次
- `Exception` 无命中

- [ ] **Step 9: SQL 交叉复核统计口径**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select (select count(*) from t_bed) - (select count(*) from t_student where bed_id is not null) as free_beds; select count(*) as checkin_rows from t_checkin;"
```

Expected: `free_beds = 23`、`checkin_rows = 3`，与程序打印的 23 和 3 完全一致（说明统计口径与真实数据一致，不是硬编码）。

- [ ] **Step 10: 提交**

```powershell
git add -A
git commit -m "feat(stat): 查询统计模块(概览/名单/空床/住宿信息/流水)"
```

---

## Task 11: 全流程验收脚本与交付检查

**Files:**
- Rewrite: `test_input.txt`（覆盖规格 §12 要求的全流程与全部反例）
- Create: `docs/superpowers/plans/` 下无需新增；验收证据写在对话/提交说明里

- [ ] **Step 1: 重置数据库（保证脚本可反复执行）**

```powershell
$env:MYSQL_PWD="123456"
cmd /c "mysql --host=localhost --user=root --default-character-set=utf8mb4 < db\schema.sql"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select (select count(*) from t_bed) as beds, (select count(*) from t_student) as students, (select count(*) from t_checkin) as checkins;"
```

Expected: `beds=24 students=5 checkins=0`（回到初始状态）。

- [ ] **Step 2: 写 `test_input.txt`**

内容（**不能加注释行**，程序会把每一行当输入读走）：

```
admin
wrong
admin
123456
abc
1
1
2
3号楼
男
6
测试楼栋
2
1号楼
男
6
重名测试
2
4号楼
女
x
6
待删除
3
4
4号楼
女
7
改过的楼
4
9999
4
4
4
1
5
2
3
1
104
4
3
1
104
5
1
1
4
7
5
4
7
2
7
26
2
2
7
8
3
2
2025006
孙悟空
男
20
13900000006
2
2025007
猪八戒
男
21
13900000007
2
2025006
重复
男
20
13900000008
3
2025007
猪悟能
男
22
13900000009
1
5
4
1
2025001
1
1
1
2025002
1
2
1
2025003
1
3
1
2025006
1
4
1
2025007
1
1
1
2025007
1
26
1
2025004
1
5
1
2025004
2
13
1
2025001
2
2025005
2
2025003
3
2
4
1
2
4
1
3
4
1
5
5
1
5
7
8
3
4
2025001
5
4
2
2025001
3
5
1
2
1
3
0
4
2025004
5
0
6
6
```

- [ ] **Step 3: 分阶段核对期望消息**

| 阶段 | 行范围（大致） | 关键期望消息 |
|---|---|---|
| A 登录 | `admin wrong admin 123456 abc 1` | `用户名或密码错误` → `请重新登录` → `登录成功` → `请输入数字`（字母当数字被拦下重试） |
| B 楼栋 | `1 …5` | `新增成功`、`该楼栋名称已存在`、`请输入数字`、`修改成功`、`该楼栋不存在`、`删除成功`、`该楼栋下还有3个房间, 请先删除房间` |
| C 房间床位 | `2 …8` | `新增成功, 已自动生成4个床位`、`该楼栋下已存在房间:104`、两次 `调整成功`、`操作成功` |
| D 学生 | `3 …5` | 两次 `新增成功`、`该学号已存在`、`修改成功` |
| E 入住退住 | `4 …3` | 4 个 `办理入住成功`（张三/李四/王五/孙悟空住满 101）、`该房间已住满`、`该床位已停用, 不能分配`、`性别与楼栋不符, 不能入住`、第 5 个 `办理入住成功`（赵敏）、`该学生已入住1号楼101房1床, 请先办理退住`、`该学生当前未入住, 无需退住`、`办理退住成功`（王五） |
| F 容量与删除保护 | `2 …8` | `新床位数不能少于已住人数3`、`床号大于3的床位上有1名在住学生, 请先办理退住`、`调整成功`、`房间内还有3名在住学生, 不能删除`、`删除成功`（删掉空房间 104） |
| G 在住学生删除保护 + 退住 | `3 …3` | `该学生正在1号楼101房1床, 请先办理退住`、`办理退住成功`（张三） |
| H 查询统计 | `5 …6 6` | `空床位数:22`、`流水条数:7`、`住宿位置:2号楼201房1床` |

- [ ] **Step 4: 跑完整验收**

```powershell
cmd /c "run.bat < test_input.txt > out\acceptance.log 2>&1"
Get-Content out\acceptance.log -Encoding UTF8
Get-Content out\acceptance.log -Encoding UTF8 | Select-String -Pattern "Exception" -SimpleMatch
Get-Content out\acceptance.log -Encoding UTF8 | Select-String -Pattern "该功能尚未实现" -SimpleMatch
Get-Content out\acceptance.log -Encoding UTF8 | Select-String -Pattern "build failed" -SimpleMatch
```

Expected: 第三次命令（`Exception`）**无命中**（程序没有崩溃、没有异常堆栈）；后两条也无命中。

- [ ] **Step 5: 逐条确认 10 个反例都出现**

```powershell
$log = "out\acceptance.log"
"用户名或密码错误","请输入数字","该楼栋名称已存在","该楼栋不存在","该楼栋下还有3个房间, 请先删除房间","该楼栋下已存在房间:104","该学号已存在","该学生已入住1号楼101房1床, 请先办理退住","该学生当前未入住, 无需退住","该房间已住满","该床位已停用, 不能分配","性别与楼栋不符, 不能入住","新床位数不能少于已住人数3","床号大于3的床位上有1名在住学生, 请先办理退住","房间内还有3名在住学生, 不能删除","该学生正在1号楼101房1床, 请先办理退住" | ForEach-Object {
    $hit = (Get-Content $log -Encoding UTF8 | Select-String -Pattern $_ -SimpleMatch).Count
    "$_  =>  $hit"
}
```

Expected: 每一行都 `=> 1`（或更多），没有任何一行是 `=> 0`。

- [ ] **Step 6: SQL 复核终态**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select (select count(*) from t_bed) as beds, (select sum(capacity) from t_room) as cap_sum; select count(*) as checkins, sum(action='入住') as in_cnt, sum(action='退住') as out_cnt from t_checkin; select count(*) as occupied from t_student where bed_id is not null;"
```

Expected（终态）：

- `beds = cap_sum = 25` —— 房间 101 扩容到 5 后总床位 25，与各房间 capacity 之和严格相等（**床位数不变量成立**）
- `checkins = 7`、`in_cnt = 5`、`out_cnt = 2`
- `occupied = 3`（李四、孙悟空、赵敏）

- [ ] **Step 7: SQL 复核「状态与流水一致」**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select s.no, s.name, s.bed_id, (select c.action from t_checkin c where c.student_no = s.no order by c.id desc limit 1) as last_action from t_student s order by s.no;"
```

Expected:

| 学号 | 姓名 | bed_id | last_action |
|---|---|---|---|
| 2025001 | 张三 | NULL | 退住 |
| 2025002 | 李四 | 2 | 入住 |
| 2025003 | 王五 | NULL | 退住 |
| 2025004 | 赵敏 | 13 | 入住 |
| 2025005 | 周芷 | NULL | NULL（无流水） |
| 2025006 | 孙悟空 | 4 | 入住 |
| 2025007 | 猪悟能 | NULL | NULL（无流水） |

规律：**`bed_id` 非空 ⟺ 最后一条流水是「入住」**。

- [ ] **Step 8: SQL 复核「一床一人」**

```powershell
$env:MYSQL_PWD="123456"
mysql --host=localhost --user=root --default-character-set=utf8mb4 --database=rg01 --execute="select bed_id, count(*) as c from t_student where bed_id is not null group by bed_id having c > 1; select count(*) as ghost_beds from t_bed bd left join t_room r on r.id = bd.room_id where r.id is null;"
```

Expected: 两条都返回空结果集 —— 没有床位被两名学生同时占用，也没有「找不到所属房间」的孤儿床位。

- [ ] **Step 9: 交付检查**

```powershell
.\build.bat
git status --short
Get-ChildItem -Recurse -Filter *.class src | Measure-Object | Select-Object -ExpandProperty Count
```

Expected: `build ok`；`git status --short` 只剩未跟踪的 `out/`、`test-inputs/`（若已提交则干净）；`src` 下 `.class` 数量为 **0**（编译产物只出现在 `net/`，且已被 `.gitignore` 忽略）。

- [ ] **Step 10: 提交**

```powershell
git add -A
git commit -m "test: 全流程验收脚本(含10个反例)与数据一致性复核"
```

- [ ] **Step 11: 交回用户确认遗留项**

向用户报告并询问两件事（**不要自己决定**）：

1. `data-backup-0908/`（旧 Java 序列化数据快照）现在还有没有用？确认后可删除。
2. `edusystem.iml` 里声明的 JDK 1.8 与本机 JDK 21 不符，是否要顺手改成 21 或直接删掉这个 IDEA 模块文件。

---

## 计划自查结果

**规格覆盖检查**（逐条对规格 §2 范围与 §6 业务规则）：

| 规格要求 | 落在哪个任务 |
|---|---|
| 楼栋增删改查 | Task 6 |
| 有房间的楼栋禁止删除 | Task 7 Step 10 |
| 房间增删查 + 自动生成床位 | Task 7 |
| 容量调整（扩容补床 / 缩容删空床 / 被在住床位挡住） | Task 7 |
| 房间与床位停用/启用（有在住学生禁止停用） | Task 7 |
| 删除房间连带删床位、有人在住禁止删 | Task 7 |
| 学生增删改查（学号主键不可改） | Task 8 |
| 在住学生禁止删除 | Task 8 |
| 入住 8 条校验 + 事务 + 流水 | Task 9 |
| 退住 + 流水记录退住前位置 | Task 9 |
| 查询统计 5 项 | Task 10 |
| 6 张表 + 示例数据 + 建库脚本 | Task 2 |
| 事务支持（`ThreadLocal`） | Task 3（`JdbcUtil`）+ Task 9（使用） |
| 输入健壮性（非数字不崩） | Task 3（`ScannerUtil`）+ Task 8/11 的反例 |
| 表格中文对齐 | Task 3（`AlignUtil`）+ 各 Controller |
| 旧包/遗留文件清理 | Task 3 |
| 验收标准 12.1~12.5 | Task 11 |

**占位符扫描**：计划中无 `TBD`/`TODO`/「类似 Task N」；每个改代码的步骤都给了完整代码或精确的替换片段与位置。

**类型与命名一致性检查**（跨任务核对过）：

- `Bed.isOccupied()`、`Student.isCheckedIn()`、`Student.location()`、`Bed.location()`、`Room.getFreeCount()`、`Building.getFreeCount()` 在 Task 4 定义，Task 6~10 使用，签名一致。
- DAO 方法名在「接口定义 → 实现 → service 调用」三处一致：`countOccupied`、`countOccupiedAbove`、`deleteFreeBedsAbove`、`selectByBedId`、`updateBedId`、`updateCapacity`、`selectFreeBeds(Integer)`、`selectBuildingsWithFreeBed`。
- `JdbcUtil` 对外只有 `getConnection` / `close` / `beginTransaction` / `commit` / `rollbackQuietly`，Task 7/9 的事务模板与之匹配。
- 菜单编号与 `switch` 分支、输入脚本顺序三处对齐（Task 7 房间子菜单 8 项、Task 3 主菜单 6 项）。

**已识别的实现风险**（执行时若命中，按这里的处置方式办）：

| 风险 | 处置 |
|---|---|
| `mysql` 客户端不接受 `cmd /c "mysql ... < file"` 的写法 | 改用 `Get-Content db\schema.sql -Raw` 配合 `--execute`，或用 Navicat/Workbench 手工执行一次 |
| 5.7 上 `delete bd from t_bed bd left join ...` 报语法错 | 改为 `delete from t_bed where room_id=? and bed_no>? and id not in (select bed_id from t_student where bed_id is not null)` |
| 管道运行时中文仍乱码 | 确认 `run.bat` 已带 `-Dstdin/-Dstdout/-Dstderr.encoding=UTF-8`；仍乱码则把 `select`/`insert` 的中文改成 ASCII 断言（SQL 侧复核不受影响） |
| `Scanner` 读到 EOF（脚本行数不够） | 日志里会看到 `NoSuchElementException`；对照 Task 11 的阶段表补足缺失的输入行 |


