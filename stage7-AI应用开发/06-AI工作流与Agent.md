# 06 · AI 工作流与 Agent：五种模式 + learn-center 落地

Anthropic 的《Building Effective Agents》把 LLM 应用模式分为**五种工作流 + 一种 Agent**。核心结论（背下来）：**能用工作流解决的，不要上 Agent——工作流可预测、便宜、好调试；Agent 只在真正需要开放式自主决策时用**。

## 五种工作流模式（复杂度递增）

### ① 提示链 Prompt Chaining：串行分解

```
任务 → LLM① → LLM② → LLM③ → 结果
```

每步以下一步为输入，中间可加"程序校验门"（不合格重来或终止）。
**例**：AI 出题流水线——生成题干 → 自检（表述清晰吗/有唯一答案吗）→ 修复 → 入库。拆成三步比"一步生成完美题"成功率高得多。

**适用判据**：任务能干净地分解为固定子步骤——**用准确率换延迟，值**。

### ② 路由 Routing：先分类再分流

```
输入 → LLM 分类 → ┬→ 账单类处理器
                   ├→ 技术类处理器
                   └→ 其他 → 人工
```

**例**：客服系统先让 LLM 判断问题类型（退款/技术/咨询），分别走专门的提示词与知识库——每个分支的提示词可以短小精准。

### ③ 并行化 Parallelization：同时开工再汇总

```
输入 ─┬→ LLM①（并行处理）─┬→ 汇总器
      ├→ LLM②（并行处理）─┤
      └→ LLM③（并行处理）─┘
```

两个用途：
- **分区**：一个问题拆多方面并行（代码评审：并行检查安全/性能/风格）
- **投票**：同一任务跑多次取多数/取最优——提升准确率（对抗随机性）

### ④ 编排者-工作者 Orchestrator-Workers

中央 LLM 动态**拆解任务分派**给工作者 LLM，再汇总——与③的区别：**子任务不是预定义的，由编排者动态决定**（如"重构这个文件里所有复杂函数"——哪些函数复杂由编排者现场判断）。

### ⑤ 评估者-优化者 Evaluator-Optimizer

```
生成 LLM → 输出 → 评估 LLM 打分/反馈 → 不合格带反馈重新生成 →（循环直到合格或达上限）
```

例：文学翻译（评估者检查语义与语气，反馈具体修改意见）。**有明确评分标准的迭代任务适用**。

## Agent：LLM + 工具的自主循环

与前五种的本质区别：**Agent 由 LLM 自己决定下一步做什么**（调用哪个工具、要不要继续），而非人写死流程：

```
循环：
  ① LLM 看当前状态（用户请求 + 历史工具结果）
  ② 决定：调用某个工具（Function Calling）？还是输出最终答案？
  ③ 执行工具 → 结果回填上下文 → 回到①
```

**工具 = 你注册给 LLM 的函数**——LLM 输出"调用哪个函数+参数"，**你的代码执行它**并把结果回填。这就是 Function Calling / Tool Use。

## learn-center 落地：学习助手 Agent（真实 Tool Calling）

三个工具（@Tool 注解注册——Spring AI 1.0 原生支持）：

```java
public class LearningTools {

    @Tool(description = "按关键词搜索题库，返回匹配的面试题列表")
    public List<Map<String, Object>> searchQuestions(
            @ToolParam(description = "搜索关键词") String keyword) {
        // 真实查 MySQL 题库
    }

    @Tool(description = "查询指定用户的学习统计（签到天数/学习时长/错题数/抢课数）")
    public Map<String, Object> studyStats(@ToolParam(description = "用户ID") Long userId) { ... }

    @Tool(description = "查询某课程的名额与已报名人数")
    public Map<String, Object> courseQuota(@ToolParam(description = "课程ID") Long courseId) { ... }
}
```

Agent 循环（Spring AI 自动驱动——模型发 tool_call，框架执行工具回填，循环直到模型给出最终答案）：

```java
@GetMapping(value = "/agent/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> agentChat(@RequestParam String q) {
    return chatClient.prompt()
            .user(q)
            .tools(new LearningTools())          // ★ 注册工具，Agent 循环自动驱动
            .stream()
            .content()
            .map(token -> ServerSentEvent.builder(token).build());
}
```

**用户问“我错题最多的分类是什么”→ Agent 自主决定**：调用 studyStats 工具查数据 → 组织回答。**工具的 description 是 LLM 决策的依据——写得越清楚，选择越准**（工具描述 = 给 AI 看的 API 文档）。

## 什么时候不用 Agent（Anthropic 原文的核心忠告）

| Agent 的代价 | 说明 |
|--------------|------|
| **不可预测** | LLM 自主决策——可能绕路、过度调用工具、烧 token |
| 难调试 | 决策链在模型内部 |
| 错误会放大 | 一步选错工具，后续全偏 |

**决策表**：

```
流程可以预先写死？→ 用工作流（①~⑤）——可预测、便宜、好调试
需要"开放式的多步决策"、无法预定义路径？→ Agent
绝大多数生产场景 → 固定工作流 + 局部 Agent（混合：主流程写死，个别节点给 Agent 自主权）
```

## 生产安全护栏（Agent 上线的三条底线）

1. **工具白名单 + 权限最小化**：给 Agent 的工具只开放只读/必要操作（**绝不给删除/支付类工具**）
2. **调用上限**：单次会话工具调用次数封顶（防死循环烧 token）
3. **人工确认点**：高危操作（退款/删数据）工具执行前要求人工确认（human-in-the-loop）

## 面试问法

**Q: AI Agent 和普通 LLM 调用的区别？什么时候用 Agent？**
A: Agent = LLM + 工具 + 自主循环：模型自己决定调用哪个工具、是否继续，而非人写死流程（Function Calling 协议：模型输出工具名+参数，应用执行后回填结果循环）。工作流五模式（提示链/路由/并行/编排者/评估者）覆盖可预定义流程；Agent 只用于开放式多步决策。核心忠告（Anthropic）：能用工作流不用 Agent——可预测、便宜、好调试；生产三底线：工具白名单、调用上限、高危操作人工确认。

---

**下一篇**：[07-MCP协议与AI工具生态](07-MCP协议与AI工具生态.md)——工具共享的标准化协议。
