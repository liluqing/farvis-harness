package com.example.infra.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * 约定：
 *  1. 所有 Controller 不 try-catch，异常统一由此处理
 *  2. 未知异常返回 500 + traceId，便于定位
 *  3. 错误信息可从 .harness/ai-context/error-catalog.yaml 中查到
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ====== 业务异常 ======

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException ex) {
        log.warn("BizException: code={}, message={}, traceId={}",
            ex.getCode(), ex.getMessage(), MDC.get("traceId"));
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    // ====== 参数校验异常 ======

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", detail);
        return ResponseEntity
            .badRequest()
            .body(Result.fail("PARAM_INVALID", detail));
    }

    // ====== 未知异常 ======

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex) {
        log.error("Unknown error: traceId={}", MDC.get("traceId"), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.fail("INTERNAL_ERROR", "服务内部错误，traceId=" + MDC.get("traceId")));
    }
}
