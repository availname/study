# 01 · LLM 应用开发全景

## 一、LLM API 的两大核心能力

调用大模型 API 本质只有两类请求——**Chat（对话补全）和 Embedding（文本向量化）**：

```
① Chat Completion（对话补全）
   输入：消息数组 [{role: system, content: "你是审核员"},
                   {role: user,   content: "这段内容违规吗"}]
   输出：assistant 的回复文本（流式或一次性）
   用途：问答/审核/摘要/出题/代码生成……

② Embedding（文本向量化）
   输入：一段文本
   输出：一个高维向量（如 1536 维）——语义相近的文本向量距离近
   用途：语义搜索/RAG 检索/语义缓存（r-stage6/03 已讲）
```

## 二、OpenAI 兼容协议：事实标准

OpenAI 的 Chat API 格式已成为**行业通用协议**——国内外主流模型全部兼容：

| 模型 | base-url | 备注 |
|------|----------|------|
| OpenAI | `https://api.openai.com` | 原版 |
| **DeepSeek** | `https://api.deepseek.com` | 便宜、代码/中文强，国内首选 |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 阿里系 |
| Kimi | `https://api.moonshot.cn/v1` | 长文本 |
| Ollama（本地） | `http://localhost:11434` | 本地开源模型，零成本开发 |

**换模型 = 改 base-url 和 model 名**，代码零改动——这就是"兼容协议"的价值（类比：MySQL 协议兼容让你能随意换数据库）。

请求体长这样（curl 直观版）：

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer $LLM_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [
      {"role": "system",    "content": "你是论坛内容审核员，只输出 JSON：{\"pass\":true/false,\"reason\":\"...\"}"},
      {"role": "user",      "content": "审核这段话：..."}
    ],
    "temperature": 0.2,
    "stream": false
  }'
```

四个关键字段：

| 字段 | 作用 | 工程要点 |
|------|------|---------|
| `model` | 选哪个模型 | 不同模型能力/价格差数倍 |
| `messages` | 对话历史数组 | system 定角色、user 是输入、assistant 是历史回复 |
| `temperature` | 随机度（0~2） | 审核类调 0~0.3（稳定）；创意类 0.7+ |
| `stream` | 是否流式返回 | true 时服务端逐 token 推送（打字机效果） |

## 三、Token：LLM 世界的"计量单位"

**Token ≈ 文本碎片**（英文约 0.75 词/个，中文约 1~1.5 字/个）。API 按 token 双向计费（输入+输出）：

```
成本估算公式：一次调用成本 = (输入 tokens × 输入单价) + (输出 tokens × 输出单价)
DeepSeek-chat 示例价（2026）：输入 ~1 元/百万 token，输出 ~2 元/百万 token
```

**工程含义**（为什么后端要懂 token）：

1. **上下文窗口有限**（如 64K/128K token）——消息历史不能无限堆，要有裁剪策略
2. **成本治理**——长 system 提示词每次调用都重复计费；高频调用要缓存（r-stage6/03 语义缓存）
3. **限流按 token**——上游限的不是 QPS 是 TPM（每分钟 token 数），压测口径要对齐

## 四、LLM 的"性格缺陷"（工程化的原因）

| 缺陷 | 表现 | 工程对策 |
|------|------|---------|
| **幻觉** | 一本正经地编造不存在的事实 | RAG（给它参考资料）/ 结构化输出约束 / 人工复核 |
| **不稳定** | 同样输入两次输出可能不同 | temperature 调低；输出结构化（JSON schema） |
| **慢** | 生成是逐 token 的（秒~分钟级） | 流式输出（体验优化）+ 异步化（MQ，stage7/03） |
| **不可靠** | 偶发超时/限流/服务不可用 | 重试+降级+熔断（和调其他下游一样） |
| **贵** | 按 token 计费 | 语义缓存 / 短提示词 / 小模型分流 |

> **元认知**：LLM = 一个"能力很强但 5% 概率胡说、响应慢、按 token 计费、偶尔宕机"的下游服务。**你在 MQ/Redis/MySQL 教学里学的所有可靠性工程，全部适用于 AI 集成**——这就是为什么 AI 应用开发是后端工程师的主场。

## 五、Spring AI：Java 生态的 LLM 框架

类似 JDBC 统一数据库——**Spring AI 统一各家模型 API**：

```
spring-ai-starter-model-openai   ← OpenAI 兼容（DeepSeek/通义/Kimi/Ollama 全走它）
ChatClient                        ← 统一对话入口（同步 call / 流式 stream / 结构化 entity）
EmbeddingModel                    ← 统一向量化
VectorStore                       ← 向量库抽象（Redis/PgVector/Milvus 实现）
Advisors                          ← 请求拦截链（日志/RAG/记忆）
```

下一章用 Spring AI 把 learn-center 的审核消费者从"关键词模拟"升级为真实 LLM 审核。

## 面试问法

**Q: 后端如何集成 LLM 能力？**
A: 三层——①协议层：OpenAI 兼容协议已是事实标准，国内外主流模型全部兼容，换模型改 base-url 即可；②框架层：Spring AI/LangChain4j 统一 ChatClient/Embedding/VectorStore 抽象；③工程层：把 LLM 当"强能力但不可靠按 token 计费的下游"设计——流式输出优化体验、异步化+限流保护配额、结构化输出+RAG 抑制幻觉、降级熔断保可用、语义缓存降成本。

---

**下一篇**：[02-SpringAI与流式输出](02-SpringAI与流式输出.md)。
