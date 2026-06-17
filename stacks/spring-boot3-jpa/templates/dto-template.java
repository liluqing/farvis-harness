package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO 模板。
 *
 * 约定：
 *  1. Request DTO：纯数据载体 + Bean Validation
 *  2. Response DTO：纯数据载体 + 静态工厂方法 from(Entity)
 *  3. DTO 不包含业务逻辑
 *
 * 使用方式：Phase 3 切片按此模板生成对应的 Request/Response。
 */
public class ResourceCreateRequest {

    @NotBlank(message = "field1 不能为空")
    @Size(max = 100, message = "field1 最大 100 字符")
    private String field1;

    @NotNull(message = "field2 不能为空")
    private Long field2;

    // ====== Getters & Setters（DTO 允许 Setter） ======

    public String getField1() { return field1; }
    public void setField1(String field1) { this.field1 = field1; }

    public Long getField2() { return field2; }
    public void setField2(Long field2) { this.field2 = field2; }
}


/**
 * Response DTO 模板。
 */
class ResourceResponse {

    private Long id;
    private String field1;
    private Long field2;
    private String status;
    private String createdAt;

    /** 从领域结果构建 Response */
    public static ResourceResponse from(ResourceResult result) {
        ResourceResponse response = new ResourceResponse();
        response.id = result.id();
        response.field1 = result.field1();
        response.field2 = result.field2();
        response.status = result.status().name();
        response.createdAt = result.createdAt().toString();
        return response;
    }

    // ====== Getters ======

    public Long getId() { return id; }
    public String getField1() { return field1; }
    public Long getField2() { return field2; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
