package com.example.module;

import com.example.infra.outbox.OutboxEvent;
import com.example.infra.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 集成测试模板 —— Phase 3 TDD 第④步。
 *
 * 使用 Testcontainers 启动真实 MySQL/Redis，WireMock 模拟外部 API。
 * 只对关键业务路径编写集成测试（普通 CRUD 用切片测试即可）。
 *
 * 集成测试运行慢（≥ 30s），标记为 integrationTest task。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
class ResourceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ResourceApplicationService service;

    @Autowired
    private ResourceRepository repository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    void should_persist_and_retrieve_resource() {
        // Given
        var command = new ResourceCommand("int-req-001", "test", 100L);

        // When
        var result = service.create(command);

        // Then: 数据库中有记录
        var entity = repository.findByBusinessKey("int-req-001");
        assertThat(entity).isPresent();
        assertThat(entity.get().getStatus()).isEqualTo(ResourceEntity.Status.CREATED);
    }

    @Test
    void should_create_outbox_event_in_same_transaction() {
        // Given
        var command = new ResourceCommand("int-req-002", "test", 100L);

        // When
        var result = service.create(command);

        // Then: Outbox 事件在同一事务中写入
        await().atMost(5, SECONDS).until(() ->
            outboxEventRepository.countByStatusIn(
                java.util.List.of(OutboxEvent.Status.NEW)
            ) >= 1
        );
    }
}
