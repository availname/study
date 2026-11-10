package com.example.learn.notify;

import com.example.learn.common.ApiResponse;
import com.example.learn.user.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 通知收件箱：MQ 消费者写入（LPUSH+LTRIM 防大 Key），本接口读取（LRANGE） */
@RestController
@RequestMapping("/api/notifications")
public class NotifyController {

    private final StringRedisTemplate redis;

    public NotifyController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping
    public ApiResponse<List<String>> list() {
        long userId = UserContext.get();
        List<String> list = redis.opsForList().range("lc:notif:" + userId, 0, 49);
        return ApiResponse.ok(list == null ? List.of() : list);
    }
}
