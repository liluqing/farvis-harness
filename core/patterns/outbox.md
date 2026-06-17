# Outbox 模式

## 问题

涉及"数据库写操作 + 消息发送"的流程，两步不能原子化：

```
db.save(order);                   // 成功
kafka.send("order-created", ...); // 失败 → 数据不一致
```

或反过来：消息发送成功，但事务回滚 → 消费者收到脏消息。

## 模式

**把"业务变更"和"待发送事件"放在同一本地事务中，由独立投递器异步发送。**

```
事务开始
  ├── 写业务数据（order）
  └── 写 OutboxEvent（event_type="order_created", payload=JSON）
事务提交

独立投递器（定时/轮询）
  └── 扫描 OutboxEvent (status=NEW)
       ├── 发送到消息队列
       ├── 成功 → 标记 status=SENT
       └── 失败 → 标记 status=RETRY（重试 N 次后 → DEAD）
```

## 关键决策

| 决策点 | 选项 | 选型考虑 |
|--------|------|---------|
| Outbox 存储 | 同库附表 / 独立库 / Redis Stream | 同库附表最简单，事务天然保证 |
| 投递方式 | 定时轮询 / CDC（Debezium）/ 事务提交后 Hook | 定时轮询最简单，CDC 延迟最低 |
| 投递保证 | at-most-once / at-least-once | 业务幂等 + at-least-once |
| 重试策略 | 固定间隔 / 指数退避 / 死信队列 | 指数退避 + 最大重试次数 + DEAD 告警 |

## 与技术栈无关的约束

- 业务代码不允许直接发消息（fire-and-forget）
- OutboxEvent 的写入和业务数据写入必须在同一事务
- 投递失败不能阻塞业务请求
- DEAD 事件必须产生告警
