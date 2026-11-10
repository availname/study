-- community 社区论坛建表（复用 mq-mall 的 MySQL 容器，独立 database）
CREATE TABLE IF NOT EXISTS t_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    password   VARCHAR(64)  NOT NULL,
    points     INT          NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户（DB为真源，Redis存登录态/积分榜）';

-- 帖子：正文真源 + 审核状态机（0待审 1通过 2拒绝 3待人工）
-- view_count/like_count 是 DB 底账：Redis 计数削峰后由 CounterFlushJob 周期刷进来
CREATE TABLE IF NOT EXISTS t_post (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(128) NOT NULL,
    content       TEXT,
    author_id     BIGINT       NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    reject_reason VARCHAR(255) NULL,
    view_count    BIGINT       NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_status (status),
    KEY idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子';

-- 积分流水：message_id 唯一索引幂等（发帖奖励/签到奖励）
CREATE TABLE IF NOT EXISTS t_points_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    points     INT         NOT NULL,
    reason     VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message (message_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 本地消息表（与 mq-mall 同款可靠性基建，模式复用）
CREATE TABLE IF NOT EXISTS t_mq_send_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id    VARCHAR(64)  NOT NULL,
    exchange      VARCHAR(64)  NOT NULL,
    routing_key   VARCHAR(64)  NOT NULL,
    payload       TEXT         NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2失败 3死信',
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3)  NULL,
    last_error    VARCHAR(255) NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message (message_id),
    KEY idx_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ发送记录(本地消息表)';

-- 消费去重记录（幂等第二道防线，24h 清理）
CREATE TABLE IF NOT EXISTS t_mq_consume_record (
    message_id VARCHAR(64) PRIMARY KEY,
    consumer   VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消费去重';

-- 死信存档（重试耗尽的消息落库人工兜底）
CREATE TABLE IF NOT EXISTS t_mq_dead_letter (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id   VARCHAR(64)  NULL,
    source_queue VARCHAR(128) NULL,
    reason       VARCHAR(64)  NULL,
    payload      TEXT         NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信存档';
