package com.leo.airouterbackend.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceImplTest {

    @Test
    void tryAcquireUsesExpiringCounterInsteadOfPersistentLimiterMetadata() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate:ip:127.0.0.1")).thenReturn(1L, 2L, 3L);

        RateLimitServiceImpl service = new RateLimitServiceImpl();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);

        assertTrue(service.tryAcquire("rate:ip:127.0.0.1", 2, Duration.ofSeconds(1)));
        assertTrue(service.tryAcquire("rate:ip:127.0.0.1", 2, Duration.ofSeconds(1)));
        assertFalse(service.tryAcquire("rate:ip:127.0.0.1", 2, Duration.ofSeconds(1)));
        verify(redisTemplate).expire(eq("rate:ip:127.0.0.1"), eq(1L), eq(TimeUnit.SECONDS));
    }
}
