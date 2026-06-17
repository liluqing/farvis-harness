# 环境一致性

## 要解决的问题

"我机器上能跑"是 AI Coding 最大的谎言。

Java 项目尤其：JDK 版本、MySQL 参数、Redis 配置、Kafka 版本、application-local.yml 各改各的——AI 写的代码在这台机器上跑通，换一台就炸。

对环境漂移，AI 比人类更脆弱，因为模型无法推断你机器上的实际运行条件。

## 核心原则

**本地环境应该像"小型生产环境"，而不是"侥幸能跑的环境"。**

### 三层环境策略

| 层 | 工具 | 适用场景 |
|----|------|---------|
| 容器化依赖 | Docker Compose | 个人开发：一键启动 MySQL/Redis/Kafka |
| 外部 API 模拟 | WireMock / MockServer | 个人开发 + 测试：模拟第三方 API |
| 代码级环境声明 | Testcontainers | 自动化测试：测试代码自带依赖声明 |

三者互补，不是二选一：
- Docker Compose → 日常开发
- Testcontainers → CI 自动化
- WireMock → 隔离外部依赖

### Outbox 模式（跨技术栈通用概念）

任何涉及"DB 写操作 + 消息发送"的场景，都不能：

```
❌ db.save(order);
❌ kafka.send("order-created", order.getId());  // 事务提交失败 → 脏消息
```

而应该：

```
✅ 在同一事务中：db.save(order) + db.save(outboxEvent)
✅ 独立投递器异步发送消息
✅ 发送成功 → 标记已发送；发送失败 → 重试
```

这是一个**概念层面的模式**，不绑定任何 ORM 或消息队列。

## 与技术栈无关的检查清单

- [ ] 本地依赖是否容器化？
- [ ] 外部 API 是否有本地模拟？
- [ ] 有没有"只存在于某位同事电脑上的配置"？
- [ ] DB 事务 + 消息发送是否使用 Outbox 或等价机制？
- [ ] 环境差异是否已文档化？
