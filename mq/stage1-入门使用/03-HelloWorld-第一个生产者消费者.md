# 03 · HelloWorld：第一个生产者消费者

两种玩法：
1. **原生 Java 客户端**——先写一次，理解 AMQP 的底层结构（Connection/Channel），以后看任何 MQ 代码都不慌；
2. **Spring Boot AMQP**——实际工作中的标准写法（demo 工程就是它）。

## 一、原生客户端版

### 依赖

```xml
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.21.0</version>
</dependency>
```

### 生产者

```java
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class Producer {
    public static void main(String[] args) throws Exception {
        // 1. 连接工厂：连接 Broker 的四要素
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        // 2. 建立 TCP 连接（重，全局复用）
        try (Connection connection = factory.newConnection();
             // 3. 在连接上开一个信道（轻，每个线程一个）
             Channel channel = connection.createChannel()) {

            // 4. 声明队列（幂等：不存在则创建，存在则校验参数一致）
            //    queueDeclare(queue, durable, exclusive, autoDelete, arguments)
            channel.queueDeclare("hello.queue", false, false, false, null);

            // 5. 发消息到默认交换机，routing key = 队列名
            String message = "Hello World! " + System.currentTimeMillis();
            channel.basicPublish("", "hello.queue", null,
                    message.getBytes(StandardCharsets.UTF_8));
            System.out.println("已发送: " + message);
        }
    }
}
```

**五个关键点**：
- 消息**不是直接发到队列**，而是发给一个空名字的"默认交换机"，它按 routing key（这里就是队列名）直投队列。为什么要有交换机这一层，Stage 2 细讲。
- `queueDeclare` 是**幂等**的：消费者和生产者都可以声明，先到先创建。
- 消息 = **body（字节数组）** + **properties（属性）**。body 是字节，所以什么都能传（JSON、Protobuf…），约定序列化方式即可。
- Connection 是 TCP，建一次很贵；Channel 是轻量"子信道"，每线程一个。
- try-with-resources 关闭后连接断开——生产环境不会这样写，保持长连接。

### 消费者

```java
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 幂等声明，和生产者参数必须一致
        channel.queueDeclare("hello.queue", false, false, false, null);

        // 异步推送回调
        DeliverCallback deliverCallback = (tag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("收到: " + msg);
            // 确认消费（自动 ack 模式下可省，这里显式写出来帮你建立概念）
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        // basicConsume(queue, autoAck, deliverCallback, cancelCallback)
        channel.basicConsume("hello.queue", false, deliverCallback, tag -> {});
        System.out.println("消费者已启动，等待消息...");
    }
}
```

**推模式**：不是消费者去"拉"，而是 Broker **主动推**给在线的消费者。这是 RabbitMQ 和 Kafka（拉模式）最大的模型差异之一。

**运行**：先启消费者，再跑生产者，看控制台。然后去管理界面 `Queues` → `hello.queue`，观察 Ready/Unacked 数字的变化。

## 二、Spring Boot 版（工作标准写法）

完整可运行工程在 [demo/](../demo/README.md)，核心代码如下。

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    # virtual-host: /order   # 按项目隔离时配置
```

### 声明队列（配置类）

```java
@Configuration
public class RabbitConfig {
    @Bean
    public Queue helloQueue() {
        // durable=true：持久化（重启不丢，原理篇细讲）
        return new Queue("hello.queue", true);
    }
}
```

### 生产者

```java
@RestController
@RequiredArgsConstructor
public class HelloSender {
    private final RabbitTemplate rabbitTemplate;  // Spring Boot 自动装配，直接注入

    @GetMapping("/hello")
    public String send(@RequestParam(defaultValue = "你好，消息队列") String msg) {
        rabbitTemplate.convertAndSend("", "hello.queue", msg);
        // 第三个参数是对象时，默认用 Jackson 序列化成 JSON（demo 里配置了这个 Bean）
        return "已发送: " + msg;
    }
}
```

> `convertAndSend(exchange, routingKey, object)`：发到默认交换机，routingKey 就是队列名。Spring 默认用 JDK 序列化（又慢又不可读），demo 里配置了 `Jackson2JsonMessageConverter`，生产必配。

### 消费者

```java
@Component
@Slf4j
public class HelloReceiver {
    // 队列里有消息就自动回调这个方法，Spring 帮你处理了连接、信道、ack
    @RabbitListener(queues = "hello.queue")
    public void receive(String msg) {
        log.info("收到消息: {}", msg);
    }
}
```

**@RabbitListener 的默认 ack 模式是 AUTO**：方法正常返回 → 自动 ACK；方法抛异常 → 自动 NACK 重回队列（Stage 3 讲这是福也是祸）。

## 三、运行并观察

```bash
cd mq/demo
mvn spring-boot:run
# 浏览器访问 http://localhost:8080/hello?msg=第一条消息
# 观察 Spring Boot 控制台日志
# 再开管理界面 http://localhost:15672 → Queues → hello.queue
```

**消息在管理界面里的生命周期**：

```
发送瞬间：  Ready +1
消费者取走：Ready -1，Unacked +1
处理完ack： Unacked -1（最终 Ready=0，Unacked=0）
```

## 消息结构

一条 AMQP 消息：

```
┌────────────── Properties（属性，KV）──────────────┐
│ deliveryMode: 1(非持久化) / 2(持久化)              │
│ contentType: application/json                     │
│ expiration: "30000"（TTL，毫秒）                   │
│ messageId / correlationId / headers / priority    │
├──────────────── Body（字节数组）───────────────────┤
│ {"orderId":"1001","amount":9900}                  │
└───────────────────────────────────────────────────┘
```

**实践建议**：body 里传 JSON + 自定义 `messageId`（业务唯一 ID），为 Stage 3 的幂等打好基础。

---

**动手任务**：跑通 demo 的 Hello World；然后用管理界面手动往 `hello.queue` 发一条消息，看消费者日志是否打印。理解"生产者和消费者不需要同时在线"——这就是 MQ 解耦的直观体现。

**下一节**：[04-五种工作模式](04-五种工作模式.md)。
