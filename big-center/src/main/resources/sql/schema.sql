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
