package com.bigcenter.lab.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 动态代理演示（教程: java-core/06-设计模式/02-结构型）：
 * JDK 动态代理（接口 + 反射 InvocationHandler）——Spring AOP、MyBatis Mapper、
 * Feign 客户端全部是它的应用；big-center 的 @RateLimit 切面本质就是代理织入。
 */
public class ProxyDemo {
    private static final Logger log = LoggerFactory.getLogger(ProxyDemo.class);

    interface PayService {
        String pay(String orderNo, long amountFen);
    }

    static class RealPayService implements PayService {
        @Override public String pay(String orderNo, long amountFen) {
            return orderNo + " 支付 " + amountFen + " 分";
        }
    }

    public static void main(String[] args) {
        // JDK 动态代理：运行时生成 $Proxy0 实现 PayService，方法调用全部转发到 handler
        PayService proxied = (PayService) Proxy.newProxyInstance(
                PayService.class.getClassLoader(),
                new Class<?>[]{PayService.class},
                (Object proxy, Method method, Object[] argv) -> {
                    long start = System.nanoTime();
                    log.info("前置：方法={} 参数={}（耗时统计/事务开启/限流判断都在这里）",
                            method.getName(), argv);
                    Object result = method.invoke(new RealPayService(), argv);   // 委托真实对象
                    log.info("后置：耗时 {}ns", System.nanoTime() - start);
                    return result;
                });

        log.info("调用结果：{}", proxied.pay("P1001", 9900));
        log.info("代理类名：{}（运行时生成——Spring AOP 的 JDK 代理即此机制）", proxied.getClass().getName());
        log.info("对照：CGLib 用字节码生成子类（无需接口）；Spring Boot 默认 proxyTargetClass=true 用 CGLib");
    }
}
