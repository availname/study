package com.example.community.post;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 帖子表读写 + 状态机 CAS（0待审 1通过 2拒绝 3待人工） */
@Repository
public class PostDao {

    public static final int PENDING = 0;
    public static final int APPROVED = 1;
    public static final int REJECTED = 2;
    public static final int HUMAN_AUDIT = 3;

    private final NamedParameterJdbcTemplate jdbc;

    public PostDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PostRow(long id, String title, String content, long authorId,
                          int status, String rejectReason, long viewCount, int likeCount) {}

    public long insert(long authorId, String title, String content) {
        jdbc.update("""
                INSERT INTO t_post (title, content, author_id, status) VALUES (:t, :c, :a, 0)
                """, new MapSqlParameterSource()
                .addValue("t", title).addValue("c", content).addValue("a", authorId));
        Long id = jdbc.getJdbcTemplate().queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0 : id;
    }

    public Optional<PostRow> find(long id) {
        return jdbc.query("""
                        SELECT id, title, content, author_id, status, reject_reason, view_count, like_count
                        FROM t_post WHERE id = :id
                        """, new MapSqlParameterSource("id", id),
                rs -> {
                    if (rs.next()) {
                        return Optional.of(new PostRow(rs.getLong("id"), rs.getString("title"),
                                rs.getString("content"), rs.getLong("author_id"), rs.getInt("status"),
                                rs.getString("reject_reason"), rs.getLong("view_count"), rs.getInt("like_count")));
                    }
                    return Optional.empty();
                });
    }

    /** CAS 状态推进 */
    public int casStatus(long postId, int expect, int next, String reason) {
        return jdbc.update("""
                UPDATE t_post SET status = :next, reject_reason = :reason WHERE id = :id AND status = :expect
                """, new MapSqlParameterSource()
                .addValue("id", postId).addValue("expect", expect)
                .addValue("next", next).addValue("reason", reason));
    }

    public List<Map<String, Object>> listByStatus(int status, int limit) {
        return jdbc.queryForList("""
                SELECT id, title, author_id, status, created_at FROM t_post
                WHERE status = :st ORDER BY id DESC LIMIT :lim
                """, new MapSqlParameterSource().addValue("st", status).addValue("lim", limit));
    }

    public List<Map<String, Object>> listByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.getJdbcTemplate().queryForList(
                "SELECT id, title, author_id, view_count, like_count FROM t_post WHERE id IN (" +
                        ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + ")");
    }

    /** CounterFlushJob 刷 DB 底账 */
    public void flushCounters(long id, long viewCount, int likeCount) {
        jdbc.update("""
                UPDATE t_post SET view_count = GREATEST(view_count, :vc), like_count = :lc WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("vc", viewCount).addValue("lc", likeCount));
    }
}
