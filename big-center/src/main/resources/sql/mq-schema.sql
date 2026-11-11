-- 可靠消息全家桶三张表（mq-mall stage3 总装，随 spring.sql.init 自动执行）

CREATE TABLE IF NOT EXISTS t_mq_send_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id    VARCHAR(64)  NOT NULL COMMENT '业务幂等键，重发复用同一个',
    exchange      VARCHAR(64)  NOT NULL,
    routing_key   VARCHAR(64)  NOT NULL,
    payload       TEXT         NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2失败 3死信(重试超限)',
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3)  NULL,
    last_error    VARCHAR(255) NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message (message_id),
    KEY idx_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ发送记录(本地消息表)';

CREATE TABLE IF NOT EXISTS t_mq_consume_record (
    message_id VARCHAR(64) PRIMARY KEY COMMENT '唯一索引即幂等第二道防线',
    consumer   VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消费去重';

CREATE TABLE IF NOT EXISTS t_mq_dead_letter (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id   VARCHAR(64)  NULL,
    source_queue VARCHAR(128) NULL,
    reason       VARCHAR(64)  NULL,
    payload      TEXT         NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信落库(重试耗尽人工兜底)';
