package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.entity.rbac.RolePlanLimit;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.UsageLimitService;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;

@Service
public class UsageLimitServiceImpl implements UsageLimitService {

    @Resource
    private RbacService rbacService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void checkAndRecordChat(Long userId) {
        if (userId == null) {
            return;
        }
        RolePlanLimit limit = rbacService.getBestPlanLimit(userId);
        checkAndIncrement("usage:daily:chat:" + userId + ":" + LocalDate.now(), limit.getDailyRequestLimit(), secondsUntilEndOfDay(), "今日对话次数已用尽");
        checkAndIncrement("usage:monthly:chat:" + userId + ":" + YearMonth.now(), limit.getMonthlyRequestLimit(), secondsUntilEndOfMonth(), "本月对话次数已用尽");
    }

    @Override
    public void checkAndRecordImage(Long userId) {
        if (userId == null) {
            return;
        }
        checkAndIncrement("usage:daily:image:" + userId + ":" + LocalDate.now(),
                rbacService.getBestPlanLimit(userId).getDailyImageLimit(), secondsUntilEndOfDay(), "今日图片生成次数已用尽");
    }

    @Override
    public void checkAndRecordPlugin(Long userId) {
        if (userId == null) {
            return;
        }
        checkAndIncrement("usage:daily:plugin:" + userId + ":" + LocalDate.now(),
                rbacService.getBestPlanLimit(userId).getDailyPluginLimit(), secondsUntilEndOfDay(), "今日插件使用次数已用尽");
    }

    @Override
    public void checkApiKeyCreate(Long userId, long currentApiKeyCount) {
        if (userId == null) {
            return;
        }
        Integer apiKeyLimit = rbacService.getBestPlanLimit(userId).getApiKeyLimit();
        if (apiKeyLimit != null && apiKeyLimit >= 0 && currentApiKeyCount >= apiKeyLimit) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 数量已达到当前角色上限");
        }
    }

    @Override
    public void checkByokAllowed(Long userId) {
        if (userId == null) {
            return;
        }
        Integer allowByok = rbacService.getBestPlanLimit(userId).getAllowByok();
        if (allowByok == null || allowByok != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前角色不允许配置 BYOK");
        }
    }

    private void checkAndIncrement(String key, Long limit, long ttlSeconds, String message) {
        if (limit == null || limit == -1L) {
            return;
        }
        if (limit <= 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, message);
        }
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long value = counter.incrementAndGet();
        if (value == 1) {
            counter.expire(ttlSeconds, TimeUnit.SECONDS);
        }
        if (value > limit) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, message);
        }
    }

    private long secondsUntilEndOfDay() {
        return Math.max(1L, Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now(), LocalTime.MAX)).toSeconds());
    }

    private long secondsUntilEndOfMonth() {
        YearMonth now = YearMonth.now();
        return Math.max(1L, Duration.between(LocalDateTime.now(), now.atEndOfMonth().atTime(LocalTime.MAX)).toSeconds());
    }
}
