# Stage 7 · AI 应用开发（Java 后端视角）

> 把 LLM（大语言模型）能力接入后端系统，是 2024 年后后端工程师的**新必修课**。本阶段用 **Spring AI** 把 AI 能力真实落进 learn-center：AI 答疑、AI 出题、学习报告——全部代码在项目里，无 API Key 也能跑通全流程（Mock 降级）。

## 前置认知：LLM 对后端工程师意味着什么

| 你以为的 | 实际的 |
|---------|--------|
| AI = 算法工程师的领域 | **调用 API 的工程问题**——和调 Redis/发 MQ 一样，是后端日常 |
| 要懂模型训练 | 调用方不需要——你只需要懂 **API 协议、提示词、工程化**（成本/限流/降级/评测） |
| Python 专属 | Java 生态已成熟（Spring AI 1.0 GA / LangChain4j），生产可用 |

**后端工程师在 AI 时代的角色变化**：从"写业务 CRUD"到"**编排 AI 能力**"——把 LLM 当成一个"很聪明但不可靠、按 token 收费、会胡说"的下游服务来设计系统。这个认知贯穿本阶段全部篇目。

## 篇目

| 篇 | 主题 | 核心考点 |
|----|------|---------|
| [01](01-LLM应用开发全景.md) | LLM API 全景 | Chat/Embedding 两大 API / OpenAI 兼容协议 / **Token 与成本** / 国内模型接入 |
| [02](02-SpringAI与流式输出.md) | Spring AI + 流式 | ChatClient / **SSE 流式输出** / 结构化输出（JSON）/ ChatPort 策略抽象 |
| [03](03-RAG与向量检索.md) | RAG 知识库问答 | Embedding / 分块 / 检索增强 / **Redis 向量检索**（呼应 r-stage6/03） |
| [04](04-AI工程化与learn-center落地.md) | AI 工程化 | 提示词工程 / **降级与熔断** / 成本治理 / 评测 / learn-center 三功能落地 |
| [05](05-RAG进阶六种升级.md) | RAG 进阶 | 查询改写(HyDE) / 混合检索(BM25+向量) / **重排序** / 父子分块 / GraphRAG / Agentic RAG / 评测集 |
| [06](06-AI工作流与Agent.md) | AI 工作流与 Agent | **五种工作流模式**（提示链/路由/并行/编排者/评估者）/ Tool Calling 循环 / 三底线 / 什么时候不用 Agent |
| [07](07-MCP协议与AI工具生态.md) | MCP 协议 | Tools/Resources/Prompts 三能力 / N×M→N+M / Spring AI MCP Server |
| [08](08-AI安全与评测.md) | AI 安全 | **提示注入**（直接+间接）/ 架构层权限最小化 / 数据合规 / LLM as Judge 评测 |
| [09](09-AI微调与选型.md) | 微调与选型 | **RAG vs 微调决策树** / SFT 流程 / **LoRA 低秩旁路原理** / QLoRA / OpenAI 兼容端点接入 |
| [10](10-多模态应用.md) | 多模态 | Spring AI 发图 / **万物皆 token**（ViT patch）/ 截图报障·拍题答疑·图文 RAG / 图片提示注入防御 |

## 学习路线

```
01 全景（半天）→ 02 Spring AI 流式（一天，含代码）→ 03 RAG（一天）
→ 04 工程化 + learn-center 落地（两天，AI 答疑/出题/报告三功能）
```

## 环境与成本说明（重要）

- **模型**：任选 OpenAI 兼容端点——DeepSeek（`https://api.deepseek.com`，便宜效果好）/ 通义 / Kimi；本地 Ollama 也行（`http://localhost:11434`）
- **API Key**：各平台注册即送额度；`export LLM_API_KEY=sk-xxx` 环境变量注入，**不进代码不进 Git**
- **没有 Key？** learn-center 的 AI 模块带 **Mock 模式**（`lc.ai.provider=mock`）——管道、SSE、降级全流程照跑，只是回复是预设文本。**教学重点是工程化，不是模型本身**
