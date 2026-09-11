import java.io.OutputStreamWriter;
import java.io.Writer;

public class WriteInput {
    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("1\n");           // 注册
        sb.append("user1\n");
        sb.append("123456\n");
        sb.append("123456\n");
        sb.append("2\n");           // 登录
        sb.append("user1\n");
        sb.append("123456\n");
        sb.append("1\n");           // 班级管理
        sb.append("2\n");           // 添加班级
        sb.append("一班\n");
        sb.append("30\n");
        sb.append("1\n");           // 查看班级
        sb.append("4\n");           // 返回
        sb.append("2\n");           // 学生管理
        sb.append("2\n");           // 添加学生
        sb.append("1\n");           // 学号
        sb.append("张三\n");
        sb.append("20\n");
        sb.append("13800138000\n");
        sb.append("一班\n");
        sb.append("1\n");           // 查看学生
        sb.append("2\n");           // 添加学生
        sb.append("2\n");           // 学号
        sb.append("李四\n");
        sb.append("22\n");
        sb.append("13900139000\n");
        sb.append("一班\n");
        sb.append("1\n");           // 查看学生
        sb.append("3\n");           // 删除学生
        sb.append("1\n");           // 学号1
        sb.append("1\n");           // 查看学生
        sb.append("4\n");           // 返回
        sb.append("2\n");           // 学生管理
        sb.append("2\n");           // 添加学生
        sb.append("3\n");           // 学号
        sb.append("王五\n");
        sb.append("25\n");
        sb.append("13700137000\n");
        sb.append("二班\n");
        sb.append("1\n");           // 查看学生
        sb.append("4\n");           // 返回
        sb.append("3\n");           // 退出

        // 使用 UTF-8 编码写入，确保中文正确显示
        String outFile = "D:/workspace/edusystem/test_input.txt";
        try (Writer w = new OutputStreamWriter(new java.io.FileOutputStream(outFile), "UTF-8")) {
            w.write(sb.toString());
        }
        System.out.println("输入文件已生成 (UTF-8): " + outFile);
    }
}
