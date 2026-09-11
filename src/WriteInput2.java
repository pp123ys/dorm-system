import java.io.FileWriter;

public class WriteInput2 {
    public static void main(String[] args) throws Exception {
        // 注册 -> 登录 -> 添加班级 -> 查看班级 -> 返回 -> 添加学生1 -> 查看 -> 添加学生2 -> 查看 -> 退出
        StringBuilder sb = new StringBuilder();
        // 注册
        sb.append("1\nuser1\n123456\n123456\n");
        // 登录
        sb.append("2\nuser1\n123456\n");
        // 班级管理：添加班级
        sb.append("1\n2\n一班\n30\n");
        // 班级管理：查看
        sb.append("1\n");
        // 班级管理：返回
        sb.append("4\n");
        // 学生管理：添加学生1
        sb.append("2\n2\n1\n张三\n20\n13800138000\n一班\n");
        // 学生管理：查看
        sb.append("1\n");
        // 学生管理：添加学生2
        sb.append("2\n2\n李四\n22\n13900139000\n一班\n");
        // 学生管理：查看
        sb.append("1\n");
        // 学生管理：返回
        sb.append("4\n");
        // 退出
        sb.append("3\n");

        try (FileWriter fw = new FileWriter("D:/workspace/edusystem/test_input2.txt")) {
            fw.write(sb.toString());
        }
        System.out.println("Done");
    }
}
