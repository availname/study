-- big-center 建表（数据库 big_center，需先创建：
--   docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS big_center DEFAULT CHARSET utf8mb4")
-- 骨架阶段仅基础表；各功能表随实现步骤追加（短链/红包/支付/点赞/推送/扫码）

CREATE TABLE IF NOT EXISTS t_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 摘要（java-core/19）',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户（JWT 登录，第 2 步启用）';

CREATE TABLE IF NOT EXISTS t_short_link (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code  VARCHAR(16)  NOT NULL,
    long_url    VARCHAR(2048) NOT NULL,
    creator_id  BIGINT       NULL COMMENT '发号人（登录态 userId）',
    click_count BIGINT       NOT NULL DEFAULT 0 COMMENT '点击数（Redis 累积阈值回写）',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_short_code (short_code),
    UNIQUE KEY uk_long_url (long_url) COMMENT '同一长 URL 防重复发号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链映射（java-core/12 题一）';

CREATE TABLE IF NOT EXISTS t_pay_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_order_no VARCHAR(64)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    channel_code VARCHAR(16)  NOT NULL COMMENT '渠道码（策略路由）',
    amount_fen   BIGINT       NOT NULL COMMENT '金额单位：分（钱上不允许 double）',
    subject      VARCHAR(128) NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 10 COMMENT '10待支付 20已支付 30已关闭 40已退款（状态机）',
    paid_at      DATETIME(3)  NULL,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_pay_order_no (pay_order_no),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单（java-core/15 四关卡）';

CREATE TABLE IF NOT EXISTS t_pay_callback (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_trade_no VARCHAR(64)  NOT NULL COMMENT '渠道流水号——唯一索引是回调幂等第一层',
    pay_order_no     VARCHAR(64)  NOT NULL,
    amount_fen       BIGINT       NOT NULL,
    status           TINYINT      NOT NULL,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_channel_trade (channel_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调流水（幂等第一层，T+1 对账平台侧数据源）';

CREATE TABLE IF NOT EXISTS t_red_pack (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id      BIGINT      NOT NULL,
    total_fen     BIGINT      NOT NULL,
    count         INT         NOT NULL COMMENT '份数',
    grabbed_count INT         NOT NULL DEFAULT 0,
    status        TINYINT     NOT NULL DEFAULT 10 COMMENT '10进行中 20已抢完 30退款中 40已退款',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红包主体（java-core/14 题二）';

CREATE TABLE IF NOT EXISTS t_red_pack_detail (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    pack_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    amount_fen BIGINT      NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_pack_user (pack_id, user_id) COMMENT 'MQ 重复消费幂等',
    KEY idx_pack (pack_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢红包明细（MQ 异步落库）';
