-- mq-mall 建表脚本（CREATE IF NOT EXISTS，可重复启动）
-- spring.sql.init.mode=always 自动执行

-- ============ 订单域 ============

-- 订单表。状态机：10待支付 → 20已支付 / 50超时关闭（教程 stage4/02）
CREATE TABLE IF NOT EXISTS t_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no    VARCHAR(32)  NOT NULL COMMENT '订单号',
    user_id     BIGINT       NOT NULL,
    sku_id      BIGINT       NOT NULL,
    count       INT          NOT NULL DEFAULT 1,
    amount      INT          NOT NULL DEFAULT 0 COMMENT '金额（分）',
    status      TINYINT      NOT NULL COMMENT '10待支付 20已支付 50超时关闭',
    seckill_flag TINYINT     NULL COMMENT 'NULL普通订单 / 1秒杀订单（NULL 不参与唯一约束，见 uk_user_sku）',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at     DATETIME(3)  NULL,
    closed_at   DATETIME(3)  NULL,
    UNIQUE KEY uk_order_no (order_no),
    -- 秒杀"一人一单"兜底：seckill_flag 为 NULL 的普通订单允许多条（MySQL 唯一索引含 NULL 不去重）
    UNIQUE KEY uk_user_sku (user_id, sku_id, seckill_flag),
    KEY idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 库存表（教学版单 SKU；真实系统按 SKU 分表/分库）
CREATE TABLE IF NOT EXISTS t_stock (
    sku_id  BIGINT PRIMARY KEY,
    stock   INT NOT NULL COMMENT '当前可售库存',
    total   INT NOT NULL COMMENT '总量，用于对账'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';

INSERT IGNORE INTO t_stock (sku_id, stock, total) VALUES (1, 1000, 1000);

-- 库存流水（LOCK锁定/DEDUCT实扣/RELEASE释放），对账的数据来源
CREATE TABLE IF NOT EXISTS t_stock_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no   VARCHAR(32) NOT NULL,
    sku_id     BIGINT      NOT NULL,
    count      INT         NOT NULL,
    action     VARCHAR(10) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_order (order_no),
    KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

-- ============ MQ 可靠性基建（教程 stage3/01 本地消息表 + stage3/02 幂等）============

-- 发送记录表 = 本地消息表：与业务同事务写入，事务提交后发送，Confirm/补偿双通道保证送达
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

-- 消费去重记录（第二道幂等闸，Redis setnx 是第一道；定时清理 24h 前记录）
CREATE TABLE IF NOT EXISTS t_mq_consume_record (
    message_id VARCHAR(64) PRIMARY KEY COMMENT '通常=业务messageId，唯一索引即幂等',
    consumer   VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消费去重';

-- 死信落库（stage3/05：重试耗尽的消息由死信消费者写入，人工兜底）
CREATE TABLE IF NOT EXISTS t_mq_dead_letter (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id   VARCHAR(64)  NULL,
    source_queue VARCHAR(128) NULL,
    reason       VARCHAR(64)  NULL,
    payload      TEXT         NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信存档';

-- ============ 支付成功下游业务（教程 stage4/03）============

-- 积分流水：message_id 唯一索引 = 幂等兜底（stage3/02 方案1）
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

CREATE TABLE IF NOT EXISTS t_points_account (
    user_id    BIGINT PRIMARY KEY,
    points     INT         NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分账户';

-- 优惠券：业务唯一键三元组幂等（stage3/02，防"重启+Redis失效"重复发券事故）
CREATE TABLE IF NOT EXISTS t_coupon (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    activity    VARCHAR(32) NOT NULL,
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0未使用',
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_coupon (user_id, coupon_type, activity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

-- ============ 媒体处理流水线（stage5 视频平台场景）============

-- 视频处理任务表：状态机推进（UPLOADED→TRANSCODING→AUDITING→PUBLISHED，可分叉 REJECTED/FAILED/HUMAN_AUDIT）
CREATE TABLE IF NOT EXISTS t_media_task (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id    VARCHAR(64)  NOT NULL COMMENT '全局唯一视频ID，各环节消息的幂等键',
    title       VARCHAR(128) NOT NULL,
    status      VARCHAR(16)  NOT NULL COMMENT 'UPLOADED/TRANSCODING/TRANSCODED/AUDITING/HUMAN_AUDIT/AUDITED/PUBLISHED/REJECTED/FAILED',
    fail_reason VARCHAR(255) NULL,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_video (video_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频处理任务';
