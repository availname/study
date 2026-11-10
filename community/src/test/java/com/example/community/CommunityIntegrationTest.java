package com.example.community;

import com.example.community.messaging.SendRecordDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * stage6/07 真容器集成测试：测"踩过坑的关键链路"，不是 mock。
 * 需要 Docker 引擎运行（Testcontainers 自动起临时 MySQL/Redis 容器，测完自动销毁）。
 *
 * 三个回归用例分别锁住三个真实踩过的坑：
 * ① SETBIT 旧值语义（坑：判断写反导致首签显示"已签过"）
 * ② 本地消息表基建可用（坑三：insertPending 被遗漏，可靠性基建形同虚设）
 * ③ 幂等两层防线（坑二/坑九：Redis 残留键误杀 + DB 兜底）
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)   // 无 Docker 环境自动跳过（CI/有Docker时才真跑）
class CommunityIntegrationTest {

    @Container
    @ServiceConnection                                     // 自动把容器地址注入 datasource 配置
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withInitScript("sql/schema.sql");             // 容器内自动建表

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @org.springframework.test.context.DynamicPropertySource
    static void redisProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired StringRedisTemplate redis;
    @Autowired SendRecordDao sendRecordDao;
    @Autowired org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;

    // ===== 回归①：SETBIT 旧值语义（首签返回 false，重复签返回 true）=====
    @Test
    void setBit_firstCallReturnsFalse_repeatReturnsTrue() {
        String key = "test:sign:it:" + System.currentTimeMillis();
        boolean first = redis.opsForValue().setBit(key, 4, true);
        assertFalse(first, "首次 SETBIT 应返回旧值 false（首签）");
        boolean second = redis.opsForValue().setBit(key, 4, true);
        assertTrue(second, "重复 SETBIT 应返回旧值 true（已签过）——判断写反的坑由本用例锁住");
        redis.delete(key);
    }

    // ===== 回归②：本地消息表基建可用（insert→扫描→确认→不再扫描）=====
    @Test
    void sendRecord_lifecycle_insert_scan_confirm() {
        String messageId = "it-" + System.currentTimeMillis();
        sendRecordDao.insertPending(messageId, "community.post", "post.submitted", "{\"x\":1}");

        List<Map<String, Object>> due = sendRecordDao.findPendingDue(100);
        assertTrue(due.stream().anyMatch(r -> messageId.equals(r.get("message_id"))),
                "待确认消息必须出现在补偿扫描结果中（坑三的回归测试）");

        sendRecordDao.markConfirmed(messageId);
        List<Map<String, Object>> after = sendRecordDao.findPendingDue(100);
        assertTrue(after.stream().noneMatch(r -> messageId.equals(r.get("message_id"))),
                "Confirm 后不应再被补偿扫描到");
    }

    // ===== 回归③：幂等两层防线（Redis setnx 第一道 + DB 唯一键第二道）=====
    @Test
    void dedup_redisFirst_dbFallback() {
        String mid = "it-dedup-" + System.currentTimeMillis();
        // 第一道：Redis setnx
        Boolean first = redis.opsForValue().setIfAbsent("mq:dedup:" + mid, "1", Duration.ofMinutes(5));
        assertTrue(Boolean.TRUE.equals(first), "第一次 setnx 应成功");
        // 第二道：DB 唯一键
        assertDoesNotThrow(() -> jdbc.update(
                "INSERT INTO t_mq_consume_record (message_id, consumer) VALUES (:mid, 'it')",
                new MapSqlParameterSource().addValue("mid", mid)));
        // 重复消费场景：Redis 键存在 → tryConsume 语义为"跳过"；DB 重复插入报 DuplicateKey
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO t_mq_consume_record (message_id, consumer) VALUES (:mid, 'it')",
                new MapSqlParameterSource().addValue("mid", mid)));
        redis.delete("mq:dedup:" + mid);
    }
}
