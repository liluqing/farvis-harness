// Gradle Build — Java AI Coding 优化配置
//
// 设计目标：
//   1. 增量编译 ≤ 3s（改动单文件快速反馈）
//   2. 模块切片测试 ≤ 30s（只装配必要 Bean）
//   3. 慢测试独立任务（不阻塞日常开发）
//   4. Annotation Processor 白名单（减少编译负担）
//
// 使用方式：
//   快速测试：./gradlew fastTest
//   全量测试：./gradlew test
//   集成测试：./gradlew integrationTest

plugins {
    java
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    // 可选：Checkstyle / SpotBugs / ArchUnit
    // id("checkstyle")
    // id("com.github.spotbugs") version "6.0.8"
    // id("com.societegenerale.arch-unit") version "3.3.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

// ========== 依赖 ==========

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // implementation("org.springframework.kafka:spring-kafka")

    // 可观测性
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // 数据库
    runtimeOnly("com.mysql:mysql-connector-j")

    // Lombok（减少样板代码，但注意 @Setter 不要滥用）
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct（按需启用）
    // implementation("org.mapstruct:mapstruct:1.5.5.Final")
    // annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // ========== 测试 ==========

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:mysql:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("com.maciejwalkowiak.spring:wiremock-spring-boot:2.1.1")

    // 架构测试
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.awaitility:awaitility:4.2.1")
}

// ========== 编译配置 ==========

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.compilerArgs.addAll(
        listOf(
            "-parameters",
            "-Xlint:unchecked",
            "-Xlint:deprecation"
        )
    )
    // 独立编译进程，避免 OOM
    options.isFork = true
    options.forkOptions.memoryMaximumSize = "2g"
}

// ========== 测试配置 ==========

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // 并行测试 = CPU 核数 / 2
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    systemProperty("spring.profiles.active", "test")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// ========== 快速测试（只跑切片测试，3s 内）==========

tasks.register<Test>("fastTest") {
    description = "快速反馈：只跑模块切片测试 + 静态规则检查"
    group = "verification"

    useJUnitPlatform()
    filter {
        // 只跑切片测试
        includeTestsMatching("*SliceTest")
        includeTestsMatching("*SmokeTest")
    }
    systemProperty("spring.profiles.active", "test")
}

// ========== 集成测试（慢测试，按需触发）==========

tasks.register<Test>("integrationTest") {
    description = "集成测试：Testcontainers 真实依赖 + 契约测试"
    group = "verification"

    useJUnitPlatform()
    filter {
        includeTestsMatching("*IntegrationTest")
        includeTestsMatching("*ContractTest")
    }
    systemProperty("spring.profiles.active", "integration-test")
    // 集成测试不并行（共享容器资源）
    maxParallelForks = 1
}

// ========== 源码集 ==========

sourceSets {
    create("integrationTest") {
        java.srcDir("src/integration-test/java")
        resources.srcDir("src/integration-test/resources")
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}
