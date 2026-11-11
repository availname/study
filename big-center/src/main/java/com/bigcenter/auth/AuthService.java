package com.bigcenter.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.auth.entity.User;
import com.bigcenter.auth.mapper.UserMapper;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 注册/登录/刷新/登出（java-core/19 全套落地）。
 * 密码：BCrypt cost=10——MD5 是 GPU 彩虹表秒破级，慢哈希把撞库变"拼算力"。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    public AuthService(UserMapper userMapper, JwtService jwt) {
        this.userMapper = userMapper;
        this.jwt = jwt;
    }

    public long register(String username, String rawPassword) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));      // 盐自动生成嵌入哈希串，免管理
        try {
            userMapper.insert(u);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }
        return u.getId();
    }

    /** 登录成功签发双 token（面试时序：access 15min 零查询 + refresh 7d 可撤销） */
    public Map<String, Object> login(String username, String rawPassword) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        // 用户不存在与密码错误同一文案——不给撞库者"该用户名存在"的探测信息
        if (u == null || !encoder.matches(rawPassword, u.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_WRONG);
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("userId", u.getId());
        view.put("accessToken", jwt.signAccess(u.getId()));
        view.put("refreshToken", jwt.issueRefresh(u.getId()));
        return view;
    }

    /** refresh 换新 access（旋转 refresh：旧的同时撤销，降低被盗用窗口） */
    public Map<String, Object> refresh(String refreshToken) {
        Long userId = jwt.verifyRefresh(refreshToken);
        if (userId == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        jwt.revokeRefresh(refreshToken);                 // 旋转：一次性使用
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("accessToken", jwt.signAccess(userId));
        view.put("refreshToken", jwt.issueRefresh(userId));
        return view;
    }

    /** 登出：refresh DEL 即刻生效 + access 进黑名单（撤销剩余 ≤15 分钟窗口） */
    public void logout(String accessToken, String refreshToken) {
        Long uid = UserContext.get();
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwt.revokeRefresh(refreshToken);
        }
        if (accessToken != null && !accessToken.isBlank()) {
            jwt.logout(accessToken, uid == null ? 0L : uid);
        }
    }
}
