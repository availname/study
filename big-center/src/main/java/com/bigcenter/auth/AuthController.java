package com.bigcenter.auth;

import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证接口（/api/auth/** 白名单放行；登出需登录态拿 userId） */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@RequestParam String username, @RequestParam String password) {
        return ApiResponse.ok(authService.register(username, password));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestParam String username, @RequestParam String password) {
        return ApiResponse.ok(authService.login(username, password));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestParam String refreshToken) {
        return ApiResponse.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestParam(required = false) String refreshToken) {
        String accessToken = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok();
    }

    /** 当前登录用户（userId 来自 JWT 验签 → UserContext；越权防护示范：只认登录态身份） */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(Map.of("userId", UserContext.get()));
    }
}
