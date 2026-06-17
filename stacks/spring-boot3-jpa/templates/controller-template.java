package com.example.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Controller 模板。
 *
 * 约定：
 *  1. 只做协议转换和参数校验，不做业务编排
 *  2. @Valid 自动触发 Bean Validation
 *  3. 不 try-catch —— 异常统一走 GlobalExceptionHandler
 *  4. 统一返回 Result<T>
 *
 * 使用方式：Phase 3 切片的 API 端点按此模板生成。
 */
@RestController
@RequestMapping("/api/v1/{resources}")
public class ResourceController {

    private final ResourceApplicationService applicationService;

    public ResourceController(ResourceApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public Result<ResourceResponse> create(@Valid @RequestBody ResourceCreateRequest request) {
        ResourceCommand command = new ResourceCommand(
            request.getField1(),
            request.getField2()
        );
        ResourceResult result = applicationService.create(command);
        return Result.success(ResourceResponse.from(result));
    }

    @GetMapping("/{id}")
    public Result<ResourceResponse> get(@PathVariable Long id) {
        ResourceResult result = applicationService.findById(id);
        return Result.success(ResourceResponse.from(result));
    }
}
