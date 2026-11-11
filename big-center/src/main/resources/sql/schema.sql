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

CREATE TABLE IF NOT EXISTS t_push_message (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息 ID 单调递增——位点差值算未读',
    to_user_id BIGINT      NOT NULL,
    from_user_id BIGINT    NOT NULL,
    title      VARCHAR(128) NOT NULL,
    content    VARCHAR(1024) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_to_user (to_user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息（离线落库+拉取兜底；公告走读扩散不落表）';

CREATE TABLE IF NOT EXISTS t_course (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(128) NOT NULL,
    quota      INT          NOT NULL COMMENT '总名额',
    enrolled   INT          NOT NULL DEFAULT 0 COMMENT '已报名数（DB 兜底防线）',
    price_fen  BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限量课（秒杀四层漏斗）';

CREATE TABLE IF NOT EXISTS t_seckill_order (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no   VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    course_id  BIGINT      NOT NULL,
    status     TINYINT     NOT NULL DEFAULT 0 COMMENT '0已抢 1已支付 2已取消',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_course (user_id, course_id) COMMENT 'MQ 重复消费幂等+一人一课 DB 兜底'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单（MQ 异步建单）';

CREATE TABLE IF NOT EXISTS t_post (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(128) NOT NULL,
    content       VARCHAR(4096) NOT NULL,
    author_id     BIGINT      NOT NULL,
    status        TINYINT     NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2拒绝 3待人工（审核状态机）',
    reject_reason VARCHAR(256) NULL,
    view_count    BIGINT      NOT NULL DEFAULT 0,
    like_count    INT         NOT NULL DEFAULT 0,
    heat          DOUBLE      NOT NULL DEFAULT 0 COMMENT '热度分（ZSet 镜像，热榜衰减基准）',
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_status (status),
    KEY idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子（审核管道+热榜，community 模式总装）';

CREATE TABLE IF NOT EXISTS t_points_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    points     INT         NOT NULL,
    reason     VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL COMMENT '唯一索引幂等（两层防线 DB 层）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水（审核通过发奖，唯一索引幂等）';

CREATE TABLE IF NOT EXISTS t_like_archive (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    delta      INT         NOT NULL COMMENT '+1/-1 归档增量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_user (user_id),
    KEY idx_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞归档（MQ 异步刷库的落点，AI 报告数据源）';
