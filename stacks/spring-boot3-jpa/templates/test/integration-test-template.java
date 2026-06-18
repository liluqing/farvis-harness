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

// =============================================================================
// Controller 集成测试模板 —— 使用 MockMvc 测试 HTTP 层（含认证）
// =============================================================================

/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller 集成测试模板 —— 使用 MockMvc 测试完整 HTTP 层。
 *
 * 约定：
 *  1. @SpringBootTest 启动完整容器
 *  2. @AutoConfigureMockMvc 注入 MockMvc，不需要 RANDOM_PORT
 *  3. @ActiveProfiles("test") 使用 test 配置（H2 内存库）
 *  4. login() helper 获取 JWT token，用于需要认证的端点
 *  5. CRUD + Auth 两类测试覆盖核心路径
 *
 * 使用方式：Phase 3 切片的 Controller 层集成测试按此模板生成。
 * /
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceRepository repository;

    // ── Login helper ─────────────────────────────────────────────────
    // 调用 /api/v1/auth/login 获取 JWT token，供需要认证的测试使用
    private String login(String username, String password) throws Exception {
        var loginRequest = """
            {"username": "%s", "password": "%s"}
            """.formatted(username, password);

        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isOk())
            .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("data").get("token").asText();
    }

    // ── CRUD 测试 ────────────────────────────────────────────────────

    @Test
    void should_create_resource_with_authentication() throws Exception {
        String token = login("admin", "password123");

        var request = """
            {"field1": "value1", "field2": "value2"}
            """;

        mockMvc.perform(post("/api/v1/resources")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void should_get_resource_by_id() throws Exception {
        String token = login("admin", "password123");
        // 先创建一条记录
        var entity = repository.save(new ResourceEntity("test-key", "data"));

        mockMvc.perform(get("/api/v1/resources/{id}", entity.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(entity.getId()));
    }

    // ── Auth 测试 ────────────────────────────────────────────────────

    @Test
    void should_reject_request_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/resources/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_request_with_invalid_token() throws Exception {
        mockMvc.perform(get("/api/v1/resources/1")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_allow_unauthenticated_login() throws Exception {
        var loginRequest = """
            {"username": "admin", "password": "password123"}
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists());
    }
}
*/
