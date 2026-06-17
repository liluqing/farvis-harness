package com.example.module;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 单元测试模板 —— Phase 3 TDD 第②步。
 *
 * Mock 所有外部依赖，只验证本类的逻辑。
 * 不启动 Spring 容器。
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceUnitTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private ResourceApplicationService service;

    @Test
    void should_create_resource_successfully() {
        // Given
        var command = new ResourceCommand("req-001", "value1", 100L);
        when(idempotencyService.tryAcquire(any(), any())).thenReturn(true);
        when(repository.save(any())).thenReturn(/* mock entity */ null);

        // When
        var result = service.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
    }

    @Test
    void should_return_existing_on_idempotent_hit() {
        // Given
        var command = new ResourceCommand("req-001", "value1", 100L);
        when(idempotencyService.tryAcquire(any(), any())).thenReturn(false);

        // When
        var result = service.create(command);

        // Then: 返回已有结果，不重复创建
        assertThat(result).isNotNull();
    }
}
