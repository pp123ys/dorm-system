package net.wanhe.dormsystem.web.common;

import net.wanhe.dormsystem.exception.BuildingException;
import net.wanhe.dormsystem.exception.RoomException;
import net.wanhe.dormsystem.exception.StayException;
import net.wanhe.dormsystem.exception.StuException;
import net.wanhe.dormsystem.exception.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.InvocationTargetException;

/*
 * 全局异常处理：把控制台版的业务异常文案，原样转成前端可显示的中文提示。
 *
 * 这里有一个必须处理的坑（已用 javap 反编译 spring-web 5.3.31 的字节码确认）：
 *   InvocableHandlerMethod.doInvoke 捕获 InvocationTargetException 后，
 *   对受检异常会包装成 IllegalStateException("Invocation failure", cause) 再抛出；
 *   而既有业务异常（UserException / BuildingException / RoomException / StuException / StayException）
 *   全部是受检异常，因此 @ExceptionHandler(UserException.class) 之类匹配不到，
 *   最终落到兜底变成 500，用户看不到"该学号已存在"这类提示。
 *   （Spring 6 已改为解包后再抛；本项目受 JDK 1.8 限制只能用 5.3.x，必须自己解包。）
 *
 * 实际异常链是两层：IllegalStateException -> InvocationTargetException -> 业务异常，
 * 所以 unwrap() 要循环剥到不是包装类型为止。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------- 未登录：HTTP 401，前端据此跳回登录页 ---------- */

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<R<Void>> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new R<>(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), null));
    }

    /* ---------- 参数问题：HTTP 400 ---------- */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new R<>(HttpStatus.BAD_REQUEST.value(), "参数不合法:" + e.getMessage(), null));
    }

    /*
     * 受检业务异常被 Spring 5.3 包装成 IllegalStateException，这里剥壳后按真实类型分发。
     * 注意：不能写成 @ExceptionHandler(BusinessException.class) 这类统一类型，
     * 因为这些异常没有公共父类（现状如此，不改动既有代码）。
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> handleWrapped(IllegalStateException e) {
        return dispatch(unwrap(e));
    }

    /* ---------- 兜底：HTTP 500，详情只进日志 ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOther(Exception e) {
        return dispatch(unwrap(e));
    }

    /* ---------- 内部工具 ---------- */

    private ResponseEntity<R<Void>> dispatch(Throwable real) {
        if (real instanceof UserException) {
            return businessFail("登录", real.getMessage());
        }
        if (real instanceof BuildingException) {
            return businessFail("楼栋", real.getMessage());
        }
        if (real instanceof RoomException) {
            return businessFail("房间床位", real.getMessage());
        }
        if (real instanceof StuException) {
            return businessFail("学生", real.getMessage());
        }
        if (real instanceof StayException) {
            return businessFail("入住退住", real.getMessage());
        }
        if (real instanceof UnauthorizedException) {
            return handleUnauthorized((UnauthorizedException) real);
        }
        if (real instanceof IllegalArgumentException) {
            return handleIllegalArgument((IllegalArgumentException) real);
        }
        log.error("服务内部异常", real);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new R<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误, 请查看后端日志", null));
    }

    /*
     * 业务校验失败：HTTP 200 + code=1，message 为可直接展示的中文原因
     */
    private ResponseEntity<R<Void>> businessFail(String module, String message) {
        log.warn("业务校验失败({}): {}", module, message);
        return ResponseEntity.ok(R.fail(message));
    }

    /*
     * 剥掉 InvocationTargetException 与 Spring 的 IllegalStateException("Invocation failure", ...) 包装层。
     * 用循环 + 次数上限，避免异常 cause 链成环时死循环。
     */
    private Throwable unwrap(Throwable e) {
        Throwable cur = e;
        for (int i = 0; i < 10; i++) {
            Throwable next = null;
            if (cur instanceof InvocationTargetException) {
                next = ((InvocationTargetException) cur).getTargetException();
            } else if (cur instanceof IllegalStateException) {
                next = cur.getCause();
            }
            if (next == null || next == cur) {
                break;
            }
            cur = next;
        }
        return cur;
    }
}
