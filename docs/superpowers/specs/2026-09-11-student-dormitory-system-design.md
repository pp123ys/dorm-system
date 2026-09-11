# 学生宿舍管理系统 —— 设计规格

- 日期：2026-09-11
- 目标项目：`D:\text\edusystem`（现有控制台「教育系统」，改造为「学生宿舍管理系统」）
- 状态：已经需求澄清与设计确认，待实现

## 1. 背景与目标

现有项目是一个 Java 控制台「教育系统」：登录后进入「班级管理 / 学生管理」两个模块，采用
`System`（菜单循环）→ `Controller`（交互展示）→ `Service`（业务）→ `Dao`（JDBC）的分层结构，
数据存于 MySQL。

本次改造把业务域从「班级—学生」换成「楼栋—房间—床位—学生入住」，交付一个可运行、可验收的
**学生宿舍管理系统**控制台程序。改造要保留现有分层风格和构建方式，让代码结构依然一眼可读。

## 2. 范围

### 2.1 做（核心版）

| 模块 | 内容 |
|---|---|
| 楼栋管理 | 楼栋的增、删、改、查 |
| 房间床位管理 | 房间的增、删、查；按容量自动生成床位；床位停用/启用；调整房间容量 |
| 学生管理 | 学生的增、删、改、查 |
| 入住退住办理 | 办理入住（分配床位）、办理退住；两者都写流水 |
| 查询统计 | 楼栋占用概览、房间住宿名单、空床位清单、学生住宿信息、入住退住流水 |
| 登录 | 单一管理员账号登录（沿用现有 `t_user` 机制） |

### 2.2 不做（明确排除）

- 宿管员/学生等多角色与权限分级：只有单一管理员
- 换宿/调宿独立菜单：换宿通过「退住 + 入住」两步完成，各自留一条流水
- 违纪扣分、宿舍检查、报修、值日：核心版之外，不在本次范围
- Web 界面：保持控制台程序
- 数据导出、报表打印、批量导入学生

## 3. 运行环境事实（本机实测）

这些是实测结论，不是假设，实现必须以此为准：

| 项 | 实测结果 | 对设计的影响 |
|---|---|---|
| JDK | `javac 21.0.2` / `java 21.0.2`（`.iml` 里写的 1.8 与实际不符） | 构建实际用 JDK 21；源码**只用 Java 8 语法**，保证 JDK 8/21 都能编译 |
| MySQL 服务端 | `5.7.34`（端口 3306 可连） | **不能用 MySQL 8 专属语法**（如递归 CTE、窗口函数）；示例数据床位用显式 `INSERT ... VALUES` |
| MySQL 客户端 | `mysql.exe` 可用（`D:\mysql-5.7.34-winx64\...\bin`） | 验收可用 SQL 直接复核数据 |
| 账号 | `root` / `123456` 连接成功 | 现有 `JdbcUtil` 里的连接信息无需改动 |
| `rg01` 库 | **不存在**（现有库列表：`exam`、`filemanager`、`health_check_system`、`mingli_test`、`personality_test`、`test01`、`wanju_mall` 等，无 `rg01`） | `schema.sql` 必须自己 `CREATE DATABASE`；没有存量数据需要保留 |
| 磁盘编码 | 控制台全链路 UTF-8（JVM `-D*.encoding=UTF-8` 参数；`.bat` 不带 `chcp`——批文件 stdin 被重定向时 `chcp` 会重置后续命令的 stdin，见实现计划 Task 3 说明） | 输出中文与表格对齐需自行按显示宽度处理 |

## 4. 架构与包结构

沿用现有四层结构，包名由 `net.wanhe.edusystem` 迁移为 `net.wanhe.dorm`。

```
src/net/wanhe/dorm/
├── Run.java                      入口：登录 → 主菜单循环
├── system/                       菜单循环层（只做菜单编号分发）
│   ├── UserSystem.java           登录流程（沿用原有结构）
│   ├── BuildingSystem.java
│   ├── RoomSystem.java
│   ├── StuSystem.java
│   ├── StaySystem.java
│   └── StatSystem.java
├── controller/                   交互层（读输入、打表格、捕获异常并提示）
│   ├── UserController.java
│   ├── BuildingController.java
│   ├── RoomController.java
│   ├── StuController.java
│   ├── StayController.java
│   └── StatController.java
├── service/                      业务接口（所有校验规则的定义）
│   ├── UserService.java
│   ├── BuildingService.java
│   ├── RoomService.java
│   ├── StuService.java
│   ├── StayService.java
│   └── StatService.java
├── service/impl/                 业务实现（校验规则落地，事务边界所在）
│   └── ...ServiceImpl.java（同上六个）
├── dao/                          数据访问接口
│   ├── UserDao.java
│   ├── BuildingDao.java
│   ├── RoomDao.java
│   ├── BedDao.java
│   ├── StuDao.java
│   ├── CheckinDao.java
│   └── StatDao.java
├── dao/impl/                     数据访问实现（纯 JDBC + PreparedStatement）
│   └── ...DaoImpl.java（同上七个）
├── pojo/
│   ├── User.java  Building.java  Room.java
│   └── Bed.java   Student.java   Checkin.java
├── exception/
│   ├── UserException.java     BuildingException.java  RoomException.java
│   └── StuException.java      StayException.java
└── util/
    ├── JdbcUtil.java             连接与事务（在现有基础上扩展）
    ├── ScannerUtil.java          统一控制台输入
    ├── AlignUtil.java            按显示宽度对齐（中文算 2 宽）
    └── LoginContext.java         保存当前登录管理员名（流水 operator 的来源）
```

### 4.1 依赖方向

`Run` → `system` → `controller` → `service` → `dao` → `JdbcUtil`，**单向依赖，不反向**。

- `controller` 不含业务判断，只做「取值 → 调 service → 打印结果或提示」
- `service` 是唯一放校验规则的地方，失败即抛对应 `XxxException`
- `dao` 不含业务规则，只做 SQL 执行与结果集映射

### 4.2 职责边界要点

- **`BedDao` 独立存在**：床位虽是房间的子实体（建房时按 `capacity` 批量生成），但入住时要独立锁定/更新某张床，混进 `RoomDao` 会让两边都变模糊。
- **`StatDao` 只放跨表聚合查询**：楼栋占用概览、空床位清单、学生住宿信息、流水查询。单表 CRUD 留在各自 Dao。每个查询**归属唯一 DAO**，不允许两处都写。

## 5. 数据库设计

- 库：`rg01`（由 `schema.sql` 创建），字符集 `utf8mb4`，引擎 `InnoDB`
- 风格沿用现有项目：自增主键、**不加外键约束**、关联由业务层保证
- 共 6 张表：沿用 1 张（`t_user`）+ 新增 5 张

### 5.1 表清单

| # | 表名 | 职责 | 关键约束 |
|---|---|---|---|
| 1 | `t_user` | 管理员登录（沿用） | `login_name` 唯一 |
| 2 | `t_building` | 楼栋 | `name` 唯一 |
| 3 | `t_room` | 房间 | `(building_id, room_no)` 唯一 |
| 4 | `t_bed` | 床位 | `(room_id, bed_no)` 唯一 |
| 5 | `t_student` | 学生 + 入住关系 | `no` 唯一；`bed_id` 唯一且可空 |
| 6 | `t_checkin` | 入住/退住流水（只增不改） | 无 |

### 5.2 DDL

```sql
CREATE DATABASE IF NOT EXISTS rg01 DEFAULT CHARSET utf8mb4;
USE rg01;

-- 1. 管理员表（沿用原有结构）
CREATE TABLE IF NOT EXISTS t_user (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  login_name VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(50) NOT NULL COMMENT '密码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '管理员表';

-- 旧结构清理（本机 rg01 不存在，此处仅保证脚本可重复执行）
DROP TABLE IF EXISTS t_clazz;
DROP TABLE IF EXISTS t_student;
DROP TABLE IF EXISTS t_bed;
DROP TABLE IF EXISTS t_room;
DROP TABLE IF EXISTS t_building;
DROP TABLE IF EXISTS t_checkin;

-- 2. 楼栋表
CREATE TABLE t_building (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  name VARCHAR(50) NOT NULL UNIQUE COMMENT '楼栋名称',
  sex VARCHAR(4) NOT NULL COMMENT '楼栋类型: 男/女',
  floors INT NOT NULL DEFAULT 6 COMMENT '楼层数',
  remark VARCHAR(100) COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '楼栋表';

-- 3. 房间表
CREATE TABLE t_room (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  building_id INT NOT NULL COMMENT '所属楼栋id',
  room_no VARCHAR(20) NOT NULL COMMENT '房间号',
  capacity INT NOT NULL COMMENT '床位数',
  status VARCHAR(10) NOT NULL DEFAULT '正常' COMMENT '状态: 正常/停用',
  UNIQUE KEY uk_room (building_id, room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '房间表';

-- 4. 床位表（不含 student_id, 占用关系存在 t_student.bed_id）
CREATE TABLE t_bed (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  room_id INT NOT NULL COMMENT '所属房间id',
  bed_no INT NOT NULL COMMENT '床位号, 从1开始',
  status VARCHAR(10) NOT NULL DEFAULT '正常' COMMENT '状态: 正常/停用',
  UNIQUE KEY uk_bed (room_id, bed_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '床位表';

-- 5. 学生表（bed_id 为 NULL 表示未入住）
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

-- 6. 入住退住流水表（位置与学生信息冗余存储, 保证历史可读）
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
```

### 5.3 两个关键设计取舍

1. **入住关系只存在 `t_student.bed_id`，`t_bed` 里不放 `student_id`。**
   - 「一个学生只能占一个床位」由 `uk_student_bed` 唯一索引 + 单列结构直接保证，无需额外校验。
   - 「一个床位只能住一个学生」由同一唯一索引保证（MySQL 唯一索引允许多个 NULL，所以未入住学生不冲突）。
   - 入住 = `update t_student set bed_id=?`；退住 = `set bed_id=null`；占用情况用 `t_bed left join t_student` 查询。
2. **流水表冗余存 `student_no`/`student_name`/`building_name`/`room_no`/`bed_no`。**
   - 日后学生或房间被删除，历史记录仍然完整可读，不会因为 join 不到而失真。
   - 流水表**只增不改不删**，没有任何更新/删除入口。

### 5.4 示例数据

`schema.sql` 末尾插入以下初始数据，保证「开箱即可演示」：

- 管理员：`admin` / `123456`
- 楼栋：`1号楼`(男, 6层)、`2号楼`(女, 6层)
- 房间：1号楼 `101`/`102`/`103`，2号楼 `201`/`202`/`203`，容量均为 4
- 床位：上述 6 间房各 4 张（`bed_no` 1~4），**用显式 `INSERT ... VALUES` 逐条列出**（MySQL 5.7 无递归 CTE，不能用生成式写法）
- 学生 5 名：`2025001 张三 男 18`、`2025002 李四 男 19`、`2025003 王五 男 20`、`2025004 赵敏 女 19`、`2025005 周芷 女 18`
- **所有示例学生 `bed_id` 均为 NULL（初始未入住），流水表初始为空** —— 与「未入住」状态严格一致，避免初始数据自相矛盾

> 运行时新建房间时，由程序按 `capacity` 自动生成床位，不依赖脚本生成。

## 6. 业务规则

### 6.1 楼栋

| 操作 | 规则 |
|---|---|
| 新增 | `name` 非空且不重复；`sex` 只能是 `男` 或 `女`；`floors` ≥ 1 |
| 修改 | 同上；改名需保证不与他人重复 |
| 删除 | 该楼栋下**存在任何房间**即拒绝，提示「请先删除该楼栋下的房间」 |

### 6.2 房间与床位

| 操作 | 规则 |
|---|---|
| 新增 | 所属楼栋必须存在；`room_no` 非空且在同楼栋内不重复；`capacity` 范围 1~20；成功后**自动生成 1~capacity 号床位**（状态「正常」） |
| 调整容量 | 新容量 **≥ 当前已住人数**，否则拒绝；调大自动补生成床位（从当前最大床号+1 起）；调小前先检查 `bed_no > 新容量` 的床位是否有人住，**有人住则拒绝**（提示先办理退住），无人住则删除这些空床位。由此恒有「房间床位数 = capacity」这一不变量 |
| 删除 | 房间内**有在住学生**即拒绝；否则**连带删除该房间全部床位**及其空床位记录 |
| 停用/启用床位 | 床位**有在住学生**时禁止停用；停用后该床位不参与入住分配 |
| 房间停用 | 房间停用时，若房内有在住学生则拒绝 |

### 6.3 学生

| 操作 | 规则 |
|---|---|
| 新增 | `no` 非空且不重复；`name` 非空；`sex` 只能是 `男` 或 `女`；`age` 若填则 10~100 |
| 修改 | **学号是业务主键，不可修改**，只改姓名/性别/年龄/电话（校验同上）；`bed_id` 不可通过学生修改功能直接改（只能走入住/退住），避免绕过校验 |
| 删除 | 该学生**在住中**即拒绝，提示「该学生正在 X楼Y房Z床，请先办理退住」 |

### 6.4 入住办理（`StayService.checkIn(studentNo, bedId, operator)`）

按顺序校验，任一步失败抛 `StayException`，**不做任何写库**：

| 序号 | 校验 | 失败提示 |
|---|---|---|
| 1 | 学号对应的学生存在 | 该学号的学生不存在 |
| 2 | 学生当前 `bed_id` 为 NULL | 该学生已入住 X楼Y房Z床，请先办理退住 |
| 3 | 目标床位存在 | 床位不存在 |
| 4 | 床位 `status` = 正常 | 该床位已停用，不能分配 |
| 5 | 床位所属房间 `status` = 正常 | 该房间已停用，不能分配 |
| 6 | 床位所属房间已住人数 < `capacity` | 该房间已住满 |
| 7 | 床位当前无人占用（`t_student` 中无该 `bed_id`） | 该床位已被占用 |
| 8 | **学生性别 == 楼栋 `sex`** | 性别与楼栋不符，不能入住 |

通过后，**同一事务内**：`update t_student set bed_id=?` + `insert t_checkin(入住)`。

### 6.5 退住办理（`StayService.checkOut(studentNo, operator)`）

| 序号 | 校验 | 失败提示 |
|---|---|---|
| 1 | 学生存在 | 该学号的学生不存在 |
| 2 | 学生当前 `bed_id` 非空 | 该学生当前未入住，无需退住 |

通过后，**同一事务内**：`update t_student set bed_id=null` + `insert t_checkin(退住)`（流水写入**退住前的**楼栋/房间/床位，便于追溯退的是哪张床）。

### 6.6 换宿

不设独立菜单与独立流水类型。教师用「退住 → 入住」两步完成，产生一条「退住」+ 一条「入住」流水。

## 7. 事务与错误处理

### 7.1 事务支持（`JdbcUtil` 扩展）

现状：`JdbcUtil` 只有 `getConnection()` 与 `close(rs, state, conn)`，每次调用新建连接，无法保证「改 `bed_id` + 写流水」的原子性。

方案：`JdbcUtil` 内用 `ThreadLocal<Connection>` 持有当前连接，新增三个方法：

```java
public static void beginTransaction() throws SQLException   // 取连接 + setAutoCommit(false)
public static void commit()          // 提交并归还连接
public static void rollback()        // 回滚并归还连接
```

- 事务开启后，`JdbcUtil.getConnection()` 返回的是**同一个连接**，因此**各 Dao 的方法签名完全不用改**。
- `close(rs, state, conn)` 在事务中**不关闭连接**，只关 `ResultSet`/`PreparedStatement`，由 `commit`/`rollback` 统一归还。
- 本程序是单线程控制台，`ThreadLocal` 无并发风险。
- `service/impl/StayServiceImpl` 的两个写方法用 `try / catch / rollback / finally` 包裹。

### 7.2 错误处理

- Service 校验失败：抛 `BuildingException` / `RoomException` / `StuException` / `StayException` / `UserException`，消息为可直接给用户看的中文。
- DAO 层 SQL 异常：包装为 `RuntimeException("XxxDao.方法名失败", e)`（沿用现有风格），保留原始异常链。
- Controller：`catch (RuntimeException e)` → 打印 `e.getMessage()` → 返回子菜单。**任何输入错误都不允许让程序崩溃退出。**
- 输入层：`ScannerUtil` 统一处理非数字输入（重试而不是抛 `InputMismatchException`）。

## 8. 菜单与交互

### 8.1 主菜单（`Run`）

```
--学生宿舍管理系统--
1. 楼栋管理
2. 房间床位管理
3. 学生管理
4. 入住退住办理
5. 查询统计
6. 退出系统
请选择:
```

登录失败（账号或密码错）提示后重新输入；登录成功后进入上表循环。每个子菜单最后一项固定为「返回」。

### 8.2 子菜单

| 菜单 | 子项 |
|---|---|
| 1 楼栋管理 | 1 查看楼栋 2 新增楼栋 3 修改楼栋 4 删除楼栋 5 返回 |
| 2 房间床位管理 | 1 按楼栋查看房间 2 查看某房间床位详情 3 新增房间 4 调整房间容量 5 删除房间 6 停用/启用房间 7 停用/启用床位 8 返回 |
| 3 学生管理 | 1 查看学生 2 新增学生 3 修改学生 4 删除学生 5 返回 |
| 4 入住退住办理 | 1 办理入住 2 办理退住 3 返回 |
| 5 查询统计 | 1 楼栋占用概览 2 房间住宿名单 3 空床位清单 4 学生住宿信息 5 入住退住流水 6 返回 |

### 8.3 交互细则

- **新增房间**只输入容量，床位由程序生成，不逐个录入。
- **办理入住**输入学号后：显示学生信息 → 列出有空床的楼栋（含空床数）→ 输入楼栋 id → 列出该楼栋空床位（显示 `床位id / 房间号 / 床号`）→ 输入床位 id 完成办理；成功后打印住宿位置。
- **查询统计**输出为对齐表格：
  - 楼栋占用概览：楼栋、类型、房间数、总床位、已住、空床、占用率
  - 空床位清单：楼栋、房间号、床位号
  - 学生住宿信息：学号、姓名、性别、楼栋、房间号、床位号（未入住显示「未入住」）

## 9. 类与接口清单

### 9.1 POJO

| 类 | 持久字段 | 展示辅助字段（不参与 insert/update） |
|---|---|---|
| `User` | id, loginName, password | — |
| `Building` | id, name, sex, floors, remark | `roomCount`, `bedCount`, `occupiedCount`（占用概览用） |
| `Room` | id, buildingId, roomNo, capacity, status | `buildingName`, `occupiedCount` |
| `Bed` | id, roomId, bedNo, status | `buildingName`, `roomNo`, `studentNo`, `studentName`（空床位/住宿展示用） |
| `Student` | id, no, name, sex, age, phone, bedId | `buildingName`, `roomNo`, `bedNo`（查住宿信息用） |
| `Checkin` | id, studentId, studentNo, studentName, action, buildingName, roomNo, bedNo, operator, createTime(`java.util.Date`) | — |

所有展示辅助字段在类的注释里明确标注「仅查询展示用」。

### 9.2 DAO 方法（每个查询归属唯一 DAO）

| DAO | 方法 |
|---|---|
| `UserDao` | `User selectByLoginName(String)` |
| `BuildingDao` | `List<Building> selectAll()`、`Building selectById(int)`、`Building selectByName(String)`、`int insert(Building)`、`int update(Building)`、`int delete(int)` |
| `RoomDao` | `List<Room> selectByBuildingId(int)`、`Room selectById(int)`、`Room selectByBuildingAndNo(int, String)`、`int insert(Room)`、`int update(Room)`、`int updateCapacity(int roomId, int capacity)`、`int delete(int)`、`int countByBuildingId(int)`、`int countOccupied(int roomId)` |
| `BedDao` | `List<Bed> selectByRoomId(int)`、`Bed selectById(int)`、`List<Bed> selectFreeBeds(Integer buildingId)`（`null` = 全部楼栋）、`int insert(Bed)`、`int delete(int)`、`int deleteByRoomId(int)`、`int deleteFreeBedsAbove(int roomId, int keepCount)`（删除 `bed_no > keepCount` 且无人住的床位）、`int countOccupiedAbove(int roomId, int keepCount)`、`int updateStatus(int, String)`。`selectByRoomId` / `selectById` 用 LEFT JOIN 填充 `buildingName`/`roomNo`/`studentNo`/`studentName` 展示字段 |
| `StuDao` | `List<Student> selectAll()`、`Student selectById(int)`、`Student selectByNo(int)`、`Student selectByBedId(int)`、`int insert(Student)`、`int update(Student)`、`int delete(int)`、`int updateBedId(int studentId, Integer bedId)`。三个查询方法都用 LEFT JOIN 填充 `buildingName`/`roomNo`/`bedNo` 展示字段 |
| `CheckinDao` | `int insert(Checkin)`、`List<Checkin> selectAll()`、`List<Checkin> selectByStudentNo(int)` |
| `StatDao` | `List<Building> selectBuildingOverview()`、`List<Building> selectBuildingsWithFreeBed()`、`List<Student> selectStudentsByRoom(int roomId)` |

### 9.3 Service 方法

| Service | 方法 |
|---|---|
| `UserService` | `User login(String loginName, String password)` |
| `BuildingService` | `List<Building> list()`、`Building get(int id)`、`void add(Building)`、`void update(Building)`、`void delete(int id)` |
| `RoomService` | `List<Room> listByBuilding(int buildingId)`、`List<Bed> listBeds(int roomId)`、`void add(Room)`、`void updateCapacity(int roomId, int newCapacity)`、`void delete(int roomId)`、`void updateRoomStatus(int roomId, String status)`、`void updateBedStatus(int bedId, String status)` |
| `StuService` | `List<Student> list()`、`void add(Student)`、`void update(Student)`、`void delete(int id)` |
| `StayService` | `List<Building> buildingsWithFreeBed()`、`List<Bed> freeBeds(int buildingId)`、`Student stayInfo(int studentNo)`、`Student checkInTarget(int studentNo)`（入住前校验：不存在或已入住即抛异常，避免让用户白选一轮床位）、`void checkIn(int studentNo, int bedId, String operator)`、`void checkOut(int studentNo, String operator)` |
| `StatService` | `List<Building> buildingOverview()`、`List<Student> roomRoster(int roomId)`、`List<Bed> freeBeds(Integer buildingId)`、`Student studentStay(int studentNo)`、`List<Checkin> checkinHistory(Integer studentNo)` |

**避免重复实现**：空床位清单在 `StayService.freeBeds` 与 `StatService.freeBeds` 中都调用同一个 `BedDao.selectFreeBeds(Integer)`；`StayService.buildingsWithFreeBed` 走 `StatDao.selectBuildingsWithFreeBed`；`StayService.stayInfo` 与 `StatService.studentStay` 都走 `StuDao.selectByNo(int)`（该方法已 LEFT JOIN 带出住宿位置，不再单独写一份）。

## 10. 工具类设计

### 10.1 `JdbcUtil`（在现有基础上扩展）

保留：连接常量（`jdbc:mysql://localhost:3306/rg01?...`、`root`/`123456`）、驱动注册、`getConnection()`、`close()`。
新增：`beginTransaction()` / `commit()` / `rollbackQuietly()`，内部 `ThreadLocal<Connection>` 持有事务连接；事务中 `close()` 不关连接。

- `beginTransaction()` 开头有嵌套防护：当前线程已持有事务连接时直接 `throw IllegalStateException`，防止旧连接被孤儿化后静默丢失数据（质量审查发现并修复）。
- `beginTransaction()` / `commit()` **不声明 `throws SQLException`**，内部把 SQL 异常包装成 `RuntimeException`，这样 Service 里的事务模板只需要处理业务异常，不必写受检的 `SQLException` 处理代码。
- `rollbackQuietly()` 吞掉回滚自身的异常并打印堆栈，避免回滚失败掩盖真正的业务异常。
- Service 统一使用这个事务模板：
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

### 10.2 `ScannerUtil`

现在是共享 `Scanner`（避免多 Scanner 抢占 `System.in`）。扩展为带校验的读取：

```java
public static int nextInt(String prompt)        // 非数字则提示并重试
public static int nextInt(String prompt, int min, int max)
public static String nextNonEmpty(String prompt) // 空串则提示并重试
public static String nextLine(String prompt)     // 允许空
public static boolean confirm(String prompt)     // y/n
```

### 10.3 `AlignUtil`

控制台表格对齐工具（中文占 2 个字符宽，直接 `String.format` 会错位）：

```java
public static int width(String s)              // 按显示宽度计算（CJK 算 2）
public static String padRight(String s, int w) // 右侧补齐到显示宽度 w
public static String padLeft(String s, int w)
```

## 11. 迁移与清理

| 动作 | 对象 |
|---|---|
| 删除 | `src/net/wanhe/edusystem/**`（含其中的 `.class` 与 `Clazz*` 全套） |
| 删除 | 根目录 `WriteInput.java`、`WriteInput2.java`、`WriteInput3.java` 及对应 3 个 `.class` |
| 删除 | `src/WriteInput*.class`（散落的编译产物） |
| 删除 | 历史日志 `run1.log`、`run2.log` |
| 保留（待用户确认后再删） | `data-backup-0908/`（旧 Java 序列化数据快照，改造后无用，但先留着做回退保险） |
| 更新 | `build.bat`：编译范围 `src\net\wanhe\dorm\*.java`，输出到 `net\` |
| 更新 | `run.bat`：主类改为 `net.wanhe.dorm.Run` |
| 重写 | `db/schema.sql`：建库 + 6 张表 + 示例数据 |
| 重写 | `test_input.txt`：覆盖全流程与反例的自动化输入脚本 |
| 不动 | `lib/mysql-connector-java-8.0.30.jar`、`edusystem.iml`（模块名与库引用保持可用） |

`.class` 编译产物不入库、不保留在 `src` 下。

## 12. 验收标准

每条都必须有实际执行证据，不接受「应该没问题」：

1. **构建**：`build.bat` 退出码 0，输出 `build ok`。
2. **全流程**：`run.bat < test_input.txt` 跑通「登录 → 新增楼栋 → 新增房间（自动生成床位）→ 新增学生 → 办理入住 → 查询统计 → 办理退住 → 查流水 → 退出」，无异常堆栈。
3. **反例**（每条都必须给出中文提示且程序不退出）：
   - 重复入住同一学生
   - 性别与楼栋不符（女生入住男生楼）
   - 房间住满后再入住
   - 停用床位后再分配
   - 删除有房间的楼栋
   - 删除有在住学生的房间
   - 删除在住学生
   - 容量调到小于已住人数
   - 输入字母当数字
   - 登录密码错误
4. **数据一致性（SQL 复核）**：用 `mysql` 客户端核对
   - `t_student.bed_id` 与 `t_checkin` 记录一一对应（每次入住/退住各一条流水，且流水中的位置与当时状态一致）
   - `t_bed` 总数 = 各房间 `capacity` 之和
   - 同一 `bed_id` 只被一个学生占用（唯一索引验证）
5. **回归可复跑**：`test_input.txt` 是覆盖上述场景的完整输入脚本。每次验收前**先执行 `db/schema.sql` 重置数据库**（脚本含 `DROP TABLE` 与示例数据插入），因此脚本可反复执行、结果可预期。

## 13. 已定取舍（决策记录）

| 决策 | 选择 | 理由 |
|---|---|---|
| 改造路径 | 原地改造 + 包名迁移到 `net.wanhe.dorm` | 单一代码路径，无重复代码，风格与现有习惯一致 |
| 系统形态 | 保持控制台 | 沿用现有分层与构建脚本，改动集中在业务模块 |
| 表数量 | 6 张（含 `t_user` 与流水表） | 床位是实体，占用查询直观；流水保证历史可追溯 |
| 入住关系位置 | `t_student.bed_id`（唯一索引） | 结构上保证一人一床、一床一人，无需额外校验 |
| 流水冗余字段 | 冗余存位置与姓名 | 房间/学生被删后历史记录依然可读 |
| 权限 | 单一管理员 | 核心版不需要多角色；`t_user` 可直接沿用 |
| 换宿 | 退住 + 入住两步 | 不引入新流水类型，减少状态机复杂度 |
| 事务实现 | `JdbcUtil` 内 `ThreadLocal<Connection>` | Dao 签名不用改，改动集中在工具类；单线程无并发风险 |
| 输入健壮性 | `ScannerUtil` 统一带校验读取 | 现有 `sc.nextInt()` 遇字母直接崩，必须先修掉 |
| 表格对齐 | 自建 `AlignUtil` | 中文占 2 宽，`String.format` 无法正确对齐 |
| 示例数据床位 | 显式 `INSERT ... VALUES` | 服务端是 MySQL 5.7，无递归 CTE 可用 |
| 容量调小的保护 | `bed_no > 新容量` 的床位有人住则拒绝调小 | 保住「房间床位数 = capacity」不变量，避免出现床号超出容量的鬼床 |
| 学号可改性 | 学号作为业务主键不可修改 | 避免「改学号」与「按学号定位」两套语义纠缠 |
| 流水操作人来源 | `util/LoginContext` 保存当前登录名 | 不必为传 operator 层层改构造器；单线程控制台无并发问题 |
| Dao 结果集映射 | 每个 DaoImpl 内写私有 `map(ResultSet)` 帮助方法 | 避免同一字段映射在多个查询里重复（DRY） |
| 控制台输出编码 | `run.bat` 增加 `-Dstdin/-Dstdout/-Dstderr.encoding=UTF-8` | JDK 18+ 起 `file.encoding` 不再决定标准流编码，否则管道重定向时中文乱码（现有 `test_input.txt` 已是乱码实例） |

## 14. 范围外但与本次相关的已知问题

- 现有 `JdbcUtil` 把账号密码硬编码在源码中——本次**不改**（属于既有风格，改动会偏离「贴近现有项目」的目标），但已知这是安全隐患。
- `edusystem.iml` 声明 JDK 1.8 与本机 JDK 21 不符——本次**不改** `.iml`，只用脚本构建。
- 现有库中还残留 `exam`、`filemanager` 等其他库，与本次改造无关，不动。
