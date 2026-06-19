# DDL 变更记录

> **迭代**：2026-06-19_积分体系_v1.0
> **最后更新**：2026-06-19

---

## 变更清单

| # | 操作 | 表 | 说明 |
|:--:|------|------|------|
| D1 | CREATE | credits_account | 积分账户表（新增） |
| D2 | CREATE | credits_transaction | 积分流水表（新增） |
| D3 | CREATE | payment_order | 支付订单表（新增） |

---

## 详细 DDL

### D1: credits_account（积分账户表）

```sql
CREATE TABLE credits_account (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT NOT NULL COMMENT '用户 ID',
    balance         INT NOT NULL DEFAULT 0 COMMENT '当前余额（Credits）',
    total_recharged INT NOT NULL DEFAULT 0 COMMENT '累计充值（Credits）',
    total_consumed  INT NOT NULL DEFAULT 0 COMMENT '累计消费（Credits）',
    plan_code       VARCHAR(32) DEFAULT NULL COMMENT '当前套餐编码（starter/pro/enterprise）',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT chk_balance CHECK (balance >= 0),
    CONSTRAINT chk_recharged CHECK (total_recharged >= 0),
    CONSTRAINT chk_consumed CHECK (total_consumed >= 0)
) COMMENT='积分账户表';
```

### D2: credits_transaction（积分流水表）

```sql
CREATE TABLE credits_transaction (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT NOT NULL COMMENT '用户 ID',
    type            VARCHAR(32) NOT NULL COMMENT '变动类型：RECHARGE/CONSUME/GIFT',
    amount          INT NOT NULL COMMENT '变动数量（正数=充值/赠送，负数=消费）',
    balance_after   INT NOT NULL COMMENT '变动后余额',
    ref_type        VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型：VIDEO/AVATAR/VOICE/PAYMENT/REGISTER',
    ref_id          VARCHAR(64) DEFAULT NULL COMMENT '关联业务 ID（订单号/视频ID 等）',
    description     VARCHAR(256) DEFAULT NULL COMMENT '描述（展示用）',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id_created (user_id, created_at),
    UNIQUE KEY uk_ref (ref_type, ref_id)
) COMMENT='积分流水表';
```

### D3: payment_order（支付订单表）

```sql
CREATE TABLE payment_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(64) NOT NULL COMMENT '订单号（PAY+yyyyMMddHHmmss+4位随机）',
    user_id         BIGINT NOT NULL COMMENT '用户 ID',
    plan_code       VARCHAR(32) NOT NULL COMMENT '套餐编码：starter/pro/enterprise',
    amount          DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    credits         INT NOT NULL COMMENT '充值 Credits 数量',
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PAID/CANCELLED/FAILED',
    paid_at         DATETIME DEFAULT NULL COMMENT '支付完成时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_id_status (user_id, status),
    CONSTRAINT chk_status CHECK (status IN ('PENDING','PAID','CANCELLED','FAILED'))
) COMMENT='支付订单表';
```
