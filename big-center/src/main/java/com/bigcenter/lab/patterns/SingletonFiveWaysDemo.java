package com.bigcenter.lab.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单例五种写法演示（教程: java-core/06-设计模式/01-创建型）：
 * 饿汉/懒汉(DCL)/静态内部类/枚举/Spring 容器单例——各自适用场景与坑。
 */
public class SingletonFiveWaysDemo {
    private static final Logger log = LoggerFactory.getLogger(SingletonFiveWaysDemo.class);

    /** ① 饿汉：类加载即建——简单，但创建成本高时会拖慢启动 */
    static class Eager {
        private static final Eager INSTANCE = new Eager();
        static Eager get() { return INSTANCE; }
    }

    /** ③ 静态内部类：懒加载 + 线程安全（JVM 类加载机制保证）——推荐 */
    static class InnerHolder {
        private InnerHolder() {}
        private static class Holder { static final InnerHolder INSTANCE = new InnerHolder(); }
        static InnerHolder get() { return Holder.INSTANCE; }
    }

    /** ④ 枚举：防反射/防序列化破坏——Effective Java 首选 */
    enum EnumSingleton {
        INSTANCE;
        int state;
    }

    /** ② 懒汉 DCL 见 {@link com.bigcenter.lab.concurrent.VolatileDclDemo}（volatile 必须的对照组） */

    public static void main(String[] args) throws Exception {
        log.info("① 饿汉：{}（类加载即初始化）", Eager.get() == Eager.get());
        log.info("③ 静态内部类：{}（首次 get 才加载 Holder——懒加载+免锁）", InnerHolder.get() == InnerHolder.get());
        EnumSingleton e1 = EnumSingleton.INSTANCE;
        EnumSingleton e2 = EnumSingleton.valueOf("INSTANCE");
        // 反射破坏演示：普通类可 setAccessible 调私有构造；枚举被 JVM 保护
        try {
            var ctor = InnerHolder.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ctor.newInstance();
            log.info("④ 反射创建了新实例！枚举没这个洞");
        } catch (Exception ex) {
            log.info("④ 反射攻击被挡：{}", ex.getCause().getClass().getSimpleName());
        }
        log.info("④ 枚举：{}（valueOf/反射都归一）", e1 == e2);
        log.info("⑤ Spring 单例：容器 scope=singleton——业务项目里 @Component 即最常用单例，big-center 全部 Service 都靠它");
    }
}
