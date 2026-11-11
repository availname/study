package com.bigcenter.auth;

import com.bigcenter.auth.entity.User;
import com.bigcenter.auth.mapper.UserMapper;
import com.bigcenter.common.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 注册/登录业务：BCrypt 慢哈希、错误文案防探测、refresh 旋转——java-core/19 验收 */
class AuthServiceTest {

    private UserMapper userMapper;
    private JwtService jwt;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userMapper = Mockito.mock(UserMapper.class);
        jwt = Mockito.mock(JwtService.class);
        authService = new AuthService(userMapper, jwt);
    }

    @Test
    void 注册时密码以BCrypt摘要入库_绝无明文() {
        // MP insert 回填自增 ID：mock 里用 thenAnswer 模拟
        when(userMapper.insert(Mockito.<User>any())).thenAnswer(inv -> {
            inv.getArgument(0, User.class).setId(1001L);
            return 1;
        });
        authService.register("tom", "p@ss123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        String stored = captor.getValue().getPassword();
        assertNotEquals("p@ss123", stored, "绝不能明文入库");
        assertTrue(new BCryptPasswordEncoder().matches("p@ss123", stored), "BCrypt 摘要必须可校验");
        assertTrue(stored.startsWith("$2"), "BCrypt 哈希串特征 $2");
    }

    @Test
    void 用户名重复_抛业务异常1002() {
        when(userMapper.insert(Mockito.<User>any()))
                .thenThrow(new DuplicateKeyException("uk_username"));
        BizException e = assertThrows(BizException.class, () -> authService.register("tom", "x"));
        assertEquals(1002, e.getErrorCode().code());
    }

    @Test
    void 用户不存在与密码错误_同一文案_不给撞库探测信息() {
        // 用户不存在
        when(userMapper.selectOne(any())).thenReturn(null);
        BizException e1 = assertThrows(BizException.class, () -> authService.login("ghost", "x"));
        // 用户存在但密码错
        User u = new User();
        u.setId(1001L);
        u.setUsername("tom");
        u.setPassword(new BCryptPasswordEncoder(4).encode("right"));
        when(userMapper.selectOne(any())).thenReturn(u);
        BizException e2 = assertThrows(BizException.class, () -> authService.login("tom", "wrong"));
        assertEquals(e1.getMessage(), e2.getMessage(), "两种失败必须同文案");
        assertEquals(1003, e2.getErrorCode().code());
    }

    @Test
    void 登录成功_签发access与refresh双token() {
        User u = new User();
        u.setId(1001L);
        u.setUsername("tom");
        u.setPassword(new BCryptPasswordEncoder(4).encode("right"));
        when(userMapper.selectOne(any())).thenReturn(u);
        when(jwt.signAccess(1001L)).thenReturn("access-token");
        when(jwt.issueRefresh(1001L)).thenReturn("refresh-token");

        var view = authService.login("tom", "right");
        assertEquals("access-token", view.get("accessToken"));
        assertEquals("refresh-token", view.get("refreshToken"));
        // Long 装箱坑：signAccess 参数是 long 1001L
        verify(jwt).signAccess(1001L);
        verify(jwt).issueRefresh(1001L);
    }

    @Test
    void refresh旋转_旧token先撤销再发新对() {
        when(jwt.verifyRefresh("old-r")).thenReturn(1001L);
        when(jwt.signAccess(1001L)).thenReturn("new-a");
        when(jwt.issueRefresh(1001L)).thenReturn("new-r");

        authService.refresh("old-r");
        // 旋转语义：旧 refresh 必须被撤销（一次性使用，被盗窗口最小化）
        verify(jwt).revokeRefresh("old-r");
        verify(jwt).signAccess(1001L);
    }

    @Test
    void refresh无效_抛1004_且不做任何签发() {
        when(jwt.verifyRefresh("bad-r")).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> authService.refresh("bad-r"));
        assertEquals(1004, e.getErrorCode().code());
        verify(jwt, Mockito.never()).signAccess(Mockito.anyLong());
    }
}
