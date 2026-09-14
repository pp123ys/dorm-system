package net.wanhe.dormsystem.web.common;

/*
 * 统一响应体：code=0 表示成功，非 0 表示业务失败（此时 message 为中文原因）。
 * 让前端只需要判断一个字段，不必区分 HTTP 状态码与业务状态码两种失败。
 */
public class R<T> {

    public static final int OK = 0;
    public static final int FAIL = 1;

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(OK, "ok", data);
    }

    public static <T> R<T> ok() {
        return new R<>(OK, "ok", null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(FAIL, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
