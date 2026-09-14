# 构建与运行说明（本机环境适配）

本文记录 `build.bat` / `run.bat` / `db/schema.sql` 为适配**本机实际环境**所做的修改，
以及三处曾经导致「编译不过 / 程序连不上库」的真实原因。

## 一、本机实际环境（实测）

| 项 | 实测结果 | 说明 |
|---|---|---|
| JDK | `1.8.0_191`（`C:\Program Files\Java\jdk1.8.0_191`） | 源码是 Java 8 语法，编译通过 |
| MySQL 服务端 | `5.5.28`（端口 3306，服务名 `MySQL`，`C:\Program Files\MySQL\MySQL Server 5.5`） | 不是 5.7，也不是 8.0 |
| MySQL 账号 | `root` / `123456` | 与 `JdbcUtil.java` 中硬编码一致 |
| 数据库 | `dorm_system` | 改造前**并不存在**，需要先执行 `db/schema.sql` |
| 驱动 | `lib/mysql-connector-java-8.0.30.jar` | 8.0.30 可正常连接 5.5.28（已实测） |
| 工程路径 | `C:\Users\惠普\Desktop\dorm-system` | 含中文，这正是构建脚本原先失败的原因 |

> 原 README 写的是「JDK 18+ / MySQL 5.7」，与本机不符，已按实测环境修正。

## 二、改造前的三个真实故障及修法

### 1. `build.bat` 报「找不到文件: Run.java」——两条独立原因叠加

**原因 A：输出目录 `net\` 不存在。**
`javac -d net ...` 在输出目录不存在时，报的却是 `javac: 找不到目录: net` 或
`找不到文件: <某个源码>`，与真实原因不符，极易误导。
修法：编译前先 `rmdir /s /q net` 再 `md net`。

**原因 B：`dir /s /b > filelist.txt` 与 `javac @argfile` 编码不一致（关键）。**
- `dir` 重定向写出的 `filelist.txt` 使用**当前控制台编码**，在本机（代码页 936）
  实测写出的是 **UTF-8** 字节；
- 而 `javac` 读取 `@argfile` 时使用**平台默认编码 GBK**；
- 于是路径里的 `惠普` 被读成乱码 `鎯犳櫘`，`javac` 回答「找不到文件」。

实测证据（`filelist.txt` 第 1 行原始字节）：

```
… 92, 230,131,160, 230,153,174, 92 …      <- UTF-8 的「惠普」
按 GBK 读 => C:\Users\鎯犘\Desktop\…      <- javac 实际看到的路径
```

修法：不再用 `dir` 落盘，改用 `for /f` 收集 `powershell` 输出的**相对路径**
（`src\...`，纯 ASCII），命令行过长时 cmd 会自动分段调用 `javac`：

```bat
for /f "usebackq delims=" %%f in (`powershell -NoProfile -Command "...relative paths..."`) do call set "SOURCES=%%SOURCES%% %%f"
javac -encoding UTF-8 -cp "lib\*" -d net %SOURCES%
```

> 若工程被移动到纯英文路径（如 `D:\dorm-system`），旧写法其实也能编译成功——
> 这也说明故障与中文用户名强相关。

### 2. `db/schema.sql` 在 MySQL 5.5 上失败：`ERROR 1067 Invalid default value`

脚本第 65 行 `t_checkin.create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`
需要 MySQL **5.6.5+**；5.5.28 直接拒绝，导致 `t_checkin` 建表失败（其余表会建成，
于是出现"半套表"的中间状态）。修法：该列改为
`TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`（5.5 支持），Java 侧
`rs.getTimestamp()` 读取行为不变。

同时把 4 段示例数据改为**幂等**写法（`WHERE NOT EXISTS` / 由房间推导床位），
脚本重复执行不再因主键或唯一键冲突而中断。

### 3. 中文输入输出：JDK 8 不认 `-Dstdin/-Dstdout.encoding`

原 `run.bat` 依赖 JDK 18+ 才有的 `-Dstdin.encoding` / `-Dstdout.encoding` /
`-Dstderr.encoding`。本机 JDK 8 **静默忽略**这些参数，真正生效的是
`-Dfile.encoding=UTF-8`（Java 8 的 stdin 解码与 stdout 编码都由它决定）。

实测对照（同一份 UTF-8 输入）：

| 配置 | 结果 |
|---|---|
| `chcp 65001` + `-Dfile.encoding=UTF-8` | 中文菜单正常、登录成功（**正确**） |
| `chcp 65001`（无任何编码参数） | 中文失效，登录读不到正确账号 |
| `chcp 936` + `-Dfile.encoding=UTF-8` | 中文菜单正常、登录成功 |
| `chcp 65001` + `-Dfile.encoding=GBK` | 中文失效 |

因此 `run.bat` 保留 `chcp 65001` 并**必须**带 `-Dfile.encoding=UTF-8`；
另外三个参数在 JDK 18+ 上仍有意义，故一并保留（JDK 8 下无害）。

## 三、使用步骤

```bat
rem 1. 初始化数据库（可重复执行）
mysql -h 127.0.0.1 -uroot -p123456 --default-character-set=utf8mb4 < db\schema.sql

rem 2. 编译（成功输出 build ok）
build.bat

rem 3. 运行（管理员 admin / 123456）
run.bat
```

初始数据：1号楼(男)/2号楼(女)，各 3 间房、每间 4 床，5 名学生（初始均未入住）。

## 四、验收记录（本次改造实测）

环境：JDK 1.8.0_191 + MySQL 5.5.28，工程位于含中文的路径下。

**1) 从零重建：删库 → 建库 → 编译 → 运行**

- `drop database dorm_system` 后执行 `db/schema.sql`：`exit 0`，6 张表齐全，
  `t_checkin.create_time` 为 `timestamp`；再执行一次仍 `exit 0`（幂等，无重复键报错）。
- `build.bat`：`build ok`，产物 **54 个 class = 54 个 java 源文件**；连续执行两次结果一致。

**2) 功能路径（用脚本喂输入逐条实测）**

| 路径 | 结果 |
|---|---|
| 登录 `admin` / `123456` | `登录成功` |
| 学生管理 → 新增学生（含中文姓名） | `新增成功`，`t_student` 落库姓名无乱码 |
| 学生管理 → 查看学生 | 新学生出现在表格中，住宿位置显示 `未入住` |
| 入住办理（2025001 → 1号楼 → 床位1） | `办理入住成功, 住宿位置:1号楼101房1床`；`t_student.bed_id=1`，`t_checkin` 写入 1 条 `入住` 流水 |
| 流水 `create_time` | 由表默认值正确写入（`2026-09-12 14:54:12`） |
| 查询统计（概览/名单/空床位/住宿信息/流水） | 均正常输出表格 |
| 同学生重复入住、学号重复等反例 | 被正确拦截（`该学号已存在` 等提示，不崩） |
| 退出系统 | `谢谢使用` |

验收用的测试数据已清理，数据库已恢复为初始 5 名学生、`t_checkin` 空表。

**3) 已验证的失败→修复对照**

改造前：`build.bat` 报 `找不到文件 ...Run.java`；`schema.sql` 报
`ERROR 1067 Invalid default value for 'create_time'`；`dorm_system` 库根本不存在。
改造后上述三项均通过（原因与修法见第二节）。

## 4.1 已知限制：把脚本管道喂给 `run.bat` 时

`run.bat` 内部会 `call build.bat`，而 `build.bat` 需要启动一次 `powershell`
（已加 `^<nul` 防止它吞掉 stdin）。在**交互式控制台**里这样完全没问题，就是正常用法。

但如果用「管道重定向」把一整个输入脚本喂给 `run.bat`（例如
`cmd /c "run.bat < test_input.txt"`），cmd 会额外启动一层批处理解释器，
这一层会先消费掉管道里的 stdin，导致程序读到 EOF 并抛
`NoSuchElementException`。**这不是程序缺陷**，实测对照：

| 调用方式 | stdin 是否完整到达程序 |
|---|---|
| `java ... net.wanhe.dormsystem.Run < 输入.txt`（与 `run.bat` 同一命令行参数） | 完整 |
| `build.bat < 输入.txt` + `java ... < 输入.txt`（同一 cmd 内两条命令） | 完整 |
| `run.bat < 输入.txt`（多包一层批处理） | 在管道 stdin 下会被外层吞掉 |

结论与建议：
- **人工使用 `run.bat` 交互操作不受影响**；
- 若要用脚本做自动化验收，请用
  `build.bat < 输入.txt > out\acceptance.log 2>&1` 后再单独跑
  `java -Dfile.encoding=UTF-8 -cp ".;net;lib\*" net.wanhe.dormsystem.Run < 输入.txt`
  （即把 `run.bat` 的两步拆开执行）。

## 五、注意事项

- `build.bat` / `run.bat` **保持纯 ASCII**：`.bat` 里的中文注释需要与控制台代码页
  一致，且 UTF-8 注释字节紧邻 `>` 时会被 cmd 当作续字节吃掉
  （`dir > filelist.txt` 的 `>` 会被误吞，直接导致语法错误），故中文说明只放在本文档。
- 数据库连接信息仍硬编码在 `src/net/wanhe/dormsystem/util/JdbcUtil.java`
  （本机实测可用，未改动）。换机器只需改该文件或同步建库。
- 仓库中 `README.md`、`docs/`、`test-inputs/`、`test_input.txt` 在工作区处于
  **已删除但未提交**状态；这些文件描述的是旧环境（JDK 21 + MySQL 5.7），
  若仍需保留请告诉我，我可以恢复并改写为当前环境版本。

## 六、为支持 Web 版，控制台版新增的依赖（重要）

Web 版要复用已有的 DAO + Service，而这两层通过 `util/JdbcUtil.java` 取连接。为了让**同一份代码**
在控制台模式（无 Spring 容器）和 Web 模式（Spring 管理事务）下都能工作，`JdbcUtil` 改为：

- 未注入 `DataSource` 时：用原有的硬编码连接信息自建 `DriverManagerDataSource`，
  事务仍由 `beginTransaction/commit/rollbackQuietly` 手动控制 → **控制台行为与改造前一致**；
- 被 Spring 注入 `DataSource` 时：连接走 `DataSourceUtils`，自动加入 Spring 声明式事务。

因此 `JdbcUtil` 现在依赖 Spring 的 5 个 jar，控制台版运行时也需要它们在 `lib\` 下：

```
spring-jdbc-5.3.31.jar    spring-tx-5.3.31.jar     spring-core-5.3.31.jar
spring-beans-5.3.31.jar   spring-jcl-5.3.31.jar
```

这些 jar **不入版本库**（体积大）。新克隆仓库后执行一次：

```bat
fetch-lib.bat
```

它会从本地 Maven 仓库（`%USERPROFILE%\.m2\repository`）把这 5 个 jar 水合到 `lib\`，
之后 `build.bat` / `run.bat` 照常使用（`lib\*` 已在 classpath 上）。若本地仓库里也没有，
先跑一次 `build-web.bat` 让 Maven 下载，再执行 `fetch-lib.bat`。

改动后的实测结果（本次已验证）：`build.bat` 输出 `build ok`，54 个 class = 54 个源文件；
控制台版登录、中文新增学生、办理入住（`t_student.bed_id` 更新 + `t_checkin` 流水写入、
`create_time` 由默认值填充）全部正常。Web 版说明见 `WEB.md`。
