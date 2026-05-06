# Dynamic Executor

基于 Spring Boot 的线程池动态管理项目，支持在运行时通过 API 或 Web 页面实时修改线程池参数，无需重启应用。

## 项目结构

```
dynamic_executor
├── pom.xml
└── src/main
    ├── java/com/abioclaw/dynamicexecutor
    │   ├── DynamicExecutorApplication.java       # 启动类
    │   ├── config
    │   │   └── ThreadPoolProperties.java         # 配置属性类（绑定 application.yml）
    │   ├── dto
    │   │   └── ThreadPoolConfigDTO.java          # 数据传输对象
    │   ├── util
    │   │   └── ThreadPoolManager.java            # 线程池管理器（核心逻辑）
    │   └── controller
    │       └── ExecutorController.java           # REST 接口控制器
    └── resources
        ├── application.yml                        # 应用配置（YAML 格式）
        └── static
            └── manage.html                        # 管理页面
```

## 功能特性

1. **线程池实时监控** — 查看线程池的实时运行状态（线程数、任务数、队列状态等）
2. **动态配置修改** — 无需重启应用，运行时即时修改核心线程数、最大线程数、存活时间
3. **多队列类型支持** — 支持 LinkedBlockingQueue、ArrayBlockingQueue、SynchronousQueue、PriorityBlockingQueue
4. **多拒绝策略支持** — 支持 CallerRunsPolicy、AbortPolicy、DiscardPolicy、DiscardOldestPolicy
5. **即时重建生效** — 每次配置修改都销毁旧线程池并用新配置重建，保证配置一致性
6. **Web 管理页面** — 提供可视化界面管理线程池配置
7. **YAML 配置驱动** — 使用类型安全的外部化配置，支持通过配置文件设置初始参数

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 启动

```bash
mvn spring-boot:run
```

启动后访问地址：

| 地址 | 说明 |
|------|------|
| `http://localhost:8080/executor` | 查看线程池实时状态 |
| `http://localhost:8080/executor/config` | 查看/修改线程池配置 |
| `http://localhost:8080/manage.html` | Web 管理页面 |

## 接口文档

### 1. 查询线程池实时状态

```
GET /executor
```

**响应示例** (text/plain)：
```
线程池信息: {核心线程数: 50, 最大线程数: 100, 当前线程数: 0, 活跃线程数: 0, 已完成任务数: 0, 总任务数: 0, 队列大小: 0, 队列剩余容量: 1000, 存活时间: 600 秒, 拒绝策略: CallerRunsPolicy}
```

### 2. 获取线程池配置

```
GET /executor/config
```

**响应示例** (application/json)：
```json
{
    "corePoolSize": 50,
    "maximumPoolSize": 100,
    "keepAliveTime": 600,
    "timeUnit": "SECONDS",
    "queueType": "LINKED_BLOCKING",
    "queueCapacity": 1000,
    "rejectedPolicy": "CALLER_RUNS"
}
```

### 3. 修改线程池配置（即时生效）

```
PUT /executor/config
Content-Type: application/json
```

**请求体示例**：
```json
{
    "corePoolSize": 10,
    "maximumPoolSize": 30,
    "keepAliveTime": 120,
    "timeUnit": "MINUTES",
    "queueType": "ARRAY_BLOCKING",
    "queueCapacity": 500,
    "rejectedPolicy": "ABORT"
}
```

> 所有字段均为可选，`null` 字段不会被修改。每次调用都会更新属性并重建线程池，确保配置完全一致。

**curl 示例**：
```bash
curl -X PUT http://localhost:8080/executor/config \
  -H "Content-Type: application/json" \
  -d '{"corePoolSize":10,"maximumPoolSize":30}'
```

## 配置说明

[application.yml](file:///d:/git/projects/abioclaw/dynamic_executor/src/main/resources/application.yml) 中的线程池配置项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `thread.pool.core-pool-size` | 2 | 核心线程数 |
| `thread.pool.maximum-pool-size` | 4 | 最大线程数 |
| `thread.pool.keep-alive-time` | 30 | 非核心线程存活时间 |
| `thread.pool.time-unit` | SECONDS | 时间单位：`SECONDS` / `MINUTES` / `HOURS` / `DAYS` |
| `thread.pool.queue-type` | LINKED_BLOCKING | 队列类型：`LINKED_BLOCKING` / `ARRAY_BLOCKING` / `SYNCHRONOUS` / `PRIORITY_BLOCKING` |
| `thread.pool.queue-capacity` | 50 | 队列容量 |
| `thread.pool.rejected-policy` | CALLER_RUNS | 拒绝策略：`CALLER_RUNS` / `ABORT` / `DISCARD` / `DISCARD_OLDEST` |

## 技术知识点

### ThreadPoolExecutor 线程池原理

线程池通过复用线程减少线程创建/销毁的开销，核心工作流程：

1. 新任务到来，线程数 < corePoolSize → 创建新线程直接执行
2. 线程数 ≥ corePoolSize → 任务放入等待队列
3. 等待队列已满，线程数 < maximumPoolSize → 创建新线程执行
4. 等待队列已满，线程数 ≥ maximumPoolSize → 执行拒绝策略

### 四种等待队列特性

| 队列类型 | 特点 |
|----------|------|
| **LinkedBlockingQueue** | 基于链表，可选有界/无界，吞吐量高 |
| **ArrayBlockingQueue** | 基于数组，必须有界，内存占用连续且低 |
| **SynchronousQueue** | 不存储元素，每个 put 必须等待 take，适用于直接交付场景 |
| **PriorityBlockingQueue** | 基于优先级排序，必须实现 Comparable |

### 四种拒绝策略

| 策略 | 行为 |
|------|------|
| **CallerRunsPolicy** | 由提交任务的调用者线程执行该任务 |
| **AbortPolicy** | 抛出 RejectedExecutionException 异常 |
| **DiscardPolicy** | 静默丢弃被拒绝的任务 |
| **DiscardOldestPolicy** | 丢弃队列中等待最久的任务，然后提交新任务 |

### Spring Boot 关键技术

- **`@ConfigurationProperties`** — 类型安全的外部化配置绑定，将 YAML/Properties 中的配置映射到 Java Bean
- **`spring-boot-configuration-processor`** — 编译时生成配置元数据，为 IDE 提供自动补全和文档提示
- **`@PostConstruct` / `@PreDestroy`** — JSR-250 生命周期注解，管理 Bean 的初始化和销毁
- **`@Resource`** — JSR-250 标准依赖注入注解，按名称匹配 Bean
- **Lombok `@Data`** — 自动生成 getter/setter/toString/equals/hashCode 方法

### 动态重配设计要点

- **volatile** — 保证线程池引用在多线程间的可见性，重建后其他线程立即可见新实例
- **synchronized** — 保证配置修改的原子性，同一时刻只有一个线程执行重配
- **shutdown() vs shutdownNow()** — 使用 `shutdown()` 优雅关闭旧线程池，确保已提交任务执行完毕
- **全量重建** — `applyConfig` 每次都先更新配置属性再重建线程池，逻辑简单可靠，避免部分更新带来的不一致
