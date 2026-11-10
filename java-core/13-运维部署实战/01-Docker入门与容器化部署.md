# 01 · Docker 入门与容器化部署

你已经在用 Docker（mq-mall 三件套、redis-learn、Testcontainers）——本篇把"会用"升级为"懂原理、会写 Dockerfile、能排坑"。

## 一、核心三概念（一张图）

```
镜像 Image（类/模板，只读）
   │ docker run
   ▼
容器 Container（实例/进程，可写层叠加在只读层上）
   │ docker commit / dockerfile build
   ▼
新镜像 Image
```

| 命令 | 用途 | 你项目的例子 |
|------|------|-------------|
| `docker run -d --name x -p 3307:3306 mysql:8.0` | 启动容器（-d 后台，-p 端口映射：宿主:容器） | mq-mall 三件套 |
| `docker ps` / `docker ps -a` | 运行中/全部容器 | 排查第一命令 |
| `docker logs -f 容器名` | 看日志（-f 跟随） | 应用排障第一入口 |
| `docker exec -it 容器 bash` | 进入容器 | 进 MySQL/Redis 容器执行命令 |
| `docker stop/start/rm/restart` | 生命周期管理 | — |
| `docker cp 容器:/path ./` | 容器与宿主机复制文件 | 拉取容器内日志/dump |
| `docker inspect 容器` | 查看详情（IP/挂载/环境变量） | 排查网络问题 |

## 二、Dockerfile：怎么写一个高质量镜像

mq-mall/community 的 Dockerfile（教学版）：

```dockerfile
FROM eclipse-temurin:17-jre-alpine      # 基础镜像（jre 只含运行时，比 JDK 小一半+）
WORKDIR /app
COPY target/*.jar app.jar               # 复制 jar
ENTRYPOINT ["java", "-jar", "app.jar"]  # 启动命令
```

### 生产级 Java Dockerfile 的四个优化

```dockerfile
# ① 分层构建：依赖层与应用层分离——代码改动只重建薄层（利用缓存，构建秒级）
FROM eclipse-temurin:17-jre AS builder
WORKDIR /app
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]

# ② JVM 容器感知参数（JDK 10+ 自动识别容器内存限制）
#    -XX:MaxRAMPercentage=75.0  （用容器内存的 75%，避免 OOMKilled）
# ③ 非 root 用户运行（安全）
RUN addgroup -S app && adduser -S app -G app
USER app
# ④ 健康检查交给 compose/k8s（HEALTHCHECK）
```

**两个经典容器坑（都与你有关）**：

| 坑 | 原因 | 解法 |
|----|------|------|
| Java 容器内存 OOMKilled | JVM 误用宿主机内存（老 JDK）| JDK 10+ 默认容器感知 + `-XX:MaxRAMPercentage=75` |
| 时区不对（日志差 8 小时） | 容器默认 UTC | `-e TZ=Asia/Shanghai` 或 ENTRYPOINT 加时区参数 |

## 三、Docker Compose：多容器编排

mq-mall 的 compose（你一直在用的）就是最佳示例：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment: { MYSQL_ROOT_PASSWORD: root123, MYSQL_DATABASE: mq_mall }
    ports: ["3307:3306"]
    volumes:
      - mq-mall-mysql-data:/var/lib/mysql       # 数据卷：容器删数据不丢
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-uroot", "-proot123"]
  redis: { image: redis:7-alpine, ports: ["6380:6379"] }
  rabbitmq:
    image: rabbitmq:3.13-management
    ports: ["5673:5672", "15673:15672"]
volumes:
  mq-mall-mysql-data:
  mq-mall-rabbit-data:
```

**关键三讲**：

1. **Volumes 数据卷**（数据持久化——容器删了数据在）：
   - `volumes:` 命名卷（Docker 管理，安全）vs 宿主机挂载 `./data:/path`（直观可看）
   - **你踩过的坑实例**：rabbit 数据在卷里，容器强杀重建数据不丢
2. **depends_on + healthcheck**：`depends_on: condition: service_healthy` 保证 MySQL 就绪后才起应用（mq-mall compose 的 `--wait` 就是等健康检查）
3. **网络**：同 compose 内容器**用服务名互相访问**（`mysql:3306`，不是 localhost——localhost 是容器自己）

## 四、镜像加速与排查（踩坑实录坑七复盘）

```bash
# 拉取停滞/失败：配置镜像加速器（Docker Desktop → Settings → Docker Engine）
{
  "registry-mirrors": ["https://docker.m.daocloud.io"]
}

# 排查命令
docker images                    # 本地镜像
docker system df                 # 磁盘占用（images/containers/volumes/cache）
docker system prune -a           # 清理无用镜像/容器（谨慎：会删所有停止的容器）
```

## 五、Docker 是分层文件系统（理解构建缓存）

```
镜像 = 只读层叠加（Dockerfile 每条指令一层）
Dockerfile 指令顺序 = 缓存命中策略：
  COPY pom.xml + RUN mvn dependency:go-offline   ← 依赖层（少变，缓存长）
  COPY src/ + RUN mvn package                     ← 源码层（常变，重建只走这层）
改代码只重建源码层——构建提速的关键就是"把常变的放后面"
```

（mq-mall/community 的 Dockerfile 用 layertools 分层 + Spring Boot 分层 jar，同理）

## 六、生产实践：容器的日志与资源限制

| 实践 | 做法 |
|------|------|
| 日志轮转防磁盘爆 | compose 加 `logging: {driver: json-file, options: {max-size: "50m", max-file: "3"}}` |
| 内存限制 | `deploy.resources.limits.memory` 或 `-m 2g`——防容器吃光宿主机内存 |
| 数据库密码 | 不写死 yml——环境变量注入或 secrets（你项目的 root123 是教学环境专用） |

## 面试问法

**Q: 容器和虚拟机的区别？**
A: VM 虚拟整套硬件+Guest OS（GB 级、分钟启动）；容器共享宿主机内核，只是被隔离约束的进程组（MB 级、秒级启动）——通过 namespace（隔离视图：pid/net/mnt）+ cgroups（限制资源）实现。代价是隔离性弱于 VM，收益是轻量高密度。Docker 镜像分层联合文件系统（overlay2），层可复用共享——构建缓存与增量分发的基础。

---

**下一篇**：[02-Linux排查命令速查](02-Linux排查命令速查.md)。
