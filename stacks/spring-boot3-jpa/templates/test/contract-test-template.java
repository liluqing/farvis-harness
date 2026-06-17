package com.example.test.contract;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 契约测试模板 —— 验证本服务与外部 API 的交互契约。
 *
 * 设计原则：
 *  1. 使用 WireMock 模拟外部服务（不调真实 API）
 *  2. 每个外部服务一个 ContractTest 类
 *  3. 测试命名：contract_{provider}_{operation}_{scenario}
 *  4. 断言点：HTTP 状态码 + 响应关键字段 + 外部调用确实发生（verify）
 *
 * 何时写契约测试：
 *  - 调了外部 HTTP API（非本项目服务）
 *  - 异步接收外部回调/Webhook
 *  - 外部 API 的 stub 定义在 .harness/devops/wiremock/stubs/ 下
 *
 * 使用方式：
 *   1. 复制本文件，替换 ExternalServiceName 为实际服务名
 *   2. 每个外部 API 端点写一个 @Test
 *   3. 外部服务 stub 放在 .harness/devops/wiremock/stubs/{service}.json
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // 替换为实际的外部 API base-url property
        "app.external-api.{service-name}.base-url=http://localhost:${wiremock.server.port}"
    }
)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)  // 随机端口，避免冲突
class ExternalServiceNameContractTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WireMock.reset();
    }

    // ====== 正常场景 ======

    @Test
    void contract_heygen_generateVideo_shouldReturnVideoId_onSuccess() {
        // Given: 外部 API stub（与 wiremock/stubs/heygen-api.json 保持一致）
        stubFor(post(urlEqualTo("/v2/video.generate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "code": 0,
                        "message": "success",
                        "data": {
                            "video_id": "video_test_001"
                        }
                    }
                    """)));

        // When: 调用本服务接口（内部触发外部 API 调用）
        // Then: 验证响应 + 验证外部调用确实发生
        mockMvc.perform(post("/api/v1/videos/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "avatarId": "avatar_001",
                        "text": "Hello World"
                    }
                    """))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.videoId").isNotEmpty());

        // 契约断言：外部 API 确实被调了一次
        verify(1, postRequestedFor(urlEqualTo("/v2/video.generate")));
    }

    // ====== 异常场景 ======

    @Test
    void contract_heygen_generateVideo_shouldHandle409_onDuplicate() {
        // Given: 外部 API 返回 409（重复提交）
        stubFor(post(urlEqualTo("/v2/video.generate"))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"code": 409001, "message": "A video generation is already in progress"}
                    """)));

        // When: 重复提交
        mockMvc.perform(post("/api/v1/videos/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"avatarId": "avatar_001", "text": "duplicate request"}
                    """))
            // Then: 本服务应返回 409 或降级响应
            .andExpect(status().is4xxClientError());
    }

    @Test
    void contract_heygen_generateVideo_shouldFallback_onTimeout() {
        // Given: 外部 API 超时（模拟慢响应 > 超时阈值）
        stubFor(post(urlEqualTo("/v2/video.generate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(31_000)  // 31 秒，超过 readTimeout(30s)
                .withBody("""{"code": 0, "data": {"video_id": "slow_001"}}""")));

        // When & Then: 应触发重试 + 最终降级，返回合理响应
        mockMvc.perform(post("/api/v1/videos/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avatarId": "avatar_timeout", "text": "timeout test"}"""))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.code").value("DOWNSTREAM_TIMEOUT"));
    }

    @Test
    void contract_heygen_generateVideo_shouldRetry_on500() {
        // Given: 外部 API 前 2 次返回 500，第 3 次成功
        stubFor(post(urlEqualTo("/v2/video.generate"))
            .inScenario("Retry on 500")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Attempt 2"));

        stubFor(post(urlEqualTo("/v2/video.generate"))
            .inScenario("Retry on 500")
            .whenScenarioStateIs("Attempt 2")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Attempt 3"));

        stubFor(post(urlEqualTo("/v2/video.generate"))
            .inScenario("Retry on 500")
            .whenScenarioStateIs("Attempt 3")
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""{"code": 0, "data": {"video_id": "retry_ok_001"}}""")));

        // When & Then: 重试后成功
        mockMvc.perform(post("/api/v1/videos/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avatarId": "avatar_retry", "text": "retry test"}"""))
            .andExpect(status().isAccepted());

        // 契约断言：外部 API 被调了 3 次
        verify(3, postRequestedFor(urlEqualTo("/v2/video.generate")));
    }

    // ====== 契约不变性断言 ======

    @Test
    void contract_heygen_requestMustContain_avatarId_and_text() {
        // 验证：本服务发出的请求必须包含 avatar_id 和 text 字段
        // 这是与外部 API 的契约 —— 如果外部 API 改了字段名，这里会挂
        stubFor(post(urlEqualTo("/v2/video.generate"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("""{"code": 0, "data": {"video_id": "test"}}""")));

        mockMvc.perform(post("/api/v1/videos/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"avatarId": "avatar_001", "text": "contract test"}"""))
            .andExpect(status().isAccepted());

        // 断言请求体包含必要字段
        verify(postRequestedFor(urlEqualTo("/v2/video.generate"))
            .withRequestBody(matchingJsonPath("$.avatar_id"))
            .withRequestBody(matchingJsonPath("$.text")));
    }
}
