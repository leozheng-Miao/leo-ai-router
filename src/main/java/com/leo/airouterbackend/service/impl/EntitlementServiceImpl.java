package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.model.entity.SubscriptionPlan;
import com.leo.airouterbackend.model.entity.UserSubscription;
import com.leo.airouterbackend.service.EntitlementService;
import com.leo.airouterbackend.service.MembershipService;
import com.leo.airouterbackend.service.ModelProviderService;
import com.leo.airouterbackend.service.PointService;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@Service
public class EntitlementServiceImpl implements EntitlementService {

    private static final String TIER_FREE = "free";
    private static final String TIER_PRO = "pro";
    private static final String TIER_ADVANCED = "advanced";
    private static final String TIER_IMAGE = "image";

    @Resource
    private MembershipService membershipService;

    @Resource
    private PointService pointService;

    @Resource
    private ModelProviderService modelProviderService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void checkChatAccess(Long userId, Model model) {
        if (userId == null || model == null) {
            return;
        }
        String tier = normalizeTier(model);
        if (TIER_IMAGE.equals(tier) || "video".equals(tier)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该模型不能用于聊天");
        }
        SubscriptionPlan plan = currentPlan(userId);
        if (TIER_ADVANCED.equals(tier)) {
            checkLimit(getTodayUsed(userId, TIER_ADVANCED), plan.getDailyAdvancedLimit(), "今日高级模型聊天次数已用尽");
        } else {
            checkLimit(getTodayUsed(userId, TIER_PRO), plan.getDailyProLimit(), "今日普通模型聊天次数已用尽");
        }
    }

    @Override
    public boolean canUseChatAccess(Long userId, Model model) {
        try {
            checkChatAccess(userId, model);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    @Override
    public void recordChatUsage(Long userId, Model model) {
        if (userId == null || model == null) {
            return;
        }
        String tier = TIER_ADVANCED.equals(normalizeTier(model)) ? TIER_ADVANCED : TIER_PRO;
        RAtomicLong counter = redissonClient.getAtomicLong(dailyChatKey(userId, tier));
        long value = counter.incrementAndGet();
        if (value == 1) {
            counter.expire(secondsUntilEndOfDay(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void checkImagePoints(Long userId, Model model, int count) {
        if (userId == null) {
            return;
        }
        checkImageAccess(userId, model);
        long cost = calcPointCost(model, count);
        if (cost <= 0) {
            return;
        }
        long balance = pointService.getBalance(userId);
        if (balance < cost) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足，当前积分：" + balance + "，需要：" + cost);
        }
    }

    @Override
    public void checkImageAccess(Long userId, Model model) {
        if (userId == null || model == null) {
            return;
        }
        if (!requiresPaidSubscription(model)) {
            return;
        }
        UserSubscription subscription = membershipService.getActiveSubscription(userId);
        if (subscription == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该图片模型仅限会员使用，请先开通套餐");
        }
    }

    @Override
    public void deductImagePoints(Long userId, Model model, int count, Long refId) {
        if (userId == null) {
            return;
        }
        long cost = calcPointCost(model, count);
        if (cost > 0) {
            pointService.consumePoints(userId, cost, "image_consume", "image_generation_record", refId,
                    "图片生成消耗：" + model.getModelKey() + " x " + count);
        }
    }

    @Override
    public void checkApiKeyCreate(Long userId, long currentApiKeyCount) {
        SubscriptionPlan plan = currentPlan(userId);
        Integer limit = plan.getApiKeyLimit();
        if (limit != null && limit >= 0 && currentApiKeyCount >= limit) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 数量已达到当前套餐上限");
        }
    }

    @Override
    public void checkByokAllowed(Long userId) {
        SubscriptionPlan plan = currentPlan(userId);
        if (plan.getAllowByok() == null || plan.getAllowByok() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前套餐不允许配置 BYOK");
        }
    }

    @Override
    public long getTodayUsed(Long userId, String tier) {
        return redissonClient.getAtomicLong(dailyChatKey(userId, tier)).get();
    }

    private SubscriptionPlan currentPlan(Long userId) {
        UserSubscription subscription = membershipService.getActiveSubscription(userId);
        return membershipService.getActivePlan(subscription == null ? TIER_FREE : subscription.getPlanCode());
    }

    private String normalizeTier(Model model) {
        String tier = StrUtil.blankToDefault(model.getAccessTier(), TIER_FREE).toLowerCase();
        if (TIER_IMAGE.equals(tier) || "video".equals(tier) || TIER_ADVANCED.equals(tier) || TIER_PRO.equals(tier)) {
            return tier;
        }
        return TIER_FREE;
    }

    private void checkLimit(long used, Long limit, String message) {
        if (limit == null || limit == -1L) {
            return;
        }
        if (limit <= 0 || used >= limit) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, message);
        }
    }

    private long calcPointCost(Model model, int count) {
        int safeCount = Math.max(1, count);
        int pointCost = model == null || model.getPointCost() == null ? 0 : model.getPointCost();
        return (long) pointCost * safeCount;
    }

    private boolean requiresPaidSubscription(Model model) {
        if (model == null || !"image".equals(model.getModelType())) {
            return false;
        }
        String modelKey = StrUtil.blankToDefault(model.getModelKey(), "").toLowerCase();
        if (modelKey.contains("gpt") || modelKey.contains("gemini")) {
            return true;
        }
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        String providerName = provider == null ? "" : StrUtil.blankToDefault(provider.getProviderName(), "").toLowerCase();
        return providerName.contains("openai") || providerName.contains("gemini") || providerName.contains("google");
    }

    private String dailyChatKey(Long userId, String tier) {
        return "usage:daily:chat-tier:" + userId + ":" + tier + ":" + LocalDate.now();
    }

    private long secondsUntilEndOfDay() {
        return Math.max(1L, Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now(), LocalTime.MAX)).toSeconds());
    }
}
