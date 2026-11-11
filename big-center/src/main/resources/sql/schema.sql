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
