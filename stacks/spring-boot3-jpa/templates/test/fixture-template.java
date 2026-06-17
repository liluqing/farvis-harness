package com.example.test.fixture;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 测试数据工厂 —— 统一管理 Test Fixtures。
 *
 * 设计原则：
 *  1. 每个实体一个工厂类，放在 test/fixture/ 包下
 *  2. 工厂方法返回「合理默认值」，测试只覆盖关心的字段
 *  3. 使用 Builder + Consumer 模式，允许链式定制
 *  4. 不依赖数据库——返回的是未持久化的对象，测试中自行 save
 *
 * 使用方式：
 *   // 默认值
 *   Order order = OrderFixture.defaultOrder();
 *
 *   // 定制
 *   Order order = OrderFixture.order(o -> {
 *       o.setStatus(OrderStatus.PAID);
 *       o.setAmount(new BigDecimal("99.00"));
 *   });
 *
 *   // 批量
 *   List<Order> orders = OrderFixture.orders(5, o -> o.setStatus(OrderStatus.PENDING));
 */
public final class OrderFixture {

    private OrderFixture() {}

    // ====== 工厂方法 ======

    public static Order defaultOrder() {
        return order(null);
    }

    public static Order order(Consumer<Order> customizer) {
        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        order.setRequestId(UUID.randomUUID().toString());
        order.setSkuCode("SKU-DEFAULT");
        order.setQuantity(1);
        order.setAmount(new BigDecimal("19.90"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        order.setUpdatedAt(Instant.now());

        if (customizer != null) {
            customizer.accept(order);
        }
        return order;
    }

    /** 批量创建 N 条（使用默认值） */
    public static java.util.List<Order> orders(int count) {
        return orders(count, null);
    }

    /** 批量创建 N 条（带定制） */
    public static java.util.List<Order> orders(int count, Consumer<Order> customizer) {
        java.util.List<Order> list = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Order order = defaultOrder();
            order.setOrderId("ORD-" + String.format("%04d", i + 1));
            if (customizer != null) {
                customizer.accept(order);
            }
            list.add(order);
        }
        return list;
    }

    // ====== 预定义场景 ======

    /** 已支付的订单 */
    public static Order paidOrder() {
        return order(o -> o.setStatus(OrderStatus.PAID));
    }

    /** 已取消的订单 */
    public static Order cancelledOrder() {
        return order(o -> o.setStatus(OrderStatus.CANCELLED));
    }

    /** 金额为 0 的订单（边界测试） */
    public static Order zeroAmountOrder() {
        return order(o -> o.setAmount(BigDecimal.ZERO));
    }
}

// ====== 复制此模板为新实体创建 Factory ======
//
// 1. 复制本文件，替换 Order 为你的实体类
// 2. 保留 defaultXxx() / xxx(customizer) / xxxs(count, customizer) 三个方法
// 3. 按实体字段填充默认值
// 4. 添加预定义场景方法（如 paidOrder()）
//
// 命名规范：
//   类名：{Entity}Fixture
//   方法：default{Entity}() / {entity}(Consumer) / {entity}s(int, Consumer)
