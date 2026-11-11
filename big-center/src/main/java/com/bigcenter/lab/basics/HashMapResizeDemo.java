package com.bigcenter.lab.basics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * HashMap 扩容演示（教程: java-core/00-Java语言基石/01-集合框架）：
 * 容量 16、负载因子 0.75 → size>12 触发扩容翻倍 rehash；
 * 反射看 table.length 变化；同时演示"指定初始容量"避免多次扩容（大 Key 篇性能点）。
 */
public class HashMapResizeDemo {
    private static final Logger log = LoggerFactory.getLogger(HashMapResizeDemo.class);

    public static void main(String[] args) throws Exception {
        Map<Integer, Integer> map = new HashMap<>(16);
        log.info("初始：懒加载，table 尚未分配（第一次 put 才建）");
        for (int i = 1; i <= 20; i++) {
            map.put(i, i);
            if (i == 12) log.info("put 到 {}：size=12（临界值 16*0.75）", i);
            if (i == 13) log.info("put 到 {}：size=13 触发扩容！table.length={}（16→32）",
                    i, tableLength(map));
        }
        log.info("最终 table.length={}", tableLength(map));

        // 预估容量：1000 条数据 → new HashMap<>(2048)（1000/0.75=1334，取 2 的幂 2048）——一次都不扩
        Map<Integer, Integer> sized = new HashMap<>(2048);
        for (int i = 0; i < 1000; i++) sized.put(i, i);
        log.info("预分配 2048 放 1000 条：table.length={}（全程零 rehash）", tableLength(sized));
        log.info("要点：HashMap 容量恒为 2 的幂（hash 均匀分布靠 & (n-1)），扩容翻倍 rehash 是 O(n)——预估容量是免费优化");
    }

    /** 反射读 table.length（教学观察用，生产别这么干） */
    static int tableLength(Map<?, ?> m) throws Exception {
        Field table = HashMap.class.getDeclaredField("table");
        table.setAccessible(true);
        Object[] arr = (Object[]) table.get(m);
        return arr == null ? 0 : arr.length;
    }
}
