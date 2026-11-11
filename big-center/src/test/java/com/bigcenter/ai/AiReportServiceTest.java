package com.bigcenter.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 报告验收（learn-center AiService 语义钉子）：
 * 数字由系统聚合（真实 SQL），措辞由 ChatPort 生成——数据进 prompt 的分工。
 */
class AiReportServiceTest {

    private ChatPort chatPort;
    private JdbcTemplate jdbc;
    private AiReportService service;

    @BeforeEach
    void setUp() {
        chatPort = Mockito.mock(ChatPort.class);
        jdbc = Mockito.mock(JdbcTemplate.class);
        service = new AiReportService(chatPort, jdbc, Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class));
    }

    @Test
    void 报告_聚合四项数据_并交给ChatPort生成表述() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(3L, 5L, 50L, 12L);
        when(chatPort.ask(anyString(), anyString())).thenReturn("【Mock 报告】继续加油！");

        String report = service.report(1001L);
        assertEquals("【Mock 报告】继续加油！", report);
        // 数据进 prompt：四项数字必须出现在给模型的用户提示里（事实系统给，措辞 AI 编）
        verify(chatPort).ask(contains("学习顾问"), contains(
                "抢到课程 3 门"));
    }

    @Test
    void 报告_SQL返回null_按0兜底不抛NPE() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);
        when(chatPort.ask(anyString(), anyString())).thenReturn("空数据报告");
        assertEquals("空数据报告", service.report(1001L));
    }

    @Test
    void 报告_积分用SUM聚合() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        service.report(1001L);
        verify(jdbc).queryForObject(contains("SUM(points)"), eq(Long.class), any(Object[].class));
    }
}
