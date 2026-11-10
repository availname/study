package com.example.learn.ai;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.learn.common.BizException;
import com.example.learn.interview.Question;
import com.example.learn.interview.QuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 出题服务单测：mock ChatPort（模拟 LLM 返回）+ mock QuestionMapper。
 * 覆盖：JSON 围栏剥离 / 去重 / 难度钳制 / 脏数据跳过 / 解析失败兜底。
 * （ChatPort 策略模式的收益：业务逻辑测试不依赖真实模型——stage7/04 工程化的落地）
 */
class AiQuestionGeneratorTest {

    private ChatPort chatPort;
    private QuestionMapper questionMapper;
    private AiQuestionGenerator generator;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 解析 Question::getTitle 需要实体元数据（MyBatis 运行时 normally 注册）
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Question.class);
    }

    @BeforeEach
    void setUp() {
        chatPort = mock(ChatPort.class);
        questionMapper = mock(QuestionMapper.class);
        generator = new AiQuestionGenerator(chatPort, new ObjectMapper(), questionMapper);
    }

    @Test
    void 解析数组并入库_难度钳制到1到3() {
        when(chatPort.ask(any(), any())).thenReturn("""
                [{"title":"synchronized 锁升级","answerMd":"偏向→轻量→重量","difficulty":99},
                 {"title":"volatile 可见性","answerMd":"MESI","difficulty":0}]
                """);

        int inserted = generator.generate("并发", 2);

        assertEquals(2, inserted);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper, times(2)).insert(captor.capture());
        List<Question> saved = captor.getAllValues();
        assertEquals("synchronized 锁升级", saved.get(0).title);
        assertEquals(3, saved.get(0).difficulty);       // 99 → 3
        assertEquals(1, saved.get(1).difficulty);        // 0 → 1
        assertEquals("并发", saved.get(0).category);
    }

    @Test
    void 剥离markdown代码围栏后再解析() {
        when(chatPort.ask(any(), any())).thenReturn("""
                ```json
                [{"title":"TCP 三次握手","answerMd":"SYN/SYN+ACK/ACK","difficulty":2}]
                ```
                """);

        assertEquals(1, generator.generate("网络", 1));
        verify(questionMapper).insert(ArgumentMatchers.<Question>any());
    }

    @Test
    void 已存在的标题去重跳过() {
        when(chatPort.ask(any(), any())).thenReturn("""
                [{"title":"已有题","answerMd":"","difficulty":2},
                 {"title":"新题","answerMd":"","difficulty":2}]
                """);
        when(questionMapper.selectCount(any())).thenReturn(1L, 0L);   // "已有题"存在，"新题"不存在

        assertEquals(1, generator.generate("并发", 2));           // 只有"新题"入库
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionMapper).insert(captor.capture());
        assertEquals("新题", captor.getValue().title);
    }

    @Test
    void 无标题的脏元素跳过() {
        when(chatPort.ask(any(), any())).thenReturn("""
                [{"answerMd":"缺标题"},
                 {"title":"","answerMd":"空标题"},
                 {"title":"有效题","answerMd":"","difficulty":2}]
                """);

        assertEquals(1, generator.generate("并发", 3));
    }

    @Test
    void 非JSON输出抛业务异常() {
        when(chatPort.ask(any(), any())).thenReturn("抱歉，我无法生成题目。");
        assertThrows(BizException.class, () -> generator.generate("并发", 1));
        verify(questionMapper, never()).insert(ArgumentMatchers.<Question>any());
    }

    @Test
    void 非数组JSON抛业务异常() {
        when(chatPort.ask(any(), any())).thenReturn("{\"title\":\"单个对象不是数组\"}");
        assertThrows(BizException.class, () -> generator.generate("并发", 1));
    }
}
