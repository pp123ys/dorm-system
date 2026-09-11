package net.wanhe.dorm.util;

/*
 * 控制台表格对齐工具
 * 中文等全角字符在控制台占 2 个字符宽, 直接 String.format 会错位,
 * 这里统一按显示宽度补齐
 */
public class AlignUtil {

    /*
     * 计算字符串的显示宽度: 全角/CJK 字符算 2, 其他算 1
     */
    public static int width(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += isFullWidth(s.charAt(i)) ? 2 : 1;
        }
        return w;
    }

    private static boolean isFullWidth(char c) {
        return c >= 0x1100 && (
                c <= 0x115F
                        || c == 0x2329 || c == 0x232A
                        || (c >= 0x2E80 && c <= 0xA4CF && c != 0x303F)
                        || (c >= 0xAC00 && c <= 0xD7A3)
                        || (c >= 0xF900 && c <= 0xFAFF)
                        || (c >= 0xFE30 && c <= 0xFE6F)
                        || (c >= 0xFF00 && c <= 0xFF60)
                        || (c >= 0xFFE0 && c <= 0xFFE6));
    }

    /*
     * 右侧补空格到显示宽度 w
     */
    public static String padRight(String s, int w) {
        StringBuilder sb = new StringBuilder(s == null ? "" : s);
        while (width(sb.toString()) < w) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /*
     * 按显示宽度截断: 超过 w 的末尾用 "…" 收口, 避免长文本把表格撑错位
     */
    public static String truncate(String s, int w) {
        if (s == null) {
            return "";
        }
        if (width(s) <= w) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = isFullWidth(s.charAt(i)) ? 2 : 1;
            if (used + cw > w - 2) {
                break;
            }
            sb.append(s.charAt(i));
            used += cw;
        }
        return sb + "…";
    }

    /*
     * 打印一行表格: 每列 "| " + 补齐内容 + " ", 末尾再补一个 "|"
     */
    public static void printRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append("| ").append(padRight(cells[i], widths[i])).append(' ');
        }
        System.out.println(sb.append('|').toString());
    }

    /*
     * 打印分隔线: 每列宽度 = 列宽 + 2, 与 printRow 的占位严格对应
     */
    public static void printLine(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int w : widths) {
            sb.append('+');
            for (int i = 0; i < w + 2; i++) {
                sb.append('-');
            }
        }
        System.out.println(sb.append('+').toString());
    }
}
