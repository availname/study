package com.bigcenter.shortlink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Base62 编解码往返一致性（12 题一：发号器 → 短码的唯一可逆映射） */
class Base62Test {

    @Test
    void 编解码往返一致_覆盖边界与典型值() {
        long[] samples = {0, 1, 61, 62, 3843, 3844, 238327, 238328, Long.MAX_VALUE};
        for (long n : samples) {
            assertEquals(n, Base62.decode(Base62.encode(n)), "往返不一致: " + n);
        }
        assertEquals("0", Base62.encode(0));
        assertEquals("10", Base62.encode(62), "62 进位：62 → \"10\"");
        assertEquals("zz", Base62.encode(61 * 62 + 61), "3843 = 61*62+61 → \"zz\"（字母表序：数字/大写/小写）");
    }

    @Test
    void 编码长度随数值对数增长_7位覆盖35万亿() {
        // 62^7 ≈ 3.5 万亿——7 位短码足够覆盖（12 题一容量论证）
        assertEquals(7, Base62.encode(3521614606207L).length());
        assertEquals(8, Base62.encode(3521614606208L).length());
    }

    @Test
    void 非法输入拒绝() {
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("a-b"));
        assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
    }
}
