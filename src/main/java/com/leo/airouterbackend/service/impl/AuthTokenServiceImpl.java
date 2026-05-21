package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.auth.JwtClaims;
import com.leo.airouterbackend.auth.JwtUtils;
import com.leo.airouterbackend.config.AuthProperties;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.vo.LoginUserVO;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.utils.AuthPasswordUtils;
import com.leo.airouterbackend.mapper.UserMapper;
import cn.hutool.core.bean.BeanUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String REFRESH_INDEX_PREFIX = "auth:refresh:index:";
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:access:blacklist:";

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private AuthProperties authProperties;

    @Resource
    private RbacService rbacService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserMapper userMapper;

    @Override
    public AuthLoginVO createLoginSession(User user, HttpServletRequest request) {
        List<String> roles = new ArrayList<>(rbacService.getUserRoleCodes(user.getId()));
        String accessToken = jwtUtils.createAccessToken(user, roles);
        String deviceId = IdUtil.simpleUUID();
        String refreshSecret = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
        String refreshToken = deviceId + "." + refreshSecret;
        RefreshSession session = new RefreshSession();
        session.setUserId(user.getId());
        session.setDeviceId(deviceId);
        session.setRefreshTokenHash(jwtUtils.sha256(refreshToken));
        session.setClientIp(request == null ? null : request.getRemoteAddr());
        session.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        session.setCreateTime(Instant.now().getEpochSecond());
        session.setUpdateTime(session.getCreateTime());
        long refreshTtl = authProperties.getJwt().getRefreshTokenTtlSeconds();
        getRefreshMap(user.getId()).put(deviceId, session,
                refreshTtl, TimeUnit.SECONDS);
        redissonClient.<String>getBucket(REFRESH_INDEX_PREFIX + deviceId)
                .set(String.valueOf(user.getId()), refreshTtl, TimeUnit.SECONDS);
        return buildLoginVO(user, accessToken, refreshToken);
    }

    @Override
    public AuthLoginVO refresh(String refreshToken, HttpServletRequest request) {
        RefreshTokenParts parts = parseRefreshToken(refreshToken);
        Long userId = resolveRefreshUserId(parts.deviceId());
        RefreshSession session = userId == null ? null : getRefreshMap(userId).get(parts.deviceId());
        if (session == null || !session.getRefreshTokenHash().equals(jwtUtils.sha256(refreshToken))) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "refreshToken 无效");
        }
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在");
        }
        String accessToken = jwtUtils.createAccessToken(user, new ArrayList<>(rbacService.getUserRoleCodes(user.getId())));
        String newRefreshSecret = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
        String newRefreshToken = parts.deviceId() + "." + newRefreshSecret;
        session.setRefreshTokenHash(jwtUtils.sha256(newRefreshToken));
        session.setClientIp(request == null ? session.getClientIp() : request.getRemoteAddr());
        session.setUserAgent(request == null ? session.getUserAgent() : request.getHeader("User-Agent"));
        session.setUpdateTime(Instant.now().getEpochSecond());
        long refreshTtl = authProperties.getJwt().getRefreshTokenTtlSeconds();
        getRefreshMap(user.getId()).put(parts.deviceId(), session,
                refreshTtl, TimeUnit.SECONDS);
        redissonClient.<String>getBucket(REFRESH_INDEX_PREFIX + parts.deviceId())
                .set(String.valueOf(user.getId()), refreshTtl, TimeUnit.SECONDS);
        return buildLoginVO(user, accessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (StrUtil.isNotBlank(accessToken)) {
            try {
                JwtClaims claims = jwtUtils.parseAccessToken(accessToken);
                long ttl = Math.max(1L, claims.getExpiresAt() - Instant.now().getEpochSecond());
                RBucket<Boolean> bucket = redissonClient.getBucket(ACCESS_BLACKLIST_PREFIX + claims.getJti());
                bucket.set(true, ttl, TimeUnit.SECONDS);
            } catch (BusinessException ignored) {
            }
        }
        if (StrUtil.isNotBlank(refreshToken)) {
            RefreshTokenParts parts = parseRefreshToken(refreshToken);
            Long userId = resolveRefreshUserId(parts.deviceId());
            if (userId != null) {
                getRefreshMap(userId).remove(parts.deviceId());
            }
            redissonClient.getBucket(REFRESH_INDEX_PREFIX + parts.deviceId()).delete();
        }
    }

    @Override
    public JwtClaims parseAndValidateAccessToken(String accessToken) {
        JwtClaims claims = jwtUtils.parseAccessToken(accessToken);
        if (Boolean.TRUE.equals(redissonClient.<Boolean>getBucket(ACCESS_BLACKLIST_PREFIX + claims.getJti()).get())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录凭证已失效");
        }
        return claims;
    }

    private AuthLoginVO buildLoginVO(User user, String accessToken, String refreshToken) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        boolean hasPassword = AuthPasswordUtils.hasPassword(user.getUserPassword());
        loginUserVO.setHasPassword(hasPassword);
        loginUserVO.setNeedSetPassword(!hasPassword);
        return AuthLoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(authProperties.getJwt().getAccessTokenTtlSeconds())
                .loginUser(loginUserVO)
                .build();
    }

    private RMapCache<String, RefreshSession> getRefreshMap(Long userId) {
        return redissonClient.getMapCache(REFRESH_PREFIX + userId);
    }

    private Long resolveRefreshUserId(String deviceId) {
        String userId = redissonClient.<String>getBucket(REFRESH_INDEX_PREFIX + deviceId).get();
        if (StrUtil.isBlank(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            redissonClient.getBucket(REFRESH_INDEX_PREFIX + deviceId).delete();
            return null;
        }
    }

    private RefreshTokenParts parseRefreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken) || !refreshToken.contains(".")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "refreshToken 格式错误");
        }
        String[] parts = refreshToken.split("\\.", 2);
        return new RefreshTokenParts(parts[0], parts[1]);
    }

    private record RefreshTokenParts(String deviceId, String secret) {
    }

    @Data
    public static class RefreshSession implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long userId;
        private String deviceId;
        private String refreshTokenHash;
        private String clientIp;
        private String userAgent;
        private Long createTime;
        private Long updateTime;
    }
}
