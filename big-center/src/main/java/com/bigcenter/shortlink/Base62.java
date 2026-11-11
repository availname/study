package com.bigcenter.shortlink;

/** Base62 编码（java-core/12 题一：发号器自增 ID → 短码，天然无碰撞） */
public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private Base62() {}

    public static String encode(long num) {
        if (num < 0) throw new IllegalArgumentException("只支持非负数: " + num);
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(ALPHABET[(int) (num % 62)]);
            num /= 62;
        } while (num > 0);
        return sb.reverse().toString();
    }

    public static long decode(String code) {
        long num = 0;
        for (char c : code.toCharArray()) {
            int d;
            if (c >= '0' && c <= '9') d = c - '0';
            else if (c >= 'A' && c <= 'Z') d = c - 'A' + 10;
            else if (c >= 'a' && c <= 'z') d = c - 'a' + 36;
            else throw new IllegalArgumentException("非法短码字符: " + c);
            num = num * 62 + d;
        }
        return num;
    }
}
