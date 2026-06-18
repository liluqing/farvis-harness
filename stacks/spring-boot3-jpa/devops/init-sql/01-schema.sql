-- ============================================================
-- Harness 初始化 SQL
-- 首次启动 Docker Compose 时自动执行（mysql:/docker-entrypoint-initdb.d）
-- ============================================================

-- ====== Harness 基础设施表 ======

-- Outbox 事件表 —— 确保 DB 事务 + 消息发送原子性
CREATE TABLE IF NOT EXISTS outbox_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(100)  NOT NULL COMMENT '事件类型，如 order_created, video_generated',
    aggregate_id    VARCHAR(100)  NOT NULL COMMENT '聚合根 ID',
    payload         TEXT          NOT NULL COMMENT '事件 payload（JSON）',
    status          VARCHAR(20)   NOT NULL DEFAULT 'NEW' COMMENT 'NEW / SENT / RETRY / DEAD',
    retry_count     INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
    last_error      VARCHAR(2000) NULL     COMMENT '最后一次错误信息',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_outbox_status (status, id),
    INDEX idx_outbox_aggregate (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbox 事件表';


-- ====== 业务表模板（按项目实际表结构替换）======

-- 示例：通用业务表模板
-- CREATE TABLE IF NOT EXISTS resources (
--     id              BIGINT AUTO_INCREMENT PRIMARY KEY,
--     business_key    VARCHAR(100)  NOT NULL COMMENT '业务主键',
--     status          VARCHAR(20)   NOT NULL DEFAULT 'CREATED' COMMENT '状态',
--     created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--
--     UNIQUE KEY uk_business_key (business_key),
--     INDEX idx_status (status)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务表模板';
