package com.bigcenter.redpack;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import com.bigcenter.redpack.entity.RedPackDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 红包接口（/api/redpack/**，JWT 保护） */
@RestController
@RequestMapping("/api/redpack")
public class RedPackController {

    private final RedPackService service;

    public RedPackController(RedPackService service) {
        this.service = service;
    }

    /** 发红包（金额单位：分） */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestParam long totalFen, @RequestParam int count) {
        long id = service.create(totalFen, count);
        return ApiResponse.ok(Map.of("packId", id));
    }

    /** 抢红包 */
    @PostMapping("/{packId}/grab")
    public ApiResponse<Map<String, Object>> grab(@PathVariable long packId) {
        Long amount = service.grab(packId, UserContext.get());
        return ApiResponse.ok(Map.of("packId", packId, "amountFen", amount));
    }

    /** 拆分明细（发红包人查看手气） */
    @GetMapping("/{packId}/details")
    public ApiResponse<List<RedPackDetail>> details(@PathVariable long packId) {
        return ApiResponse.ok(service.details(packId));
    }
}
