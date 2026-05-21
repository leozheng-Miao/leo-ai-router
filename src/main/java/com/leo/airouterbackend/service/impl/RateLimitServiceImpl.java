package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.service.RateLimitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * API Key 限流前缀
     */
    private static final String RATE_LIMIT_API_KEY_PREFIX = "rate:api_key:";

    /**
     * IP 限流前缀
     */
    private static final String RATE_LIMIT_IP_PREFIX = "rate:ip:";

    @Override
    public boolean tryAcquire(String key, int limit, Duration duration) {
        if (limit <= 0 || key == null || key.isBlank()) {
            return false;
        }
        Duration safeDuration = duration == null || duration.isZero() || duration.isNegative()
                ? Duration.ofSeconds(1)
                : duration;
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current == null) {
            log.warn("Rate limit counter increment returned null, key={}", key);
            return false;
        }
        if (current == 1L) {
            stringRedisTemplate.expire(key, Math.max(1L, safeDuration.toSeconds()), TimeUnit.SECONDS);
        }
        boolean allowed = current <= limit;
        if (!allowed) {
            log.debug("Rate limit exceeded for key: {}", key);
        }
        return allowed;
    }

    @Override
    public long getAvailablePermits(String key) {
        return 0;
    }

    @Override
    public boolean checkApiKeyRateLimit(String apiKey, int limit) {
        String key = RATE_LIMIT_API_KEY_PREFIX + sha256(apiKey);
        return tryAcquire(key, limit, Duration.ofSeconds(1));
    }

    @Override
    public boolean checkIpRateLimit(String ip, int limit) {
        String key = RATE_LIMIT_IP_PREFIX + ip;
        return tryAcquire(key, limit, Duration.ofSeconds(1));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
