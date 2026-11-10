# 05 · GitHub Actions：把刚建的仓库变成自动流水线

你已经把 mq-mall 和 community 推上了 GitHub——现在给它们装上**自动大脑**：每次 push，GitHub 自动帮你编译、测试、构建 Docker 镜像、推到镜像仓库。这就是 CI/CD 的第一课，而且你的仓库现成可用。

## 概念一分钟

```
.git/workflows 里的 YAML = 流水线剧本
push / PR / 定时 → 触发 Workflow
   → Job（运行在一台临时 Ubuntu 容器 = Runner）
      → Step（checkout 代码 → 装 JDK → mvn verify → 构建镜像 → 推送）
```

**GitHub 免费额度**：私有仓库每月 2000 分钟 Actions 时长（公开仓库完全免费）——教学项目绰绰有余。

## 第一个 Workflow：push 自动编译测试

在 community 项目下创建 `.github/workflows/ci.yml`（已为你写好，见下）：

```yaml
name: CI
on:
  push:
    branches: [main]          # push 到 main 触发
  pull_request:               # 任何 PR 也触发

jobs:
  build:
    runs-on: ubuntu-latest    # GitHub 提供的临时机器
    steps:
      - uses: actions/checkout@v4          # 1. 拉代码

      - uses: actions/setup-java@v4        # 2. 装 JDK 17（带 Maven 缓存）
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Build & Test                 # 3. 编译+测试（mq-mall 同款命令）
        run: mvn -B verify

      - name: Upload test report           # 4. 测试报告存档（失败时可见）
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: target/surefire-reports/
```

推上去之后：GitHub 仓库页 **Actions** tab 立刻出现一次运行，绿勾 = 编译测试通过；改坏代码再推，红叉直接在 commit 上标出——**代码质量门禁从此自动化**。

## 进阶：构建 Docker 镜像推到 GHCR（GitHub 镜像仓库）

编译通过后顺手把应用打成镜像，推到 GitHub 自家的镜像仓库 GHCR（免费）：

```yaml
  docker:
    needs: build              # 编译成功才构建镜像
    runs-on: ubuntu-latest
    permissions:
      packages: write         # 允许推 GHCR
    steps:
      - uses: actions/checkout@v4
      - name: Build image
        run: |
          docker build -t ghcr.io/${{ github.repository }}:latest \
                       ghcr.io/${{ github.repository }}:${{ github.sha }} .
      - name: Push image
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker push ghcr.io/${{ github.repository }} --all-tags
```

`${{ secrets.GITHUB_TOKEN }}` 是 **Actions 自动注入的临时凭证**——不用配任何密钥就能推自家 GHCR（它的权限只在本仓库内，安全默认）。

配套需要一个 Dockerfile（已放在 community 根目录）：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 两个仓库的 workflow 已备好

| 仓库 | 文件 | 流水线内容 |
|------|------|-----------|
| community | `.github/workflows/ci.yml` | 编译测试 → Docker 镜像 → GHCR |
| mq-mall | `.github/workflows/ci.yml` | 编译测试（同样模式） |

把这两个文件 push 上去（`git add .github && git commit && git push`），然后打开仓库 Actions tab 看它转起来——**你第一次亲眼看 CI 跑通，就把"提交代码"和"质量验证"焊在了一起**。

## CI/CD 的完整图景（本篇是 C，D 在哪）

```
CI（本篇）：push → 自动构建+测试 → 镜像入库
CD（下一站）：镜像入库 → 自动部署
   简单版：Watchtower（服务器上自动拉新镜像重启）
   标准版：ArgoCD/GitOps——Git 里的 YAML 就是生产环境的状态声明
   K8s 版：Deployment 滚动更新（学习顺序：先 Docker 单机 → 再 K8s）
```

**学习路线建议**：本篇 CI → 自己租/用一台旧电脑跑 Watchtower 体验 CD → 想深入再学 K8s。不要一上来啃 K8s——没有 CI 的 K8s 只是把混乱自动化。

## Actions 实战三坑

| 坑 | 解法 |
|----|------|
| Maven 依赖每次全量下载（慢 5 分钟） | `setup-java` 的 `cache: maven`（上面已配） |
| 私有仓库 Actions 超免费额度 | 减少触发范围（只 main 分支）、共享缓存 |
| 测试要连 Redis/MySQL 但 Runner 上没有 | **Testcontainers**（下一章！）或 H2 替身——别用 CI 的"服务容器"硬凑 |

## 面试问法

**Q: 讲讲你的 CI/CD 流程。**
A: GitHub Actions——push 触发：checkout → JDK/Maven 缓存 → mvn verify（测试失败 PR 直接挡住）→ 通过后构建 Docker 镜像推 GHCR（用内置 GITHUB_TOKEN 免密）。部署侧从 Watchtower 拉新镜像起步，标准做法是 GitOps（ArgoCD）让 Git 成为环境唯一真相。设计要点：门禁自动化（测试不过不上库）、缓存加速、失败工件留存。

---

**下一篇**：[06-Debezium-CDC与Outbox模式](06-Debezium-CDC与Outbox模式.md)。
