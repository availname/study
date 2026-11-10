package com.example.mqmall.seckill;

import com.example.mqmall.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** 秒杀接口（教程 stage4/01） */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /** 模拟 users 个用户并发抢购 skuId=1 */
    @PostMapping("/{skuId}")
    public ApiResponse<Map<String, Object>> seckill(@PathVariable long skuId,
                                                    @RequestParam(defaultValue = "200") int users) {
        return ApiResponse.ok(seckillService.seckill(skuId, users));
    }

    /** 轮询抢购结果 */
    @GetMapping("/result/{orderId}")
    public ApiResponse<SeckillService.SeckillResult> result(@PathVariable long orderId) {
        return ApiResponse.ok(seckillService.result(orderId));
    }

    /** 实时统计：Redis 预扣 vs DB 落库 对账 */
    @GetMapping("/{skuId}/stats")
    public ApiResponse<Map<String, Object>> stats(@PathVariable long skuId) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.putAll(seckillService.redisSnapshot(skuId));
        view.put("排队中", seckillService.queuedCount());
        view.put("结果明细数", seckillService.resultsView().size());
        return ApiResponse.ok(view);
    }

    /** 重置秒杀演示数据 */
    @PostMapping("/reset")
    public ApiResponse<Void> reset() {
        seckillService.reset();
        return ApiResponse.ok(null);
    }
}
