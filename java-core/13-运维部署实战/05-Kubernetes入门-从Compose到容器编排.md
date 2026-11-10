# 05 · Kubernetes 入门：从 Compose 到容器编排

> 🎯 **定位**：[01 篇](01-Docker入门与容器化部署.md) 的 Docker/Compose 解决了"单机容器化"，[03 篇](03-上线部署Checklist.md) 讲了"灰度/回滚怎么做"——但都是**单机视角**。当 mq-mall 的订单服务要跑 3 个实例（多实例后 @Scheduled 扫单 Job 重复跑的问题你在 [微服务 07](../05-微服务全家桶/07-XXL-Job分布式任务调度.md) 已经见过）、机器挂了要自动迁移、发版要滚动不中断——**这就是容器编排**，K8s 是事实标准。本篇只建立"骨架级"认知：它解决什么、核心对象怎么对应你已会的概念、面试怎么答——不展开运维细节（那是要在工作中填的坑）。

## 一、为什么 Compose 不够：单机编排 vs 集群编排

```
Docker Compose（单机）                    Kubernetes（集群）
┌──────────────────────┐          ┌────────────────────────────────┐
│  一台宿主机            │          │  N 台机器组成集群（Node）         │
│  yml 定义 5 个容器      │          │  声明"我要 3 个订单副本"          │
│  挂了？→ 没人管         │          │  挂了 → 自动重建（自愈）          │
│  发版 → docker 手动    │          │  发版 → 滚动更新，逐个替换就绪才继续 │
│  扩容 → 手动 run       │          │  扩容 → 改一个数字/自动 HPA       │
└──────────────────────┘          └────────────────────────────────┘
```

**Compose 的三个天花板**（每个都是 K8s 的存在理由）：
1. **单机**——机器挂了服务全灭；K8s 多 Node 调度，机器挂自动迁走（自愈）；
2. **无声明式期望**——Compose 是"启动命令"，K8s 是"期望状态声明"：你只说"要 3 个副本"，控制器永远把实际状态向期望对齐（这个思想在 mq-mall 支付 CAS"对账"里你已经见过：期望 vs 实际的差异驱动动作）；
3. **流量无感知**——容器重启 IP 就变；K8s Service 提供稳定虚拟 IP + 负载均衡（Nacos 服务发现的"静态化版本"）。

## 二、核心对象：用你已会的概念"对号入座"

| K8s 对象 | 一句话 | 对应你已会的 |
|---------|--------|-------------|
| **Pod** | 最小调度单位（1~N 个共享网络/存储的容器） | 一个"进程组"：应用容器 + sidecar（如日志收集） |
| **Deployment** | 声明"要 N 个 Pod 副本"，管滚动更新/回滚 | Compose 的 service + 03 篇"滚动发布/回滚"的自动化 |
| **Service** | 一组 Pod 的稳定访问入口（虚拟 IP + 负载均衡） | Nacos 服务发现（[微服务 01](../05-微服务全家桶/01-Nacos注册中心.md)）的内置版 |
| **ConfigMap / Secret** | 配置/敏感信息注入容器 | Nacos 配置中心（[微服务 02](../05-微服务全家桶/02-Nacos配置中心.md)）；Docker 篇"密码不写死 yml" |
| **Ingress** | 集群入口路由（域名→Service） | Gateway 网关（[微服务 04](../05-微服务全家桶/04-Gateway网关.md)）的集群层版本 |
| **HPA** | 按 CPU/自定义指标自动扩缩容 | Sentinel 限流（[微服务 05](../05-微服务全家桶/05-Sentinel熔断限流.md)）是"挡"，HPA 是"加人" |
| **Probe（探针）** | liveness 挂了重启 / readiness 没好不接流量 | mq-mall Dockerfile HEALTHCHECK（01 篇）的集群版 |

**一张图记住关系**：

```
用户 → Ingress（域名路由） → Service（稳定入口） → Pod × 3（Deployment 管着）
                                      ↑
                      ConfigMap（配置）/ Secret（密码）注入 Pod
```

## 三、最小可用清单：把 mq-mall 部署上 K8s

```yaml
# deployment.yaml —— 声明"mq-mall 要 2 个副本"
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mq-mall
spec:
  replicas: 2                       # 期望状态：2 个副本（挂一个自动补一个）
  selector:
    matchLabels: { app: mq-mall }
  template:
    metadata:
      labels: { app: mq-mall }
    spec:
      containers:
        - name: app
          image: ghcr.io/availname/community:latest   # CI 推的镜像（[stage6/05](../../stage6-新技术专题/05-GitHubActions-CICD.md)）
          ports: [{ containerPort: 8090 }]
          env:
            - name: SPRING_DATA_REDIS_HOST
              valueFrom: { configMapKeyRef: { name: mq-mall-config, key: redis-host } }
          resources:                 # 资源限制（Docker 篇 -m 2g 的升级版）
            requests: { memory: "512Mi", cpu: "250m" }
            limits: { memory: "1Gi", cpu: "500m" }
          readinessProbe:            # 没就绪不接流量（滚动更新不 502 的关键）
            httpGet: { path: /actuator/health, port: 8090 }
          livenessProbe:             # 死锁/假死自动重启
            httpGet: { path: /actuator/health/liveness, port: 8090 }
---
# service.yaml —— 稳定入口
apiVersion: v1
kind: Service
metadata:
  name: mq-mall-svc
spec:
  selector: { app: mq-mall }
  ports: [{ port: 80, targetPort: 8090 }]
```

```bash
kubectl apply -f deployment.yaml   # 声明期望状态（不是"启动"，是"注册期望"）
kubectl get pods -w                # 看实际向期望对齐的过程
kubectl rollout undo deployment/mq-mall   # 一行回滚（03 篇回滚纪律的自动化）
```

**滚动更新发生了什么**（面试必答）：新 ReplicaSet 起 1 个新 Pod → readiness 探针通过 → Service 摘一个旧 Pod 加一个新 Pod → 全部替换后旧 ReplicaSet 归零。**任何时刻都有就绪实例在接流量**——这就是 03 篇"发布中观察期"要人盯的事，K8s 变成了自动的。

## 四、K8s 时代的"多实例"问题：你早就见过一半

多副本之后，教学项目里"单实例假设"的代码全部现形——**这正是微服务 07 篇的引子**：

| 单实例写法 | 多副本下的事故 | 解法（已学/新学） |
|-----------|--------------|-----------------|
| `@Scheduled` 扫单 Job 每台都跑 | 同一订单被重复关单（CAS 幂等才没出事——mq-mall OrderSweepJob 注释里写的"重复执行无害"就是防线） | 分布式调度 XXL-Job / K8s **CronJob** 单点跑 |
| 本地缓存（Caffeine） | 各副本不一致 | Redis 中央缓存（redis stage3 一致性） |
| Session 登录态 | 副本间不共享 | Redis Token / JWT（[19 篇](../19-登录态与接口安全.md) 对比表） |
| 文件存本地磁盘 | 副本看不到彼此的文件 | 对象存储（OSS/S3）/ PV 挂载 |

**K8s 的态度**：容器**随时可死、随时可迁**（不可变基础设施）——所以"本地状态"一律外置：状态进 Redis/DB，文件进对象存储，配置进 ConfigMap。这就是为什么云原生应用"天生"要用 Redis——不是跟风，是**容器生命周期倒逼的架构**。

## 五、知识边界：面试够用，工作再深

| 层次 | 内容 | 现在要会 |
|------|------|---------|
| 概念 | Pod/Deployment/Service/ConfigMap/探针/滚动更新 | ✅ 本篇 |
| 使用 | kubectl apply/get/logs/describe、看 Pod 事件排错 | ✅ 本篇入门 + 02 篇 Linux 排查迁移 |
| 运维 | 集群搭建、网络插件、存储类、RBAC、Helm | ⏸ 入职后按平台团队的规范学（多数公司有现成平台） |

**面试问法**

**Q1: K8s 和 Docker Compose 的区别？**
A: Compose 是单机"启动脚本"——定义容器怎么起；K8s 是集群"期望状态控制器"——声明要几个副本，控制器持续把实际向期望对齐：机器挂了自动迁移（自愈）、发版自动滚动（探针保证不中断）、扩容改一个数字。本质区别是**命令式 vs 声明式**，和我项目里支付状态机的 CAS 思想同源：声明期望，差异驱动动作。

**Q2: Pod 和容器什么关系？Deployment 又解决什么？**
A: Pod 是最小调度单位，内含 1~N 个共享网络/存储的容器（主容器+sidecar）；Deployment 管理 Pod 副本数与滚动更新——声明 replicas=2，挂一个自动补一个；发版起新 ReplicaSet 逐个替换，readiness 探针保证任何时刻都有就绪实例接流量，一行 rollout undo 回滚。

**Q3: 应用上 K8s 后要注意什么？（多副本问题）**
A: 容器随时可死可迁，本地状态必须外置：登录态上 Redis（我项目的 UUID token 天然满足）、定时任务防多副本重复跑（XXL-Job 或 K8s CronJob）、本地缓存换 Redis 中央缓存、文件上对象存储。我项目里 OrderSweepJob 的 CAS 幂等设计"重复执行无害"，就是为多实例部署预埋的防线。

**Q4: liveness 和 readiness 探针的区别？**
A: liveness 失败→重启容器（治假死/死锁）；readiness 失败→摘出 Service 不接流量但不重启（治"还在预热/暂时过载"）。发布场景 readiness 是滚动更新不 502 的关键——新 Pod 就绪才摘旧的。配 Spring Boot Actuator 的 health 端点（mq-mall 已引入 actuator）。

---

**下一篇**：回 [13-运维部署实战 README](README.md) · 前置：[01-Docker 入门](01-Docker入门与容器化部署.md)（镜像/Compose 基础）、[03-上线部署 Checklist](03-上线部署Checklist.md)（灰度/回滚纪律）、[微服务 07-XXL-Job](../05-微服务全家桶/07-XXL-Job分布式任务调度.md)（多实例任务调度）。
