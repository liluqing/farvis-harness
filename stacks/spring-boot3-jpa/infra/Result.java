package com.example.infra;

/**
 * 统一返回体 —— 所有对外 API 使用此格式。
 *
 * 约定：
 *   code=0     → 成功
 *   code≠0     → 失败，code 对应 .harness/ai-context/error-catalog.yaml
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    private Result() {}

    // ====== 工厂方法 ======

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.code = mapCodeToInt(code);
        result.message = message;
        return result;
    }

    // ====== 错误码映射（可按项目扩展） ======

    private static int mapCodeToInt(String code) {
        return switch (code) {
            case "PARAM_INVALID"    -> 40001;
            case "NOT_FOUND"        -> 40401;
            case "CONFLICT"         -> 40901;
            case "INTERNAL_ERROR"   -> 50001;
            default                 -> 40000;  // 通用业务错误
        };
    }

    // ====== Getters ======

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
