package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.auth.JwtUtils;
import com.leo.airouterbackend.config.AuthProperties;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenServiceImplTest {

    @Test
    void createLoginSessionWritesDeviceIndexSoRefreshDoesNotNeedKeyScan() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        RbacService rbacService = mock(RbacService.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RMapCache<String, AuthTokenServiceImpl.RefreshSession> refreshMap = mock(RMapCache.class);
        RBucket<String> indexBucket = mock(RBucket.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthProperties authProperties = new AuthProperties();
        authProperties.getJwt().setRefreshTokenTtlSeconds(1800L);

        when(rbacService.getUserRoleCodes(10L)).thenReturn(Set.of("user"));
        when(jwtUtils.createAccessToken(any(User.class), any())).thenReturn("access-token");
        when(redissonClient.<String, AuthTokenServiceImpl.RefreshSession>getMapCache("auth:refresh:10")).thenReturn(refreshMap);
        when(redissonClient.<String>getBucket(startsWith("auth:refresh:index:"))).thenReturn(indexBucket);

        AuthTokenServiceImpl service = new AuthTokenServiceImpl();
        ReflectionTestUtils.setField(service, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(service, "authProperties", authProperties);
        ReflectionTestUtils.setField(service, "rbacService", rbacService);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);

        User user = new User();
        user.setId(10L);
        service.createLoginSession(user, request);

        verify(indexBucket).set(eq("10"), eq(1800L), eq(TimeUnit.SECONDS));
    }
}
