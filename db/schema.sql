-- 教育系统 edusystem 数据库脚本 (rg01)
USE rg01;

-- 用户表
CREATE TABLE t_user (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  login_name VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(50) NOT NULL COMMENT '密码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户表';

-- 班级表
CREATE TABLE t_clazz (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  name VARCHAR(50) NOT NULL UNIQUE COMMENT '班级名称',
  `count` INT NOT NULL COMMENT '座位数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '班级表';

-- 学生表 (clazz_name 存班级名称, 不加外键约束: 删除班级不影响学生记录)
CREATE TABLE t_student (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
  no INT NOT NULL UNIQUE COMMENT '学号',
  name VARCHAR(50) COMMENT '姓名',
  age INT COMMENT '年龄',
  phone VARCHAR(20) COMMENT '电话',
  clazz_name VARCHAR(50) COMMENT '所属班级名称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '学生表';
