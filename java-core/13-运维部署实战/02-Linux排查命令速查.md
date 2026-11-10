# 02 · Linux 排查命令速查

后端线上排查的 Linux 命令全集——**按排查场景组织**，不是按字母背。全部命令在 Git Bash/服务器上可用。

## 场景一：CPU 飙高（配合 JVM 排查篇04）

```bash
top                          # 找到吃 CPU 的 Java 进程 PID
top -Hp <pid>                # 找到该进程里吃 CPU 的线程 TID
printf '%x\n' <tid>          # TID 转 16 进制
jstack <pid> | grep 0x<hex> -A 20   # 对应线程堆栈 → 定位代码行
# Arthas 一步版：thread -n 3
```

## 场景二：日志排查三剑客（grep / awk / sed）

```bash
# grep：找错误
grep -n "ERROR" app.log              # -n 行号；-i 忽略大小写；-A 3 后 3 行；-B 3 前 3 行；-C 3 前后
grep -rn "BizException" src/         # 递归搜代码

# awk：按列提取统计
awk '{print $1}' access.log          # 取第 1 列
awk '{a[$1]++} END {for(k in a) print k, a[k]}' access.log | sort -rn -k2 | head
#  ↑ 统计每列出现次数（如按 IP 聚合访问量）

# sed：流编辑（替换/删除行）
sed -n '100,200p' app.log            # 看 100~200 行
sed -i 's/ERROR/ERR/g' app.log       # 全局替换（-i 直接改文件）

# 组合管道：从 1GB 日志里找最近一小时某个订单号的全部相关日志
grep "order-1001" app.log | grep -v DEBUG | tail -100
```

**查日志三板斧**（背）：

```bash
tail -f app.log                      # 实时跟踪（Ctrl+C 退出）
grep -c "ERROR" app.log              # 错误计数
grep "order-1001" app.log | wc -l    # 关键词计数
```

## 场景三：磁盘与文件

```bash
df -h                        # 磁盘使用率（线上第一死因：磁盘满）
du -sh * | sort -rh | head   # 当前目录哪些文件/目录最大
find / -name "*.log" -mtime +7 -size +100M    # 找 7 天前的大日志文件
find /var/log -mtime +30 -exec rm {} \;       # 删 30 天前的日志（cron 定期清理）
lsof | grep deleted          # ★ 文件已删但空间未释放（进程还持有句柄）——磁盘满的经典隐形原因
# 解法：重启持有进程，或 echo > /proc/<pid>/fd/<n> 清空
```

## 场景四：网络排查

```bash
netstat -tlnp                # 监听的端口与进程（端口冲突排查）
ss -tlnp                     # 同上（更快，新工具）
curl -v http://localhost:8082/api/health      # verbose 看请求全过程
ping / telnet host port      # 连通性（telnet 测端口通不通）
tcpdump -i any port 6379     # 抓包看 Redis 协议交互（终极手段）
```

**你踩过的坑在这套命令下的呈现**（踩坑实录坑十二）：
- 代理截断 22 端口 → `telnet github.com 22` 通但握手被断 → 解法 ssh 走 443

## 场景五：进程与资源

```bash
ps -ef | grep java           # 进程列表
jps -l                       # Java 进程专用（JVM 篇04）
kill <pid>                   # 优雅终止（发 SIGTERM，进程可做清理）
kill -9 <pid>                # 强杀（SIGKILL，不做清理——最后手段）
free -h                      # 内存使用
uptime                       # 负载
vmstat 1                     # 系统整体状态
```

## 场景六：文本处理三剑客组合拳（日志分析实战）

```bash
# 统计 access.log 里每个接口的调用量 TOP10
awk '{print $7}' access.log | sort | uniq -c | sort -rn | head -10

# 找出响应时间超过 1s 的请求分布
grep "timeout" app.log | awk '{print $NF}' | sort -n | uniq -c

# 两文件差集：A 有 B 没有的是什么
comm -23 <(sort a.txt) <(sort b.txt)

# 多文件日志按时间合并排序（分布式日志排查）
cat *.log | sort -k1,2 > merged.log
```

## 常用 shell 技巧速记

| 技巧 | 命令 |
|------|------|
| 后台运行不中断（退出终端也活着） | `nohup java -jar app.jar &` |
| 查看进程环境变量 | `cat /proc/<pid>/environ \| tr '\0' '\n'` |
| 查看进程打开的文件 | `lsof -p <pid>` |
| 定时任务 | `crontab -e`（`*/5 * * * * 命令` 每 5 分钟） |
| 快速创建大文件测试 | `dd if=/dev/zero of=test.bin bs=1M count=100` |
| 压缩解压 | `tar -czf x.tar.gz dir/` / `tar -xzf x.tar.gz` |

## 面试问法

**Q: 线上日志文件几个 G，怎么快速定位问题？**
A: 三板斧组合——①grep 精确关键词+时间窗缩小范围（`grep "2026-11-09 10:3" app.log`）；②管道聚合统计（awk 按维度计数+sort 排名找异常模式）；③tail -f 实时观察复现。大文件不打开不加载，全用流式管道逐行处理——内存占用恒定。配套：日志要提前规划格式（时间戳在前可按时间 grep 切片、traceId 串联多文件）。

---

**下一篇**：[03-上线部署Checklist](03-上线部署Checklist.md)。
