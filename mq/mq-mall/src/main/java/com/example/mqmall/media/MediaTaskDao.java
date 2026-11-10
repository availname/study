package com.example.mqmall.media;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 视频处理任务表读写。状态推进一律 CAS（WHERE status=期望值），与订单场景同一套状态机纪律 */
@Repository
public class MediaTaskDao {

    /** 状态常量（与 t_media_task.status 列一致） */
    public static final String UPLOADED = "UPLOADED";
    public static final String TRANSCODING = "TRANSCODING";
    public static final String TRANSCODED = "TRANSCODED";
    public static final String AUDITING = "AUDITING";
    public static final String HUMAN_AUDIT = "HUMAN_AUDIT";
    public static final String AUDITED = "AUDITED";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String REJECTED = "REJECTED";
    public static final String FAILED = "FAILED";

    private final NamedParameterJdbcTemplate jdbc;

    public MediaTaskDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TaskRow(String videoId, String title, String status, String failReason) {}

    public void insert(String videoId, String title) {
        jdbc.update("""
                INSERT INTO t_media_task (video_id, title, status) VALUES (:vid, :title, 'UPLOADED')
                """, new MapSqlParameterSource().addValue("vid", videoId).addValue("title", title));
    }

    public Optional<TaskRow> find(String videoId) {
        return jdbc.query("""
                        SELECT video_id, title, status, fail_reason FROM t_media_task WHERE video_id = :vid
                        """, new MapSqlParameterSource("vid", videoId),
                rs -> {
                    if (rs.next()) {
                        return Optional.of(new TaskRow(rs.getString("video_id"),
                                rs.getString("title"), rs.getString("status"), rs.getString("fail_reason")));
                    }
                    return Optional.empty();
                });
    }

    /** CAS 状态推进：只有当前状态等于期望值才允许流转（重复消息/乱序消息天然被挡住） */
    public int casStatus(String videoId, String expect, String next) {
        return jdbc.update("""
                UPDATE t_media_task SET status = :next WHERE video_id = :vid AND status = :expect
                """, new MapSqlParameterSource()
                .addValue("vid", videoId).addValue("expect", expect).addValue("next", next));
    }

    public void markFailed(String videoId, String reason) {
        jdbc.update("""
                UPDATE t_media_task SET status = 'FAILED', fail_reason = :reason WHERE video_id = :vid
                """, new MapSqlParameterSource()
                .addValue("vid", videoId).addValue("reason", reason));
    }

    /** 失败/卡死任务重新入管道：FAILED 或 UPLOADED（消息丢失卡死）→ UPLOADED */
    public int resetForRetry(String videoId) {
        return jdbc.update("""
                UPDATE t_media_task SET status = 'UPLOADED', fail_reason = NULL
                WHERE video_id = :vid AND status IN ('FAILED', 'UPLOADED')
                """, new MapSqlParameterSource("vid", videoId));
    }

    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.queryForList("SELECT status, COUNT(*) c FROM t_media_task GROUP BY status",
                        new MapSqlParameterSource())
                .forEach(row -> result.put((String) row.get("status"), ((Number) row.get("c")).longValue()));
        return result;
    }
}
