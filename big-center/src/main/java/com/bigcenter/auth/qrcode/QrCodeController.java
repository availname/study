package com.bigcenter.auth.qrcode;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 扫码登录接口（java-core/12 题二）：
 * create/poll 由 PC 调（免登录——uuid 本身就是一次性凭据）；
 * scan/confirm 由手机 App 调（必须带 App 登录态 JWT）。
 */
@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {

    private final QrCodeService service;

    public QrCodeController(QrCodeService service) {
        this.service = service;
    }

    /** ① PC 生成二维码请求 */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create() {
        return ApiResponse.ok(service.create());
    }

    /** ② 手机扫码（App 登录态） */
    @PostMapping("/{uuid}/scan")
    public ApiResponse<Void> scan(@PathVariable String uuid) {
        if (!service.scan(uuid, UserContext.get())) {
            return ApiResponse.error(1004, "二维码无效或已过期");
        }
        return ApiResponse.ok();
    }

    /** ③ 手机确认登录（只有扫码设备可确认） */
    @PostMapping("/{uuid}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String uuid) {
        if (!service.confirm(uuid, UserContext.get())) {
            return ApiResponse.error(1004, "二维码状态不允许确认");
        }
        return ApiResponse.ok();
    }

    /** ④ PC 轮询（1~2s 一次；CONFIRMED 返回双 token） */
    @GetMapping("/{uuid}/status")
    public ApiResponse<Map<String, Object>> status(@PathVariable String uuid) {
        return ApiResponse.ok(service.poll(uuid));
    }
}
