package com.bigcenter.shortlink;

import com.bigcenter.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * 短链接口（java-core/12 题一）。
 * 跳转用 302 而非 301：301 浏览器永久缓存不再回源，统计就丢了；302 每次回源可计数。
 */
@RestController
public class ShortLinkController {

    private final ShortLinkService service;

    public ShortLinkController(ShortLinkService service) {
        this.service = service;
    }

    /** 发号（需登录态——/api/** 被 JWT 拦截器保护） */
    @PostMapping("/api/shortlink")
    public ApiResponse<Map<String, Object>> create(@RequestParam String url) {
        String code = service.create(url);
        return ApiResponse.ok(Map.of("shortCode", code, "shortUrl", "http://localhost:8085/s/" + code));
    }

    /** 点击统计（登录态） */
    @GetMapping("/api/shortlink/{code}/stats")
    public ApiResponse<Map<String, Object>> stats(@PathVariable String code) {
        return ApiResponse.ok(Map.of("shortCode", code, "clickCount", service.clickCount(code)));
    }

    /** 302 跳转（/s/** 不在 /api/** 下，天然免登录） */
    @GetMapping("/s/{code}")
    public void redirect(@PathVariable String code, HttpServletResponse resp) throws IOException {
        String longUrl = service.resolve(code);
        if (longUrl == null) {
            resp.sendError(404, "短链不存在");
            return;
        }
        resp.sendRedirect(longUrl);     // 302
    }
}
