# Review Notes

> **迭代**：2026-06-19_积分体系_v1.0

---

## PRD Review（Google Staff PM）

- [x] 已完成（2026-06-19）
- 发现 7 Critical + 10 Major + 7 Minor
- 详见 PRD review 部分（已按选项 B 简化为内部演示版）

---

## 技术设计 Review（双视角）

> 日期：2026-06-19
> 视角 1：资深后端架构师（阿里 P9，分布式事务/支付系统）
> 视角 2：资深业务架构师（美团/字节，业务场景反推技术设计）

### 🔴 Critical（5 项，必须改）

| # | 问题 | 修改方案 |
|---|------|---------|
| C1 | 幂等形同虚设：ref_type+ref_id 无 UNIQUE KEY | 加唯一索引，deduct 入口先查索引做幂等短路 |
| C2 | 充值事务断点：步骤2成功步骤3失败 | 步骤2+3 合并同一事务 |
| C3 | total_recharged/total_consumed 流程中未更新 | 充值补 total_recharged+=credits，扣减补 total_consumed+=cost |
| C4 | 缺套餐列表接口 | 新增 GET /api/v1/credits/plans + 枚举类 |
| C5 | 注册赠送无幂等 | INSERT 捕获 DuplicateKey → 已存在则返回当前余额 |

### 🟠 Major（7 项，开发前明确）

| # | 问题 | 修改方案 |
|---|------|---------|
| M1 | deduct 入参未定义 | 入参改 consumeType，服务内部维护价格映射 |
| M2 | balance 返回值未定义 | 定义响应体结构 |
| M3 | 模拟充值入参未定义 | 定义 {planCode}，order_no 服务端生成 |
| M4 | user_id 来源未说明 | 从 JWT token 解析 |
| M5 | 消费标准无配置化 | 枚举类统一管理 |
| M6 | Redis 用途未提及 | 明确演示版暂不用 |
| M7 | balance 无 CHECK 约束 | 加 CHECK (balance >= 0) |
