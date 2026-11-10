# 02 · 安装与 redis-cli

有 Docker 直接一行命令（推荐）。教学环境用端口 6381，避免与 mq-mall 的 6380 冲突。

## 方式一：Docker（推荐）

```bash
docker run -d --name redis-learn -p 6381:6379 redis:7-alpine
```

| 参数 | 含义 |
|------|------|
| `--name redis-learn` | 容器名 |
| `-p 6381:6379` | 6379 是 Redis 默认端口，宿主机映射到 6381 |
| `redis:7-alpine` | alpine 版仅 ~40MB |

验证 + 进入命令行：

```bash
docker exec -it redis-learn redis-cli
127.0.0.1:6379> ping
PONG        ← 活着
```

> redis-cli 默认连容器内的 6379；从宿主机连则 `redis-cli -p 6381`（需要本机装了 cli）或始终 `docker exec` 进去。

## redis-cli 基本操作（先混个脸熟）

```bash
SET name zhangsan          # 写
GET name                   # 读 → "zhangsan"
DEL name                   # 删
EXISTS name                # 是否存在 → 0
KEYS *                     # 列出所有 key（生产禁用！会阻塞，用 SCAN）
TTL session:1001           # 剩余存活秒数，-1 永不过期，-2 不存在
EXPIRE name 60             # 60 秒后过期
SET code 8848 EX 300       # 写+设置 300 秒 TTL，一步到位
TYPE name                  # 值类型 → string
DBSIZE                     # key 总数
FLUSHDB                    # 清空当前库（教学随便玩，生产禁用！）
```

**Key 命名规范（团队协作第一课）**：用冒号分层，`业务:对象:ID`

```
session:1001              登录会话
captcha:login:13800001111 短信验证码
order:cache:10086         订单缓存
rank:game:2026            2026 游戏排行榜
lock:stock:sku1           库存锁
```

好处：一眼看懂归属；`SCAN MATCH session:*` 可以按前缀扫描；避免 key 冲突。

## 16 个数据库的真相

`SELECT 0` ~ `SELECT 15` 切换库——**它只是命名空间，不是性能隔离**，生产几乎都用 db0 + key 前缀区分（Redis Cluster 只支持 db0）。别依赖多库做"隔离"。

## 可视化管理（选一个）

| 工具 | 特点 |
|------|------|
| **Another Redis Desktop Manager**（推荐） | 免费开源，TTL/ZSet 全可视化 |
| RedisInsight | Redis 官方，自带性能分析 |
| IDEA 插件 Redis | 顺手但不专业 |

Docker 起 RedisInsight：

```bash
docker run -d --name redis-insight -p 5540:5540 redis/redisinsight:latest
# 浏览器 http://localhost:5540 → 添加连接 host.docker.internal:6381
```

## 密码与配置（生产常识）

```bash
# 带密码 + 持久化启动
docker run -d --name redis-learn -p 6381:6379 \
  redis:7-alpine redis-server --requirepass "yourpass123" --appendonly yes
# 连接时：redis-cli -a yourpass123
```

常用配置项（ redis.conf / 命令行 --参数 都行）：

| 配置 | 默认 | 说明 |
|------|------|------|
| `requirepass` | 空 | 密码，**生产必设** |
| `maxmemory` | 不限 | 内存上限，触发淘汰策略（stage2/04） |
| `appendonly` | no | 开 AOF 持久化（stage2/03） |
| `maxmemory-policy` | noeviction | 内存满了怎么办 |

## 常见问题

| 现象 | 原因/解法 |
|------|----------|
| `Connection refused` | 容器没起 / 端口没映射，`docker ps` 看状态 |
| `NOAUTH Authentication required` | 设了密码没带 `-a` |
| `MISCONF Redis is configured to save RDB snapshots` | 磁盘写失败（常见于容器只读），`CONFIG SET stop-writes-on-bgsave-error no` 或修磁盘 |
| 中文显示 `\xe4\xbd\xa0` | `redis-cli --raw` 进入，按原始字符输出 |

---

**动手任务**：起容器 → `ping` 通 → 把"验证码、购物车、排行榜"三种业务数据用手动命令存进去（types 是什么先不用管，下一节揭晓）→ 给验证码设 300s TTL 并用 `TTL` 观察。

**下一节**：[03-五大基础数据类型](03-五大基础数据类型.md)——每种类型都对应一类真实业务。
