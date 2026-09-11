# 学生宿舍管理系统

控制台版学生宿舍管理系统：Java 8 语法 + 原生 JDBC + MySQL，由原「教育系统」原地改造而来。

功能：管理员登录 → 楼栋管理 → 房间与床位管理（自动生成床位、扩容缩容、停用）→ 学生管理 →
入住退住办理（8 步严格校验 + 事务 + 流水）→ 查询统计（占用概览、住宿名单、空床位、住宿信息、流水）。

## 环境要求

| 项 | 要求 |
|---|---|
| JDK | **18+**（`run.bat` 使用 `-Dstdin/-Dstdout/-Dstderr.encoding`，本项目在本机 JDK 21.0.2 上验证） |
| MySQL | 5.7 及以上（在本机 5.7.34 上验证；脚本刻意不使用 MySQL 8 专属语法） |
| 驱动 | `lib/mysql-connector-java-8.0.30.jar`（已随项目提供，无需额外下载） |
| 编码 | 源码、脚本、输入文件统一 UTF-8；控制台输入输出统一 UTF-8 |

## 数据库

连接信息硬编码在 `src/net/wanhe/dorm/util/JdbcUtil.java`：`localhost:3306` / 库 `rg01` / `root` / `123456`。

建库建表 + 示例数据（**可重复执行**：会 drop 并重建 5 张宿舍表并重灌示例数据，`t_user` 用条件插入保留）：

```powershell
$env:MYSQL_PWD="123456"
cmd /c "mysql --host=localhost --user=root --default-character-set=utf8mb4 < db\schema.sql"
```

6 张表：`t_user`（管理员）、`t_building`（楼栋）、`t_room`（房间）、`t_bed`（床位）、
`t_student`（学生 + 入住关系 `bed_id`）、`t_checkin`（入住退住流水，只增不改）。

示例数据：管理员 `admin/123456`；1号楼（男）、2号楼（女）；每栋 3 间房、每间 4 床；5 名学生（初始均未入住）。

## 编译与运行

```powershell
.\build.bat     # 编译 src\net\wanhe\dorm 到 net\，成功输出 build ok
.\run.bat       # 交互式运行（默认管理员 admin / 123456）
```

## 验收（可重复执行）

```powershell
# 1) 重置数据库：每轮验收前都必须做，脚本里的 id 依赖示例数据的固定 id
$env:MYSQL_PWD="123456"
cmd /c "mysql --host=localhost --user=root --default-character-set=utf8mb4 < db\schema.sql"

# 2) 准备日志目录：out\ 不在版本库里，新克隆的仓库没有它
New-Item -ItemType Directory -Force out | Out-Null

# 3) 跑全流程验收（161 行输入，覆盖 8 个阶段与 16 个反例）
cmd /c "run.bat < test_input.txt > out\acceptance.log 2>&1"
Get-Content out\acceptance.log -Encoding UTF8
```

通过标准：日志中出现 `登录成功`、`谢谢使用`，且**不出现** `Exception`、`该功能尚未实现`、`build failed`。

逐条断言（16 个反例的关键字、SQL 终态核对）见
`docs/superpowers/plans/2026-09-11-student-dormitory-system.md` 的 Task 11。
按模块拆分的验收脚本见 `test-inputs/README.md`。

## 目录结构

```
build.bat / run.bat          编译与运行脚本
test_input.txt               全流程验收输入（161 行，8 阶段 + 16 反例）
test-inputs/                 按模块拆分的验收输入脚本（含 README 说明）
db/schema.sql                建库建表 + 示例数据（可重复执行）
lib/                         MySQL 驱动 jar
src/net/wanhe/dorm/          源码：Run + system/controller/service/dao/pojo/exception/util
docs/superpowers/            设计规格与实现计划
net/  out/                   编译产物与验收日志（均已 gitignore）
data-backup-0908/            改造前的旧序列化数据快照（保留在磁盘，未入库）
```

## 常见问题

| 现象 | 原因与处理 |
|---|---|
| `NoSuchElementException: No line found` | 输入脚本行数不够（每个提示都要对应一行）；或忘了重置数据库导致菜单走向与脚本不符 |
| 日志里中文乱码 | 读取时用 `Get-Content -Encoding UTF8`；日志开头的 javac 提示是 GBK 字节，属正常现象 |
| 交互运行时中文显示异常 | 先在自己的控制台执行 `chcp 65001`（脚本内**不再**内置 `chcp`：批文件 stdin 被重定向时 `chcp` 会把 stdin 重置回控制台，导致输入文件被丢弃） |
| 提示输入过长 | 列长度上限：楼栋名 ≤50、房间号 ≤20、姓名 ≤50、电话 ≤20、备注 ≤100（Service 层已校验并给出中文提示） |
| 想回到初始状态 | 重新执行 `db\schema.sql`（见「验收」第 1 步） |

## 文档

- 设计规格：`docs/superpowers/specs/2026-09-11-student-dormitory-system-design.md`
- 实现计划（逐任务代码、验收命令、决策记录）：`docs/superpowers/plans/2026-09-11-student-dormitory-system.md`
- 验收脚本说明：`test-inputs/README.md`

## 已知限制（设计阶段的有意取舍）

- 只有单一管理员账号，无角色/权限分级（未做宿管员、学生自助端）
- 换宿用「退住 + 入住」两步完成，没有独立换宿菜单
- 不含违纪扣分、宿舍检查、报修、值日等扩展模块
- 数据库账号密码硬编码在 `JdbcUtil.java`（教学项目取舍，便于零配置运行）
- `edusystem.iml` 里声明的是 JDK 1.8，与实际运行所需的 JDK 18+（编码参数）不符，尚未改动
