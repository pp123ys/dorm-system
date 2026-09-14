-- 寓安 · 学生宿舍管理系统 数据库脚本 (dorm_system)
-- 目标服务端: 本机 MySQL 5.5.28 (最低兼容 5.5.x), 不使用 5.5 不支持的语法
-- 注意: MySQL 5.5 的 DATETIME 不接受 CURRENT_TIMESTAMP 默认值(5.6.5 才支持),
--       因此 t_checkin.create_time 使用 TIMESTAMP, 详见该表注释
-- 用法: mysql -h 127.0.0.1 -uroot -p123456 --default-character-set=utf8mb4 < db\schema.sql
-- 特性: 可重复执行(idempotent), 重复导入不会因主键/唯一键冲突而报错

CREATE DATABASE IF NOT EXISTS dorm_system DEFAULT CHARSET utf8mb4;
USE dorm_system;
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
  -- 必须用 TIMESTAMP: MySQL 5.5 的 DATETIME 不支持 DEFAULT CURRENT_TIMESTAMP
  -- (该能力自 MySQL 5.6.5 才提供), 用 DATETIME 会直接 CREATE 失败(error 1067)
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '入住退住流水表';

-- ============ 初始化数据 ============
-- 管理员 admin / 123456 (条件插入: 保证脚本可重复执行, 已有 t_user 数据时不会主键冲突)
INSERT INTO t_user (login_name, password)
SELECT 'admin', '123456' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE login_name = 'admin');

-- 楼栋: 仅在表为空时写入默认数据(保证本脚本可重复执行)
INSERT INTO t_building (id, name, sex, floors, remark)
SELECT * FROM (
  SELECT 1 AS id, '1号楼' AS name, '男' AS sex, 6 AS floors, '男生宿舍' AS remark
  UNION ALL SELECT 2, '2号楼', '女', 6, '女生宿舍'
) d WHERE NOT EXISTS (SELECT 1 FROM t_building);

-- 房间
INSERT INTO t_room (id, building_id, room_no, capacity, status)
SELECT * FROM (
  SELECT 1 AS id, 1 AS building_id, '101' AS room_no, 4 AS capacity, '正常' AS status
  UNION ALL SELECT 2, 1, '102', 4, '正常'
  UNION ALL SELECT 3, 1, '103', 4, '正常'
  UNION ALL SELECT 4, 2, '201', 4, '正常'
  UNION ALL SELECT 5, 2, '202', 4, '正常'
  UNION ALL SELECT 6, 2, '203', 4, '正常'
) d WHERE NOT EXISTS (SELECT 1 FROM t_room);

-- 床位: 6 间房 x 4 床, 由房间推导, 避免手写 24 行
INSERT INTO t_bed (room_id, bed_no, status)
SELECT r.id, n.bed_no, '正常'
FROM t_room r
JOIN (
  SELECT 1 AS bed_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
) n
WHERE NOT EXISTS (SELECT 1 FROM t_bed);

-- 学生: 初始全部未入住(bed_id 为 NULL), 流水表初始为空, 两者严格一致
INSERT INTO t_student (no, name, sex, age, phone, bed_id)
SELECT * FROM (
  SELECT 2025001 AS no, '张三' AS name, '男' AS sex, 18 AS age, '13800000001' AS phone, NULL AS bed_id
  UNION ALL SELECT 2025002, '李四', '男', 19, '13800000002', NULL
  UNION ALL SELECT 2025003, '王五', '男', 20, '13800000003', NULL
  UNION ALL SELECT 2025004, '赵敏', '女', 19, '13800000004', NULL
  UNION ALL SELECT 2025005, '周芷', '女', 18, '13800000005', NULL
) d WHERE NOT EXISTS (SELECT 1 FROM t_student);
