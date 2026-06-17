# JFR & Profiling 指引

> 当出现「响应变慢」「内存飙高」「CPU 打满」但日志看不出原因时，用 JFR 定位。

## 1. 启用 JFR

### Spring Boot 启动参数

```bash
# 启动时持续采集（推荐用于生产）
java -XX:StartFlightRecording:filename=recording.jfr,maxsize=500M,maxage=1h \
     -jar app.jar

# 或通过环境变量（Docker Compose）
# JAVA_OPTS: "-XX:StartFlightRecording:filename=/tmp/recording.jfr,maxsize=200M,maxage=30m"
```

### 运行时动态开启

```bash
# 获取 JVM 进程 PID
jps -l | grep app

# 开始录制（30 秒，定位瞬时问题）
jcmd <PID> JFR.start name=debug duration=30s filename=/tmp/debug.jfr

# 查看正在运行的录制
jcmd <PID> JFR.check
```

## 2. 分析 JFR 文件

### 命令行快速分析

```bash
# 热点方法（CPU 时间排名）
jfr print --events jdk.ExecutionSample /tmp/debug.jfr | head -50

# GC 事件汇总
jfr print --events jdk.GarbageCollection /tmp/debug.jfr

# 内存分配热点
jfr print --events jdk.ObjectAllocationInNewTLAB /tmp/debug.jfr | sort -k2 -n -r | head -20

# 线程阻塞事件
jfr print --events jdk.ThreadPark /tmp/debug.jfr | grep -v "java.lang.Thread.sleep" | head -20
```

### JDK Mission Control（可视化）

```bash
# 安装
sudo apt install openjdk-17-jdk-missioncontrol  # Ubuntu
# 或下载：https://adoptium.net/jmc/

# 打开 .jfr 文件，重点看：
#   - Code → Hot Methods（CPU 热点）
#   - Memory → Allocations（分配压力）
#   - Threads → Latencies（锁等待）
```

## 3. 常见问题定位

### 「CPU 打满」

```bash
jcmd <PID> JFR.start name=cpu duration=60s filename=/tmp/cpu.jfr
# 分析：jfr print --events jdk.ExecutionSample /tmp/cpu.jfr | sort | uniq -c | sort -rn | head -20
# 预期发现：死循环、无限递归、正则回溯
```

### 「内存飙高不降」

```bash
jcmd <PID> JFR.start name=heap duration=120s filename=/tmp/heap.jfr settings=profile
# 分析：jfr print --events jdk.ObjectAllocationInNewTLAB /tmp/heap.jfr | sort -k2 -n -r | head -20
# 预期发现：大对象频繁分配、缓存未设 TTL、ThreadLocal 泄漏
```

### 「接口偶尔慢」

```bash
# 录制 5 分钟，覆盖慢请求时间段
jcmd <PID> JFR.start name=latency duration=300s filename=/tmp/latency.jfr

# 在 JMC 中看 Threads → Latencies → 按等待时间排序
# 预期发现：DB 慢查询、外部 API 超时、锁竞争
```

## 4. 项目级定位步骤

当 Phase 4 集成测试或生产监控（alert-rules.yml）触发告警时：

| 告警 | 第一步 | 第二步 |
|------|--------|--------|
| HighLatency (P99 > 2s) | JFR 录制 60s → 看热点方法 | 检查是否调了外部 API → 看 api_call_seconds |
| JvmHeapUsageHigh (> 90%) | `jcmd <PID> GC.heap_dump /tmp/heap.hprof` → Eclipse MAT 分析 | JFR 看对象分配热点 |
| HikariPoolExhausted | JFR 看 ThreadPark 事件 → 谁在等连接 | `show full processlist` 看 MySQL 慢查询 |
| OutboxPendingHigh | 查 OutboxDispatcher 日志 → `grep "Outbox" *.log` | 查下游 MQ 是否正常 |

## 5. async-profiler（替代方案）

如果 JFR 不够，用 async-profiler 看火焰图：

```bash
# 安装
curl -L https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz | tar xz

# CPU 火焰图（30 秒）
./profiler.sh -d 30 -f /tmp/flamegraph.html <PID>
# 在浏览器打开 /tmp/flamegraph.html
```

## 6. 生产安全注意事项

- JFR 开销 < 1%（JVM 内置，推荐始终开启 `default` profile）
- `settings=profile` 开销约 2%，只在定位问题时临时用
- `jcmd GC.heap_dump` 会暂停 JVM（几百 ms ~ 几秒），按需使用
- JFR 文件包含类名/方法名，注意脱敏
