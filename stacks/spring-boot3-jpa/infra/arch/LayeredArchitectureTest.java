package com.example.infra.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 架构分层校验 —— 编译期强制执行编码规则。
 *
 * 对应 .harness/ai-context/coding-rules.yaml 中的分层约束。
 * CI 中运行：./gradlew test --tests "*ArchTest"
 */
class LayeredArchitectureTest {

    private static final String ROOT = "com.example";

    // ====== 分层定义 ======

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter().importPackages(ROOT);
    }

    @Test
    void should_respect_layer_dependencies() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy(ROOT + ".controller..")
            .layer("Service").definedBy(ROOT + ".service..")
            .layer("Repository").definedBy(ROOT + ".repository..")
            .layer("Infra").definedBy(ROOT + ".infra..")
            // Controller 只依赖 Service，不直接依赖 Repository
            .whereLayer("Controller").mayNotAccessAnyLayers("Repository", "Infra")
            // Service 不依赖 Controller
            .whereLayer("Service").mayNotAccessAnyLayers("Controller")
            // Repository 只被 Service 和 Infra 访问
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Infra")
            .check(classes);
    }

    // ====== 注解约束 ======

    @Test
    void controllers_should_be_annotated_with_RestController() {
        classes()
            .that().resideInAPackage(ROOT + ".controller..")
            .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .check(classes);
    }

    @Test
    void services_should_be_annotated_with_Service() {
        classes()
            .that().resideInAPackage(ROOT + ".service..")
            .and().areNotInterfaces()
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .check(classes);
    }

    // ====== 依赖约束 ======

    @Test
    void no_class_should_depend_on_KafkaTemplate_directly() {
        // 业务代码禁止直接调 KafkaTemplate——必须通过 Outbox
        noClasses()
            .that().resideOutsideOfPackage(ROOT + ".infra.outbox..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.kafka.core.KafkaTemplate")
            .check(classes);
    }

    @Test
    void no_class_should_depend_on_RestTemplate_directly() {
        // 业务代码禁止直接调 RestTemplate——必须通过专用 ExternalApiClient
        noClasses()
            .that().resideOutsideOfPackage(ROOT + ".infra.client..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
            .check(classes);
    }

    // ====== 命名约束 ======

    @Test
    void repository_interfaces_should_be_named_Repository() {
        classes()
            .that().areInterfaces()
            .and().resideInAPackage(ROOT + ".repository..")
            .should().haveSimpleNameEndingWith("Repository")
            .check(classes);
    }

    @Test
    void controller_classes_should_be_named_Controller() {
        classes()
            .that().resideInAPackage(ROOT + ".controller..")
            .should().haveSimpleNameEndingWith("Controller")
            .check(classes);
    }
}
