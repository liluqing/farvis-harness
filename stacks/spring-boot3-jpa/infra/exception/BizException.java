package com.example.infra.exception;

/**
 * 业务异常 —— 对应 .harness/ai-context/error-catalog.yaml 中的错误码。
 *
 * 使用方式：
 *   throw new BizException("CREDITS_INSUFFICIENT", "Credits 余额不足", HttpStatus.PAYMENT_REQUIRED);
 */
public class BizException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public BizException(String code, String message) {
        this(code, message, 400);
    }

    public BizException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BizException(String code, String message, org.springframework.http.HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus.value();
    }

    public String getCode() { return code; }
    public int getHttpStatus() { return httpStatus; }
}
