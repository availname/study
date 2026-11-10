# 03 · Redis 向量检索与语义缓存：LLM 时代的 Redis 新角色

Redis 不只是缓存了。加一个搜索模块后，它能存**向量**（Embedding）并做相似度搜索——这让它成为 AI 应用里最顺手的向量数据库和**语义缓存**载体。

## 先弄懂：Embedding（嵌入向量）是什么

把文本喂给 Embedding 模型（如 text-embedding-3），输出一个高维数字向量（如 1536 维），**语义相近的文本向量距离相近**：

```
"怎么重置密码"   → [0.12, -0.38, 0.55, ...]
"密码忘了怎么办" → [0.11, -0.36, 0.52, ...]   ← 距离非常近
"今天天气真好"   → [-0.72, 0.91, -0.14, ...]  ← 距离很远
```

**魔法所在**：比较语义相似度 = 比较向量距离（余弦相似度）。模糊匹配、语义搜索、RAG 全建立在这一点上。

## Redis 当向量库：KNN 就是"最相似的 K 条"

向量检索 = 给一个查询向量，在库里找**距离最近的 K 条**（KNN）。Redis 的 RediSearch 模块（或 Redis 8 内置向量集）用 HNSW 索引实现毫秒级近似检索：

```bash
# redis-stack 镜像自带 RediSearch（教学推荐换用它，命令一致）
docker run -d --name redis-stack -p 6381:6379 redis/redis-stack:latest

# 建向量索引（HNSW 算法，余弦距离）
FT.CREATE idx:kb ON HASH PREFIX 1 kb: SCHEMA
    content TEXT
    embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE

# 写入（embedding 由模型生成，BYTEFLOAT 二进制塞入）
HSET kb:1 content "重置密码请到个人安全设置" embedding <向量字节>
HSET kb:2 content "忘记密码可以通过绑定手机找回" embedding <向量字节>

# 语义搜索："密码忘了咋整" → 向量化 → KNN 最近 2 条
FT.SEARCH idx:kb "*=>[KNN 2 @embedding $vec]" PARAMS 2 vec <查询向量字节>
```

**为什么用 Redis 而不是专用向量库（Milvus/Pinecone/pgvector）**：

| 规模 | 建议 |
|------|------|
| 千万级以下向量、已有 Redis 基础设施 | **Redis 足够**（HNSW + 低延迟 + 免新组件） |
| 亿级向量、复杂过滤 | Milvus / pgvector（Postgres） |
| Redis 方案的最大红利：**向量与业务数据同库**，一次事务写入，一次连接读取 |

## 语义缓存（Semantic Cache）：LLM 应用的省钱神器

传统缓存：key 完全相等才命中——"密码忘了怎么办"和"密码忘了咋整"是**两个不同的 key**，各查一次 LLM（双倍花钱）。

语义缓存：**把问题向量化，找最相似的已缓存问题，相似度超阈值就直接返回缓存答案**：

```
用户问: "密码忘了咋整"
  → 向量化 → 在语义缓存里 KNN
  → 找到 "密码忘了怎么办" 相似度 0.96 > 0.90 阈值
  → 直接返回缓存的 LLM 答案（省一次 LLM 调用，响应从 2s 降到 10ms）
```

Redis 语义缓存 = **Hash 存问答对 + 向量字段 + KNN 查询**，三步实现（GPTCache 的 Redis 后端就是这个原理）：

```java
public String cachedAnswer(String question) {
    float[] vec = embeddingModel.embed(question);
    // 1. KNN 找最相似的缓存问题
    SearchResults hits = searchTopK(vec, 1);
    // 2. 相似度 ≥ 0.90 → 命中语义缓存
    if (!hits.isEmpty() && similarity(hits) >= 0.90) {
        return cachedAnswerOf(hits);      // 不调 LLM，直接返回
    }
    // 3. 未命中 → 调 LLM → 写入语义缓存
    String answer = llm.chat(question);
    store(question, vec, answer);
    return answer;
}
```

**工程坑**：阈值是调出来的（0.85 太松会答非所问，0.95 太严省不了钱）；答案有"时效性"的内容（价格、库存）必须短 TTL 或不走缓存。

## 一个诚实的边界

语义相似 ≠ 语义等价。"怎么注销账号" 和 "怎么注册账号" 词面极像，embedding 距离也近——**阈值边界上的误命中不可避免**。高敏感场景（法律、医疗、交易）：语义缓存只做"预填充草稿"，最终回答仍过一遍 LLM 或人工。

## 与既有教学体系的连接

| 旧知识 | 在向量场景的新身份 |
|--------|------------------|
| ZSet 的 score 排序（stage5/05） | 向量检索的"相似度排序"是同一思想的高维版 |
| 缓存穿透/空值（stage3/01） | 语义缓存同样要防"相似度都不够"的穿透 |
| MQ 削峰（stage4/01） | Embedding 生成是 LLM 调用——批量异步生成走 MQ |
| 大 Key 治理（stage3/04） | 向量条目 1536 维 ≈ 6KB/条，千万条 = 60GB——要分片/分索引 |

## 面试问法

**Q: Redis 能做向量数据库吗？什么时候选它？**
A: 能——RediSearch/Redis 8 原生向量集，HNSW 索引支持 KNN 与混合过滤（向量+普通字段同索引查询）。千万级以下向量、想复用现有 Redis 基础设施、要求低延迟的场景首选；亿级或重过滤场景选 Milvus/pgvector。典型应用：RAG 知识检索、语义缓存（相似问题命中缓存省 LLM 调用）、以文搜图。

---

**下一篇**：[04-OpenTelemetry全链路追踪](04-OpenTelemetry全链路追踪.md)。
