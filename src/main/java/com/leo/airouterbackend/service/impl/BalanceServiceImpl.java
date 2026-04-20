package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.UserMapper;
import com.leo.airouterbackend.model.entity.BillingRecord;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.BalanceService;
import com.leo.airouterbackend.service.BillingRecordService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @program: leo-ai-router-backend
 * @description:
 * @author: Miao Zheng
 * @date: 2026-04-18 17:22
 **/
@Slf4j
@Service
public class BalanceServiceImpl extends ServiceImpl<UserMapper, User> implements BalanceService {

    @Resource
    private BillingRecordService billingRecordService;

    @Override
    public boolean checkBalance(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        return balance.compareTo(amount) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductBalance(Long userId, BigDecimal amount, Long requestLogId, String description) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "余额不足，当前余额：¥" + currentBalance + "，需要：¥" + amount);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        user.setBalance(newBalance);
        boolean updated = updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扣款失败");
        }

        // 记录账单
        BillingRecord billingRecord = BillingRecord.builder()
                .userId(userId)
                .requestLogId(requestLogId)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description(description != null ? description : "API调用消费")
                .billingType("api_call")
                .createTime(LocalDateTime.now())
                .build();
        billingRecordService.save(billingRecord);
        log.info("用户 {} 扣减余额成功：¥{} -> ¥{}", userId, currentBalance, newBalance);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addBalance(Long userId, BigDecimal amount, String description) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        user.setBalance(newBalance);
        boolean updated = updateById(user);

        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "充值失败");
        }

        // 记录账单
        BillingRecord billingRecord = BillingRecord.builder()
                .userId(userId)
                .requestLogId(null)
                .amount(amount)
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .description(description != null ? description : "账户充值")
                .billingType("recharge")
                .createTime(LocalDateTime.now())
                .build();

        billingRecordService.save(billingRecord);

        log.info("用户 {} 充值成功：¥{} -> ¥{}", userId, currentBalance, newBalance);
        return true;
    }

    @Override
    public BigDecimal getUserBalance(Long userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
    }

    @Override
    public boolean updateBalance(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return this.updateById(user);
    }
}