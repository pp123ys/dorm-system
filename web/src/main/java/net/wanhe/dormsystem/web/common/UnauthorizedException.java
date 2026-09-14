package net.wanhe.dormsystem.web.common;

/*
 * 未登录/token 无效。拦截器抛出，由 GlobalExceptionHandler 转成 HTTP 401。
 * 继承 RuntimeException，避免在方法签名上层层声明。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
