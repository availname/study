# 08 · Git 实战工作流与协作

Git 不是背命令，是**一套"随时可以反悔"的工作方式**。本篇 = 日常流 + 分支协作 + 事故自救（含本工作区真实踩过的坑：代理截断 22 端口、幽灵进程占端口）。

## 一、一次性配置（新机器五分钟）

```bash
git config --global user.name "你的名字"
git config --global user.email "you@example.com"

# GitHub 连接（两种方式二选一）
# 方式A：SSH（推荐，一次配置永久免密）
ssh-keygen -t ed25519 -C "备注"            # 生成密钥对（~/.ssh/）
cat ~/.ssh/id_ed25519_github.pub           # 复制公钥 → GitHub Settings→SSH keys→粘贴
ssh -T git@github.com                      # 验证：Hi <用户名>! 即成功

# 方式B：gh CLI（还能建仓库/开PR）
winget install GitHub.cli                  # 或下载 zip 免安装版
gh auth login                              # 浏览器设备码授权

# 大陆网络环境的坑（踩坑实录坑十二）：代理会截断 22 端口
# ~/.ssh/config 强制 GitHub SSH 走 443 备用通道：
#   Host github.com
#       HostName ssh.github.com
#       Port 443
#       User git
#       IdentityFile ~/.ssh/id_ed25519_github
```

**首次建仓推送三步**（本工作区 mq-mall/community 都是这个流程）：

```bash
git init -b main && git add -A && git commit -m "init"
gh repo create mq-mall --private --source=. --push     # 建仓+推送一步完成（需 gh 登录）
# 或网页建仓后：git remote add origin git@github.com:用户名/仓库名.git && git push -u origin main
```

## 二、日常流（add → commit → push 的纪律）

```bash
git status                 # 先看状态再动手（永远先 status）
git add -p                 # 分块暂存（只提交相关改动——不要一股脑 add -A）
git commit -m "fix: 修复秒杀发号器重启归零导致的幂等误杀"
git push origin main
```

**Commit message 规约**（团队协作第一课）：

```
<类型>: <一句话说清做了什么>
类型：feat 功能 / fix 修复 / refactor 重构 / docs 文档 / test 测试 / chore 杂务
好：fix: 修复秒杀发号器重启归零导致的幂等误杀
差：修改了一些代码 / update / fix bug
```

- **一个 commit 只做一件事**——回滚和 review 都靠这个
- 提交前 `git diff` 自查一遍（别把密码/调试代码提交上去）

## 三、分支与合并（团队协作流）

```bash
git switch -c feat/sign-in                 # 建功能分支（别在 main 直接开发）
# ...开发、commit...
git push -u origin feat/sign-in            # 推分支 → GitHub 上开 Pull Request
# PR 里 review → merge（团队约定 merge/squash/rebase）
git switch main && git pull && git branch -d feat/sign-in   # 合并后清理本地
```

**常用排查命令**：

```bash
git log --oneline --graph --all    # 图形化历史
git diff HEAD~1                    # 看上一次提交改了什么
git blame 文件                     # 每行是谁/哪次提交写的（甩锅与感谢两用）
git show <commit>                  # 看某次提交详情
```

## 四、事故自救（每条都救过命）

| 事故 | 解法 |
|------|------|
| 改乱了想回到上次提交 | `git restore .`（丢弃工作区改动）· `git restore --staged 文件`（取消暂存） |
| commit 了但还没 push，想改 | `git commit --amend`（追加到上次提交，改 message 也用它） |
| **commit 了并 push 了，想撤销** | `git revert <commit>`（生成一条"反向提交"——**安全**，不改历史） |
| 提交了密码/大文件必须从历史抹掉 | `git filter-repo` 重写历史 + **立刻换掉泄露的密钥**（历史抹除后旧密钥也作废） |
| 误删分支/reset 过头 | `git reflog` 找到丢失前的 commit 号 → `git branch 恢复分支 <号>`（reflog 是 Git 的黑匣子，90 天内都能捞） |
| merge 冲突 | 打开文件找 `<<<<<<< ======= >>>>>>>` 标记 → 手工合并 → `git add` → `git commit` |

> [!CAUTION]
> `git reset --hard` / `push --force` 是"改写历史"的危险操作：**只对纯个人分支使用**；共享分支上用 revert（向前撤销）而不是 reset（向后抹除）。

## 五、与 CI 联动（stage6/05）

```
.gitignore（target/ *.log .idea/）→ 提交干净
.github/workflows/ci.yml → push 自动 mvn verify + 构建镜像推 GHCR
PR 必须 CI 绿勾才允许 merge（仓库 Settings→Branch protection 设置）
```

## 六、本工作区的真实坑（完整版见踩坑实录）

| 坑 | 教训 |
|----|------|
| 代理截断 22 端口（坑十二） | ssh.github.com:443 备用通道写进 config |
| 幽灵进程占端口 | 先 `netstat -ano` 找 PID 杀干净再起应用 |
| HTTP 中文参数 400（坑十一） | curl 用 `--data-urlencode` |
| gh CLI zip 免安装版 | winget 失败时的备选：下载 zip 解压即用，`bin` 目录加入 PATH |

## 面试/工作常见问法

**Q: merge 和 rebase 的区别？**
A: merge 保留分支历史产生合并提交（真实但乱）；rebase 把提交"搬到"目标分支顶端（线性历史好看）——**铁律：已 push 的公共分支不 rebase**（改写历史会坑掉所有协作者）。个人本地分支随手 rebase 没问题。

**Q: git reset 三种模式区别？**
A: `--soft` 只动 HEAD（改动保留在暂存区）；`--mixed`（默认）动 HEAD+暂存区（改动留在工作区）；`--hard` 全动（**工作区改动也没了，慎用**）。

---

**Git 的终极心法**：`status` → `diff` → `add` → `commit` 是肌肉记忆；**任何"完蛋了"先 `git reflog`**——Git 几乎没有真正不可逆的操作，除非你 `--force` 推了且没留 reflog。
