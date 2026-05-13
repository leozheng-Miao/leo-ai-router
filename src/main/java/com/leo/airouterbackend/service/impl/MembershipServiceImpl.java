package com.leo.airouterbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.PointPackageMapper;
import com.leo.airouterbackend.mapper.SubscriptionPlanMapper;
import com.leo.airouterbackend.mapper.UserSubscriptionMapper;
import com.leo.airouterbackend.model.dto.membership.PointPackageSaveRequest;
import com.leo.airouterbackend.model.dto.membership.SubscriptionPlanSaveRequest;
import com.leo.airouterbackend.model.dto.membership.UserSubscriptionAdjustRequest;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.PointPackage;
import com.leo.airouterbackend.model.entity.SubscriptionPlan;
import com.leo.airouterbackend.model.entity.UserSubscription;
import com.leo.airouterbackend.model.vo.MembershipVO;
import com.leo.airouterbackend.service.MembershipService;
import com.leo.airouterbackend.service.PointService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MembershipServiceImpl extends ServiceImpl<SubscriptionPlanMapper, SubscriptionPlan> implements MembershipService {

    @Resource
    private SubscriptionPlanMapper subscriptionPlanMapper;

    @Resource
    private PointPackageMapper pointPackageMapper;

    @Resource
    private UserSubscriptionMapper userSubscriptionMapper;

    @Resource
    private PointService pointService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public List<SubscriptionPlan> listVisiblePlans() {
        return subscriptionPlanMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "active")
                .eq("visible", 1)
                .eq("isDelete", 0)
                .orderBy("priority", false));
    }

    @Override
    public List<PointPackage> listVisiblePointPackages() {
        return pointPackageMapper.selectListByQuery(QueryWrapper.create()
                .eq("status", "active")
                .eq("visible", 1)
                .eq("isDelete", 0)
                .orderBy("priority", false));
    }

    @Override
    public SubscriptionPlan getActivePlan(String planCode) {
        if (StrUtil.isBlank(planCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SubscriptionPlan plan = subscriptionPlanMapper.selectOneByQuery(QueryWrapper.create()
                .eq("planCode", planCode)
                .eq("status", "active")
                .eq("isDelete", 0));
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "套餐不存在或已下架");
        }
        return plan;
    }

    @Override
    public PointPackage getActivePointPackage(String packageCode) {
        if (StrUtil.isBlank(packageCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PointPackage pointPackage = pointPackageMapper.selectOneByQuery(QueryWrapper.create()
                .eq("packageCode", packageCode)
                .eq("status", "active")
                .eq("isDelete", 0));
        if (pointPackage == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "积分包不存在或已下架");
        }
        return pointPackage;
    }

    @Override
    public boolean savePlan(SubscriptionPlanSaveRequest request) {
        if (request == null || StrUtil.isBlank(request.getPlanCode()) || StrUtil.isBlank(request.getPlanName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        BeanUtil.copyProperties(request, plan);
        plan.setStatus(StrUtil.blankToDefault(plan.getStatus(), "active"));
        plan.setVisible(plan.getVisible() == null ? 1 : plan.getVisible());
        plan.setPriority(plan.getPriority() == null ? 0 : plan.getPriority());
        plan.setUpdateTime(LocalDateTime.now());
        if (plan.getId() == null) {
            plan.setCreateTime(LocalDateTime.now());
            plan.setIsDelete(0);
            return subscriptionPlanMapper.insert(plan) > 0;
        }
        return subscriptionPlanMapper.update(plan) > 0;
    }

    @Override
    public boolean savePointPackage(PointPackageSaveRequest request) {
        if (request == null || StrUtil.isBlank(request.getPackageCode()) || StrUtil.isBlank(request.getPackageName())
                || request.getPoints() == null || request.getPoints() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PointPackage pointPackage = new PointPackage();
        BeanUtil.copyProperties(request, pointPackage);
        pointPackage.setStatus(StrUtil.blankToDefault(pointPackage.getStatus(), "active"));
        pointPackage.setVisible(pointPackage.getVisible() == null ? 1 : pointPackage.getVisible());
        pointPackage.setPriority(pointPackage.getPriority() == null ? 0 : pointPackage.getPriority());
        pointPackage.setUpdateTime(LocalDateTime.now());
        if (pointPackage.getId() == null) {
            pointPackage.setCreateTime(LocalDateTime.now());
            pointPackage.setIsDelete(0);
            return pointPackageMapper.insert(pointPackage) > 0;
        }
        return pointPackageMapper.update(pointPackage) > 0;
    }

    @Override
    public UserSubscription getActiveSubscription(Long userId) {
        if (userId == null) {
            return null;
        }
        return userSubscriptionMapper.selectOneByQuery(QueryWrapper.create()
                .eq("userId", userId)
                .eq("status", "active")
                .eq("isDelete", 0)
                .and("(lifetime = 1 OR endTime IS NULL OR endTime > NOW())")
                .orderBy("priority", false)
                .orderBy("endTime", false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateSubscription(PaymentOrder order) {
        SubscriptionPlan plan = getActivePlan(order.getProductCode());
        UserSubscription current = getActiveSubscription(order.getUserId());
        LocalDateTime now = LocalDateTime.now();

        if (current != null && current.getPlanCode().equals(plan.getPlanCode())) {
            current.setPlanName(plan.getPlanName());
            current.setPriority(plan.getPriority());
            current.setLifetime(plan.getLifetime());
            current.setPaymentOrderId(order.getId());
            current.setStatus("active");
            current.setEndTime(calcEndTime(plan, current.getEndTime() != null && current.getEndTime().isAfter(now) ? current.getEndTime() : now));
            current.setUpdateTime(now);
            userSubscriptionMapper.update(current);
        } else {
            if (current != null && (plan.getPriority() == null ? 0 : plan.getPriority()) >= (current.getPriority() == null ? 0 : current.getPriority())) {
                current.setStatus("expired");
                current.setUpdateTime(now);
                userSubscriptionMapper.update(current);
            }
            UserSubscription subscription = UserSubscription.builder()
                    .userId(order.getUserId())
                    .planCode(plan.getPlanCode())
                    .planName(plan.getPlanName())
                    .priority(plan.getPriority())
                    .startTime(now)
                    .endTime(calcEndTime(plan, now))
                    .lifetime(plan.getLifetime())
                    .status("active")
                    .paymentOrderId(order.getId())
                    .createTime(now)
                    .updateTime(now)
                    .isDelete(0)
                    .build();
            userSubscriptionMapper.insert(subscription);
        }

        if (plan.getBonusPoints() != null && plan.getBonusPoints() > 0) {
            pointService.grantPoints(order.getUserId(), plan.getBonusPoints(), "plan_bonus",
                    "payment_order", order.getId(), "购买套餐赠送积分：" + plan.getPlanName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustUserSubscription(UserSubscriptionAdjustRequest request) {
        if (request == null || request.getUserId() == null || StrUtil.isBlank(request.getPlanCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SubscriptionPlan plan = getActivePlan(request.getPlanCode());
        UserSubscription current = getActiveSubscription(request.getUserId());
        if (current != null) {
            current.setStatus("expired");
            current.setUpdateTime(LocalDateTime.now());
            userSubscriptionMapper.update(current);
        }
        LocalDateTime now = LocalDateTime.now();
        UserSubscription subscription = UserSubscription.builder()
                .userId(request.getUserId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .priority(plan.getPriority())
                .startTime(now)
                .endTime(request.getEndTime() != null ? request.getEndTime() : calcEndTime(plan, now))
                .lifetime(request.getLifetime() != null ? request.getLifetime() : plan.getLifetime())
                .status("active")
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();
        userSubscriptionMapper.insert(subscription);
    }

    @Override
    public MembershipVO getMyMembership(Long userId) {
        UserSubscription subscription = getActiveSubscription(userId);
        SubscriptionPlan plan = subscription == null ? getActivePlan("free") : getActivePlan(subscription.getPlanCode());
        MembershipVO vo = new MembershipVO();
        vo.setPlanCode(plan.getPlanCode());
        vo.setPlanName(plan.getPlanName());
        if (subscription != null) {
            vo.setStartTime(subscription.getStartTime());
            vo.setEndTime(subscription.getEndTime());
            vo.setLifetime(subscription.getLifetime());
            vo.setStatus(subscription.getStatus());
        } else {
            vo.setLifetime(0);
            vo.setStatus("free");
        }
        vo.setDailyProLimit(plan.getDailyProLimit());
        vo.setDailyAdvancedLimit(plan.getDailyAdvancedLimit());
        vo.setDailyProUsed(getTodayUsed(userId, "pro"));
        vo.setDailyAdvancedUsed(getTodayUsed(userId, "advanced"));
        vo.setDailyProRemaining(remaining(plan.getDailyProLimit(), vo.getDailyProUsed()));
        vo.setDailyAdvancedRemaining(remaining(plan.getDailyAdvancedLimit(), vo.getDailyAdvancedUsed()));
        vo.setPointBalance(pointService.getBalance(userId));
        return vo;
    }

    private LocalDateTime calcEndTime(SubscriptionPlan plan, LocalDateTime baseTime) {
        if (plan.getLifetime() != null && plan.getLifetime() == 1) {
            return null;
        }
        int days = plan.getDurationDays() == null ? 0 : plan.getDurationDays();
        return days <= 0 ? null : baseTime.plusDays(days);
    }

    private long getTodayUsed(Long userId, String tier) {
        RAtomicLong counter = redissonClient.getAtomicLong("usage:daily:chat-tier:" + userId + ":" + tier + ":" + LocalDate.now());
        return counter.get();
    }

    private Long remaining(Long limit, Long used) {
        if (limit == null || limit == -1L) {
            return -1L;
        }
        return Math.max(0L, limit - (used == null ? 0L : used));
    }
}
