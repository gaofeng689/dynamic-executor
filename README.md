# Dynamic Executor

基于 Spring Boot 的线程池动态管理项目，支持在运行时通过 API 或 Web 页面实时修改线程池参数，无需重启应用。

## 项目结构

```
dynamic_executor/
├── pom.xml                                    # 父 POM（packaging=pom）
├── db/
│   └── init.sql                               # 建库建表脚本
├── dynamic-executor-core/                     # 业务模块 + Admin Client（端口 8080）
│   └── src/main/java/com/gaofeng/dynamicexecutor/
│       ├── DynamicExecutorApplication.java    # 启动类
│       ├── config/
│       │   └── ThreadPoolProperties.java      # 线程池配置属性
│       ├── controller/
│       │   └── ExecutorController.java        # REST 接口控制器
│       ├── dto/
│       │   └── ThreadPoolConfigDTO.java       # 数据传输对象
│       ├── metrics/
│       │   ├── PoolSnapshot.java              # 指标快照实体
│       │   ├── PoolSnapshotMapper.java        # MyBatis-Plus Mapper
│       │   └── PoolMetricsCollector.java      # 定时采集器
│       ├── scale/
│       │   ├── AutoScaler.java                # 自动伸缩执行器
│       │   ├── ScaleProperties.java           # 伸缩规则配置
│       │   └── ScaleEvent.java                # 伸缩事件记录
│       └── util/
│           └── ThreadPoolManager.java         # 线程池管理器（核心逻辑）
├── admin-server/                              # Spring Boot Admin Server（端口 9090）
│   └── src/main/java/com/gaofeng/admin/
│       └── AdminServerApplication.java
└── README.md
```

## 功能特性

1. **线程池实时监控** — 查看线程池的实时运行状态（线程数、任务数、队列状态等）
2. **动态配置修改** — 无需重启应用，运行时即时修改核心线程数、最大线程数、存活时间
3. **多队列类型支持** — LinkedBlockingQueue / ArrayBlockingQueue / SynchronousQueue / PriorityBlockingQueue
4. **多拒绝策略支持** — CallerRunsPolicy / AbortPolicy / DiscardPolicy / DiscardOldestPolicy
5. **即时重建生效** — 每次配置修改都销毁旧线程池并用新配置重建，保证配置一致性
6. **Web 管理页面** — 左右布局：实时状态 + 配置表单 + Chart.js 历史趋势图 + 自动伸缩面板
7. **Spring Boot Admin** — admin-server 模块提供监控面板（端口 9090），采集 Client 端运行指标
8. **指标持久化** — MyBatis-Plus + MySQL 定时采集线程池快照，Chart.js 折线图展示趋势
9. **自动伸缩** — 根据队列使用率自动扩容 / 根据活跃线程占比自动缩容（连续达标防抖动）

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 创建数据库

执行 `db/init.sql` 中的建库建表语句。

### 启动

```bash
# 终端 1：启动 Admin Server
mvn -pl admin-server spring-boot:run

# 终端 2：启动业务模块
mvn -pl dynamic-executor-core spring-boot:run
```

启动后访问地址：

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/manage.html` | 完整管理界面 |
| `http://localhost:8080/executor` | 线程池实时状态 JSON |
| `http://localhost:8080/executor/config` | 查看/修改线程池配置 |
| `http://localhost:9090` | Spring Boot Admin 监控面板 |

## 接口文档

### 1. 查询线程池实时状态

```
GET /executor
```

响应示例（JSON）：
```json
{
  "核心线程数": 2,
  "最大线程数": 4,
  "当前线程数": 0,
  "活跃线程数": 0,
  "已完成任务数": 0,
  "总任务数": 0,
  "队列类型": "LinkedBlockingQueue",
  "队列大小": 0,
  "队列剩余容量": 50,
  "存活时间": "30 秒",
  "拒绝策略": "CallerRunsPolicy"
}
```

### 2. 获取线程池配置

```
GET /executor/config
```

### 3. 修改线程池配置（即时生效）

```
PUT /executor/config
Content-Type: application/json
```

所有字段均为可选，`null` 字段不会被修改。每次调用都会更新属性并重建线程池，确保配置完全一致。

```bash
curl -X PUT http://localhost:8080/executor/config \
  -H "Content-Type: application/json" \
  -d '{"corePoolSize":10,"maximumPoolSize":30}'
```

### 4. 查询历史指标快照

```
GET /executor/metrics/history?minutes=5
```

### 5. 自动伸缩状态与历史

```
GET /executor/scale/status
PUT /executor/scale/status    # 启用/停用
GET /executor/scale/history
```

### 6. 制造负载波动

```
POST /executor/task/load?count=10&sleepMs=3000
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `thread.pool.core-pool-size` | 2 | 核心线程数 |
| `thread.pool.maximum-pool-size` | 4 | 最大线程数 |
| `thread.pool.keep-alive-time` | 30 | 非核心线程存活时间 |
| `thread.pool.time-unit` | SECONDS | 时间单位：SECONDS / MINUTES / HOURS / DAYS |
| `thread.pool.queue-type` | LINKED_BLOCKING | 队列类型 |
| `thread.pool.queue-capacity` | 50 | 队列容量 |
| `thread.pool.rejected-policy` | CALLER_RUNS | 拒绝策略 |
| `metrics.collection-interval-seconds` | 10 | 指标采集间隔（秒） |
| `metrics.retention-hours` | 24 | 指标保留时长（小时） |
| `scale.enabled` | true | 是否启用自动伸缩 |
| `scale.scale-up.queue-usage-percent` | 80 | 扩容触发阈值（%） |
| `scale.scale-up.delta` | 2 | 扩容步长 |
| `scale.scale-down.active-thread-ratio` | 0.3 | 缩容触发比例 |
| `scale.scale-down.consecutive-checks` | 4 | 缩容连续达标次数 |
