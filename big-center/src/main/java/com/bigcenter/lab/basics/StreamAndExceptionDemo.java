package com.bigcenter.lab.basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stream 与异常实践演示（教程: java-core/00-Java语言基石/03-Stream与Lambda、04-异常体系与最佳实践）：
 * Stream 三段式（数据源→惰性中间操作→终端触发）+ 分组聚合（SQL group by 的内存版）；
 * 异常：受检异常别吞、业务异常带错误码（big-center BizException+ErrorCode 的理论根基）。
 */
public class StreamAndExceptionDemo {
    private static final Logger log = LoggerFactory.getLogger(StreamAndExceptionDemo.class);

    record Course(String title, int priceFen, String teacher) {}

    public static void main(String[] args) {
        List<Course> courses = List.of(
                new Course("并发入门", 9900, "张三"),
                new Course("JVM 深入", 19900, "张三"),
                new Course("Redis 实战", 12900, "李四"),
                new Course("MySQL 调优", 0, "李四"));

        // ① 惰性求值：中间操作不执行，count() 触发
        long paid = courses.stream()
                .filter(c -> c.priceFen() > 0)      // 中间：惰性
                .count();                            // 终端：触发
        log.info("① filter+count：付费课 {} 门", paid);

        // ② 分组聚合（SQL group by teacher sum(price) 的内存版）
        Map<String, Long> revenue = courses.stream()
                .collect(Collectors.groupingBy(Course::teacher,
                        Collectors.summingLong(Course::priceFen)));
        log.info("② 按讲师分组营收：{}", revenue);

        // ③ 金额用 BigDecimal/long 分——double 0.1+0.2≠0.3（支付篇铁律的基石版）
        log.info("③ double 陷阱：0.1+0.2={} ≠0.3；BigDecimal: {}",
                0.1 + 0.2, BigDecimal.valueOf(1).add(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(10)));

        // ④ 异常最佳实践：try-with-resources + 业务异常带错误码
        try (var res = new AutoCloseable() {
            @Override public void close() { log.info("④ 资源已自动关闭（finally 手写易漏）"); }
        }) {
            throw new IllegalArgumentException("演示异常");
        } catch (IllegalArgumentException e) {
            log.info("④ 捕获后处理：{}（业务异常→BizException(ErrorCode)，别吞栈）", e.getMessage());
        }
    }
}
