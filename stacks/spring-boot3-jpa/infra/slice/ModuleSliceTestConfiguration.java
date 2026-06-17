package com.example.infra.slice;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 模块切片测试配置。
 *
 * 只装配本模块所需的 Spring Bean，不启动完整应用。
 * 目标：Spring 装配 + 核心行为验证 ≤ 30s。
 *
 * 每个业务模块一个 SliceTestConfiguration 类。
 * @Import 中只列本模块涉及的 Service + 必要的自动配置。
 *
 * 使用方式：
 * <pre>
 * &#64;SpringBootTest(classes = XxxSliceTestConfiguration.class)
 * &#64;ActiveProfiles("test")
 * class XxxModuleSmokeTest {
 *     &#64;Autowired
 *     private XxxApplicationService service;
 *
 *     &#64;Test
 *     void should_execute_core_behavior() {
 *         // 验证核心行为
 *     }
 * }
 * </pre>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
    // ====== 替换为实际模块的 Service ======
    // XxxApplicationService.class,
    // XxxDomainService.class,

    // ====== 必要的自动配置 ======
    // JacksonAutoConfiguration.class,
    // ValidationAutoConfiguration.class,
})
public class ModuleSliceTestConfiguration {
    // 空类，注解驱动
}
