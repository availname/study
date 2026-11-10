package com.example.learn.ai;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.learn.interview.Question;
import com.example.learn.interview.QuestionMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Agent 工具集单测（stage7/06 落地的行为固定）：
 * 工具只读、身份服务端注入——mock 三层依赖（Mapper/JdbcTemplate/Redis）验证组装逻辑。
 */
class LearningToolsTest {

    private QuestionMapper questionMapper;
    private JdbcTemplate jdbcTemplate;
    private StringRedisTemplate redis;
    private LearningTools tools;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Question.class);
    }

    @BeforeEach
    void setUp() {
        questionMapper = mock(QuestionMapper.class);
        NamedParameterJdbcTemplateHolder holder = new NamedParameterJdbcTemplateHolder();
        redis = mock(StringRedisTemplate.class);
        tools = new LearningTools(questionMapper, holder.jdbc(), redis, 1L);
        jdbcTemplate = holder.template();
    }

    /** NamedParameterJdbcTemplate 是 final 无接口类——用真实实例包 mock 的底层 JdbcTemplate */
    static final class NamedParameterJdbcTemplateHolder {
        private final JdbcTemplate template = mock(JdbcTemplate.class);
        private final org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc =
                new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(template);

        JdbcTemplate template() { return template; }
        org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc() { return jdbc; }
    }

    @Test
    void 搜索题目_映射出题目卡片字段() {
        Question q = new Question();
        q.id = 7L;
        q.title = "synchronized 锁升级";
        q.category = "并发";
        q.difficulty = 2;
        when(questionMapper.selectList(any())).thenReturn(List.of(q));

        List<Map<String, Object>> result = tools.searchQuestions("锁");

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).get("id"));
        assertEquals("并发", result.get(0).get("category"));
        assertEquals(2, result.get(0).get("difficulty"));
    }

    @Test
    void 课程名额查询_剩余等于quota减enrolled() {
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("title", "Java 并发实战", "quota", 30, "enrolled", 12));

        Map<String, Object> v = tools.courseQuota(5L);

        assertEquals("Java 并发实战", v.get("课程"));
        assertEquals(18, v.get("剩余名额"));
    }

    @Test
    void 学习统计_三层依赖缺省值为0() {
        when(redis.execute(any(RedisCallback.class))).thenReturn(3L);   // 本月签到 bitCount
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(redis.opsForValue().get("lc:study:minutes:1")).thenReturn("42");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(7L);

        Map<String, Object> v = tools.studyStats();

        assertEquals(3L, v.get("本月签到天数"));
        assertEquals(42L, v.get("累计学习分钟"));
        assertEquals(7L, v.get("错题数"));
    }

    @Test
    void 学习统计_Redis无数据_不抛异常兜底为0() {
        when(redis.execute(any(RedisCallback.class))).thenReturn(null);
        when(redis.opsForValue()).thenReturn(mock(ValueOperations.class));
        when(redis.opsForValue().get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(null);

        Map<String, Object> v = tools.studyStats();

        assertEquals(0L, v.get("本月签到天数"));
        assertEquals(0L, v.get("累计学习分钟"));
        assertEquals(0L, v.get("错题数"));
    }
}
