package com.bigcenter.ai;

import com.bigcenter.auth.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 学习报告（learn-center AiService.report 总装版）：
 * 聚合平台真实数据（秒杀订单/发帖数/点赞数/积分——数字由系统提供），
 * AI 只做个性化表述（措辞由模型生成）——防幻觉的黄金分工。
 */
@Service
public class AiReportService {

    private final ChatPort chatPort;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public AiReportService(ChatPort chatPort, JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.chatPort = chatPort;
        this.jdbc = jdbc;
        this.redis = redis;
    }

    public String report(long userId) {
        long seckillCount = count("SELECT COUNT(*) FROM t_seckill_order WHERE user_id = ? AND status IN (0,1)", userId);
        long postCount = count("SELECT COUNT(*) FROM t_post WHERE author_id = ? AND status = 1", userId);
        long points = count("SELECT COALESCE(SUM(points),0) FROM t_points_log WHERE user_id = ?", userId);
        long likeGiven = likeGiven(userId);

        String prompt = """
                根据以下学习数据写一段 150 字左右的个性化学习建议（中文，友善鼓励语气，给出具体可执行建议）：
                - 抢到课程 %d 门
                - 过审帖子 %d 篇
                - 累计积分 %d 分
                - 发出点赞 %d 次
                """.formatted(seckillCount, postCount, points, likeGiven);
        return chatPort.ask("你是开发者成长平台的学习顾问，根据数据给出具体、可执行的建议", prompt);
    }

    private long count(String sql, long userId) {
        Long c = jdbc.queryForObject(sql, Long.class, userId);
        return c == null ? 0 : c;
    }

    /** 发出点赞数：按明细 delta 聚合（含取消的 -1，抵消后是"净点赞数"） */
    private long likeGiven(long userId) {
        Long c = jdbc.queryForObject(
                "SELECT COALESCE(SUM(delta),0) FROM t_like_archive WHERE user_id = ?", Long.class, userId);
        return c == null ? 0 : c;
    }
}
