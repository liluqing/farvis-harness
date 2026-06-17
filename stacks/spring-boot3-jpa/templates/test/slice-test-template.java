package com.example.module;

import com.example.infra.slice.ModuleSliceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块切片测试模板 —— Phase 3 TDD 第③步。
 *
 * 只启动模块所需的 Spring Bean（SliceTestConfiguration），不启动完整应用。
 * 目标：验证 Spring 装配 + 核心行为 ≤ 30s。
 *
 * 外部 API 使用 WireMock 模拟。
 */
@SpringBootTest(
    classes = ResourceSliceTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE  // 不启动 Web 服务器
)
@ActiveProfiles("test")
class ResourceSliceTest {

    @Autowired
    private ResourceApplicationService service;

    @Test
    void should_load_application_context() {
        // 验证 Spring 装配成功
        assertThat(service).isNotNull();
    }

    @Test
    void should_execute_core_behavior() {
        // Given
        var command = new ResourceCommand("slice-req-001", "test", 100L);

        // When
        var result = service.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isPositive();
    }

    // ====== 并发测试（如果业务规则要求）==========

    // @Test
    // void should_keep_idempotent_under_concurrent_requests() throws Exception {
    //     int threads = 32;
    //     var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
    //     var latch = new java.util.concurrent.CountDownLatch(threads);
    //     var futures = new java.util.ArrayList<java.util.concurrent.Future<String>>();
    //
    //     for (int i = 0; i < threads; i++) {
    //         futures.add(executor.submit(() -> {
    //             try {
    //                 return service.create(new ResourceCommand("same-req-id", "test", 100L));
    //             } finally {
    //                 latch.countDown();
    //             }
    //         }));
    //     }
    //     latch.await();
    //
    //     // 所有请求返回同一个结果
    //     var results = futures.stream().map(f -> {
    //         try { return f.get().id(); }
    //         catch (Exception e) { throw new RuntimeException(e); }
    //     }).collect(java.util.stream.Collectors.toSet());
    //
    //     assertThat(results).hasSize(1);
    //     executor.shutdown();
    // }
}
