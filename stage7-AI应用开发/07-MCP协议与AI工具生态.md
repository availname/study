# 07 · MCP 协议与 AI 工具生态

MCP（Model Context Protocol，Anthropic 2024 年开放的协议）= **AI 应用与外部工具/数据源之间的标准化接口**——被称为"AI 界的 USB-C"。

## 解决什么问题

没有 MCP 时：每个 AI 应用要为每个数据源写一遍定制集成（N×M 问题）：

```
N 个 AI 应用 × M 个数据源 = N×M 套定制集成
```

MCP 之后：数据源实现一次 **MCP Server**（暴露工具/资源/提示词），任意 MCP 客户端（Claude/Cursor/你的应用）即插即用（**N+M**）：

```
┌─ MCP Host（AI 应用，如 Claude Desktop / 你的 Java 应用）─┐
│         │ MCP 协议（stdio / SSE 传输）                    │
│  ┌──────▼──────┐  ┌──────────┐  ┌──────────────┐        │
│  │MCP Server A │  │MCP Server│  │ MCP Server C │        │
│  │(GitHub 工具)│  │(数据库)  │  │ (文件系统)   │        │
│  └─────────────┘  └──────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────┘
```

## MCP Server 提供三类能力

| 能力 | 说明 | 例子 |
|------|------|------|
| **Tools 工具** | 可被 LLM 调用的函数（Function Calling 的标准化版） | 查订单/发消息/执行 SQL |
| **Resources 资源** | 可被读取的数据（文件/数据库内容） | 教程 md 文件 |
| **Prompts 提示词** | 预定义的提示词模板 | "总结这个 PR"模板 |

## 与 Function Calling 的关系（易混）

- **Function Calling**：单个 AI 应用内的工具调用机制（模型 → 你的函数）
- **MCP**：把"工具提供"标准化成协议——**任何 MCP Server 的工具可以被任何 MCP 客户端使用**
- 类比：Function Calling = 你程序里的方法调用；MCP = 把方法发布成 REST API 供所有程序调用

## Java 生态：Spring AI MCP

Spring AI 提供 MCP Starter——**把你的 Spring Bean 方法一键发布为 MCP Server**：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

```java
@Service
public class LearningTools {

    @Tool(description = "查询学习平台某用户的积分与签到天数")
    public Map<String, Object> studyStats(Long userId) { ... }

    @Tool(description = "按关键词搜索面试题库")
    public List<Map<String, Object>> searchQuestions(String keyword) { ... }
}
```

发布后，Claude Desktop / Claude Code / 任何 MCP 客户端添加这个 server 地址，**AI 就能直接查你的学习平台数据**——不需要写一行对接代码。

**真实玩法（本工作区验证）**：把 learn-center 的学习数据工具发布为 MCP Server → 在 Claude Code 里直接问"我最近的学习数据怎么样"→ Claude 通过 MCP 调用你的工具查库回答。

## MCP 的工程要点

| 要点 | 说明 |
|------|------|
| **安全第一** | MCP Server 的工具会暴露给 AI——**只开放只读/低风险操作**；写操作必须加确认机制 |
| 工具描述质量 | description 是 LLM 选择工具的依据（同 Function Calling） |
| 传输方式 | stdio（本地子进程）/ SSE/Streamable HTTP（远程服务） |
| 版本管理 | 工具签名变更要有版本意识（客户端可能缓存） |

## 面试问法

**Q: MCP 是什么？解决什么问题？**
A: Model Context Protocol，Anthropic 开放的 AI 应用与外部工具/数据源的标准化协议（USB-C 类比）。三类能力：Tools（可调用函数）/Resources（可读数据）/Prompts（模板）。解决 N×M 集成问题→N+M：数据源实现一次 MCP Server，任意 MCP 客户端即插即用。与 Function Calling 关系：FC 是应用内机制，MCP 是跨应用标准化协议。Java 生态用 Spring AI 的 MCP starter 把 Bean 方法发布为工具。

---

**下一篇**：[08-评测与安全](08-AI安全与评测.md)——提示注入攻击与 AI 应用的安全底线。
