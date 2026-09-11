import java.io.FileWriter;

public class WriteInput3 {
    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        // 1.注册
        sb.append("1\nuser1\n123456\n123456\n");
        // 2.登录
        sb.append("2\nuser1\n123456\n");
        // 3.班级管理->添加班级
        sb.append("1\n2\n一班\n30\n");
        // 4.班级管理->查看
        sb.append("1\n");
        // 5.班级管理->返回
        sb.append("4\n");
        // 6.学生管理->添加学生
        sb.append("2\n2\n1\n张三\n20\n13800138000\n一班\n");
        // 7.学生管理->查看
        sb.append("1\n");
        // 8.学生管理->返回
        sb.append("4\n");
        // 9.退出
        sb.append("3\n");

        try (FileWriter fw = new FileWriter("D:/workspace/edusystem/test3.txt")) {
            fw.write(sb.toString());
        }
        System.out.println("OK");
    }
}
