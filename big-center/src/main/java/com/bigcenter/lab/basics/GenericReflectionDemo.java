package com.bigcenter.lab.basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * 泛型擦除与反射演示（教程: java-core/00-Java语言基石/02-泛型反射与注解）：
 * 泛型只存在于编译期（擦除），但字段/方法签名上的泛型保留在 class 元数据里——反射能拿到；
 * 注解同理：运行时注解是框架装配（Spring @Autowired）的根基。
 */
public class GenericReflectionDemo {
    private static final Logger log = LoggerFactory.getLogger(GenericReflectionDemo.class);

    @Retention(RetentionPolicy.RUNTIME)         // 运行时保留：反射才读得到
    @Target(ElementType.FIELD)
    @interface Inject {}

    static class Box {
        @Inject
        private List<String> names;             // 字段泛型 List<String> 留在元数据
        private transient int cachedHash;       // transient：序列化排除标记
    }

    public static void main(String[] args) throws Exception {
        // ① 擦除证明：List<String> 和 List<Integer> 运行时同一个 Class
        log.info("① 擦除：List<String>.class == List<Integer>.class 运行时都是 {}",
                java.util.Collections.singletonList("").getClass() ==
                        java.util.Collections.singletonList(1).getClass());

        // ② 反射读字段泛型（MyBatis-Plus 解析实体、Jackson 反序列化就是这么干的）
        Field f = Box.class.getDeclaredField("names");
        ParameterizedType pt = (ParameterizedType) f.getGenericType();
        log.info("② 字段泛型：原始类型={} 泛型参数={}（擦除后仍可从元数据还原）",
                pt.getRawType(), pt.getActualTypeArguments()[0]);

        // ③ 运行时注解扫描：框架"注解驱动"的本质
        for (Field field : Box.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                log.info("③ 扫描到 @Inject 字段：{}（Spring 依赖注入=扫描+反射赋值）", field.getName());
            }
        }
    }
}
