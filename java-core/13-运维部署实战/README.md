# 13 · 运维部署实战：Docker、Linux 与上线

代码写完只是开始——**部署上线、日志排查、故障处理**是后端的另一半战场。本模块 4 篇全部结合本工作区的真实项目（mq-mall/learn-center 的 compose、你踩过的 Docker 坑）。

| 篇 | 主题 | 核心考点 |
|----|------|---------|
| [01](01-Docker入门与容器化部署.md) | Docker 实战 | 镜像/容器/Compose / **Dockerfile 最佳实践** / 你踩过的坑复盘 |
| [02](02-Linux排查命令速查.md) | Linux 排查命令 | top/jps/jstack / **日志三剑客 grep-awk-sed** / 网络 netstat-ss / 文件查找 |
| [03](03-上线部署Checklist.md) | 上线流程 | 发布前检查 / 回滚预案 / 灰度 / 监控告警 |
| [04](04-监控告警与Metrics.md) | 监控告警 | Micrometer→Prometheus→Grafana 链路 / **四黄金指标（看 P99）** / 业务大盘 / 告警纪律 |

## 与已有内容的连接

- Docker 教学篇第一课的案例就是**你踩过的坑**：docker-secrets-engine 损坏反复崩溃（踩坑实录坑六）、镜像拉取停滞（坑七）
- JVM 排查篇（02-JVM/04）的 Arthas/jstack 在 Linux 服务器上的使用是本模块的重点实操
- 监控（04 篇）是排查（02 篇/JVM 篇）的上游：告警先发现 → Trace 定位 → 日志/命令看现场；tracing 部分衔接 [stage6/04 OTel](../../stage6-新技术专题/04-OpenTelemetry全链路追踪.md)
- Redis 大 Key 排查、MySQL 慢查询——Linux 命令是所有排查的入口

## 环境说明

- 本工作区所有教学环境都是 **Docker 容器**（mq-mall 三件套/redis-learn），第一篇从"Docker 是什么"讲起
- Linux 命令在你的 Windows 上通过 **Git Bash** 大部分可用；服务器排查篇以 CentOS/Ubuntu 为准
