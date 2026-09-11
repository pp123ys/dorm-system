-- 学生宿舍管理系统 数据库脚本 (rg01)
-- 服务端为 MySQL 5.7, 不使用 8.0 专属语法

CREATE DATABASE IF NOT EXISTS rg01 DEFAULT CHARSET utf8mb4;
USE rg01;

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
