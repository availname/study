-- learn-center 建表（数据库 learn_center，需先创建：
--   docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS learn_center DEFAULT CHARSET utf8mb4")
CREATE TABLE IF NOT EXISTS t_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(32)  NOT NULL,
    password   VARCHAR(64)  NOT NULL,
    points     INT          NOT NULL DEFAULT 0 COMMENT '积分（签到/学习奖励）',
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS t_course (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(128) NOT NULL,
    teacher     VARCHAR(32)  NOT NULL,
    price       INT          NOT NULL DEFAULT 0 COMMENT '价格（分），0=免费',
    quota       INT          NOT NULL DEFAULT 100 COMMENT '限量名额（秒杀库存）',
    enrolled    INT          NOT NULL DEFAULT 0 COMMENT '已报名人数',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_title_teacher (title, teacher)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

INSERT IGNORE INTO t_course (id, title, teacher, price, quota, enrolled, status) VALUES
(1, 'Java 并发编程实战',   '老王', 9900, 30,  0, 1),
(2, 'JVM 虚拟机深度剖析',  '老李', 9900, 30,  0, 1),
(3, 'MySQL 索引与事务',    '老张', 9900, 30,  0, 1),
(4, 'Redis 高并发架构',    '老赵', 9900, 30,  0, 1),
(5, 'RabbitMQ 消息队列',   '老钱', 0,    30,  0, 1);

CREATE TABLE IF NOT EXISTS t_enroll_order (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no   VARCHAR(40)  NOT NULL,
    user_id    BIGINT       NOT NULL,
    course_id  BIGINT       NOT NULL,
    status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已取消',
    price      INT          NOT NULL DEFAULT 0,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    paid_at    DATETIME(3)  NULL,
    cancel_at  DATETIME(3)  NULL,
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_user_course (user_id, course_id) COMMENT '一人一课（防重复购）',
    KEY idx_user (user_id),
    KEY idx_status_timeout (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢课订单（一人一课唯一索引=幂等兜底）';

CREATE TABLE IF NOT EXISTS t_question (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category    VARCHAR(16)  NOT NULL COMMENT '并发/JVM/MySQL/Redis/MQ/Spring/微服务',
    title       VARCHAR(255) NOT NULL,
    answer_md   TEXT         NOT NULL COMMENT '答题要点（Markdown）',
    difficulty  TINYINT      NOT NULL DEFAULT 1 COMMENT '1易 2中 3难',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题库（刷题模块）';

INSERT IGNORE INTO t_question (category, title, answer_md, difficulty) VALUES
('并发', '线程池核心参数与执行流程？', 'core/max/keepAlive/unit/workQueue/threadFactory/handler。流程：核心未满建核心→满则入队→队满建临时到max→拒绝。先排队后扩容，无界队列让max失效。', 2),
('并发', 'volatile 的作用？', '可见性(lock前缀刷主存)+有序性(内存屏障禁重排)，不保证原子性。DCL 单例必加。', 1),
('并发', 'CAS 是什么？ABA 怎么解决？', 'CPU原子指令比较+交换。ABA用 AtomicStampedReference 版本号；自旋耗CPU用 LongAdder 分段。', 2),
('JVM', 'G1 和 CMS 的区别？', 'CMS标记清除有碎片且并发失败退化Full GC；G1 Region化+停顿目标模型+无碎片。ZGC 亚毫秒停顿。', 2),
('JVM', 'CPU 100% 怎么排查？', 'top定进程→top -Hp定线程→TID转16进制→jstack看堆栈。Arthas thread -n 3 一步到位。', 1),
('MySQL', '为什么用 B+ 树？', '磁盘IO瓶颈：非叶子只存键树更矮(千万行3~4层)；叶子链表支持范围查询。', 2),
('MySQL', 'MySQL 有哪些锁？', '全局/表级(MDL/意向)/行级(记录锁/间隙锁/临键锁)。RR当前读用临键锁防幻读插入。', 3),
('Redis', 'Redis 为什么快？', '内存+命令单线程(无锁)+epoll多路复用+高效数据结构(SDS/跳表/listpack)。', 1),
('Redis', '穿透/击穿/雪崩的区别与解法？', '穿透→空值缓存+布隆过滤器；击穿→互斥锁/逻辑过期；雪崩→TTL抖动+多级缓存+高可用。', 2),
('MQ', '消息丢失怎么防？', '三端：生产Confirm+本地消息表；Broker持久化+副本；消费手动ACK+重试+死信。', 2),
('MQ', '消息重复怎么幂等？', '两层：Redis setnx快速去重+DB唯一索引兜底；幂等键用全局唯一业务ID；跳过打日志。', 2),
('Spring', '@Transactional 失效场景？', '自调用/非public/异常被吞/受检异常默认不回滚/多线程/传播误用/MyISAM/类不受管理。', 2),
('微服务', 'Sentinel 熔断三态？', '关闭→(失败率超阈值)→打开(直接拒绝)→熔断时长结束→半开(探测)→成功关闭/失败再开。', 2);

CREATE TABLE IF NOT EXISTS t_wrong_book (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT      NOT NULL,
    question_id    BIGINT      NOT NULL,
    wrong_count    INT         NOT NULL DEFAULT 1,
    last_wrong_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_question (user_id, question_id) COMMENT '错题本：同一题重复错只加次数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- 本地消息表（与 mq-mall/community 同款可靠性基建）
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
