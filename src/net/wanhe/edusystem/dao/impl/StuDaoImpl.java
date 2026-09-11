package net.wanhe.edusystem.dao.impl;

import net.wanhe.edusystem.dao.StuDao;
import net.wanhe.edusystem.pojo.Clazz;
import net.wanhe.edusystem.pojo.Student;
import net.wanhe.edusystem.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/*
 * 学生表只存 clazz_name 列, 不存Clazz对象
 * 查询时用 left join t_clazz 把班级信息拼回来, 还原出 Student.c
 */
public class StuDaoImpl implements StuDao {

    /*
     * 查询学生的公共SQL: 学生字段 + 班级字段
     */
    private static final String SQL_FIND_STU =
            "select s.no,s.name,s.age,s.phone,s.clazz_name,c.name as cname,c.`count` as ccount " +
            "from t_student s left join t_clazz c on s.clazz_name = c.name";

    @Override
    public List<Student> select() {
        return findBySql(SQL_FIND_STU, null);
    }

    @Override
    public Student selectByNo(int no) {
        List<Student> array = findBySql(SQL_FIND_STU + " where s.no = ?", String.valueOf(no));
        if(array.isEmpty()){
            return null;
        }
        return array.get(0);
    }

    @Override
    public void insert(Student stu) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "insert into t_student(no,name,age,phone,clazz_name) values(?,?,?,?,?)";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值 (班级只存名称)
            state.setInt(1, stu.getNo());
            state.setString(2, stu.getName());
            state.setInt(3, stu.getAge());
            state.setString(4, stu.getPhone());
            state.setString(5, stu.getC().getName());
            //6.执行sql语句
            state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.insert失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(null,state,conn);
        }
    }

    @Override
    public void delete(int no) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //3.定义SQL语句
            String sql = "delete from t_student where no = ?";
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            state.setInt(1, no);
            //6.执行sql语句
            state.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("StuDao.delete失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(null,state,conn);
        }
    }

    @Override
    public List<Student> selectByClazzName(String clazzName) {
        return findBySql(SQL_FIND_STU + " where s.clazz_name = ?", clazzName);
    }

    /*
     * 执行查询SQL并映射成Student集合
     * param: 预编译?的值, 不需要为null
     */
    private List<Student> findBySql(String sql, String param) {
        List<Student> stus = new ArrayList<>();
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet rs = null;
        try {
            //2.获取一个对数据库的连接对象
            conn = JdbcUtil.getConnection();
            //4.对sql语句进行预编译
            state = conn.prepareStatement(sql);
            //5.为?赋值
            if(param != null){
                state.setString(1, param);
            }
            //6.执行sql语句
            rs = state.executeQuery();
            //7.处理结果集: 把班级信息拼回 Student.c
            while(rs.next()){
                int no = rs.getInt("no");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String phone = rs.getString("phone");
                String stuClazzName = rs.getString("clazz_name");
                String cname = rs.getString("cname");
                int ccount = rs.getInt("ccount");
                //班级已被删除时 join 不到班级行, cname为null,
                //用学生表里存的班级名兜底建个Clazz, 保证上层不空指针
                Clazz c = (cname != null) ? new Clazz(cname, ccount) : new Clazz(stuClazzName, 0);
                stus.add(new Student(no,name,age,phone,c));
            }
            return stus;
        } catch (Exception e) {
            throw new RuntimeException("StuDao查询失败", e);
        } finally {
            //8.关闭资源
            JdbcUtil.close(rs,state,conn);
        }
    }
}
