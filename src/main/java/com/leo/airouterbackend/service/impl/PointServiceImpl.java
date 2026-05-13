package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.PointAccountMapper;
import com.leo.airouterbackend.mapper.PointPackageMapper;
import com.leo.airouterbackend.mapper.PointTransactionMapper;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.PointAccount;
import com.leo.airouterbackend.model.entity.PointPackage;
import com.leo.airouterbackend.model.entity.PointTransaction;
import com.leo.airouterbackend.model.dto.membership.PointAdjustRequest;
import com.leo.airouterbackend.service.PointService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PointServiceImpl implements PointService {

    @Resource
    private PointAccountMapper pointAccountMapper;

    @Resource
    private PointTransactionMapper pointTransactionMapper;

    @Resource
    private PointPackageMapper pointPackageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointAccount getOrCreateAccount(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PointAccount account = pointAccountMapper.selectOneByQuery(QueryWrapper.create().eq("userId", userId));
        if (account != null) {
            return account;
        }
        PointAccount newAccount = PointAccount.builder()
                .userId(userId)
                .balance(0L)
                .totalGranted(0L)
                .totalConsumed(0L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        pointAccountMapper.insert(newAccount);
        return newAccount;
    }

    @Override
    public long getBalance(Long userId) {
        return getOrCreateAccount(userId).getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long userId, long points, String transactionType, String refType, Long refId, String description) {
        if (points <= 0) {
            return;
        }
        PointAccount account = getOrCreateAccount(userId);
        long before = account.getBalance() == null ? 0L : account.getBalance();
        long after = before + points;
        account.setBalance(after);
        account.setTotalGranted((account.getTotalGranted() == null ? 0L : account.getTotalGranted()) + points);
        account.setUpdateTime(LocalDateTime.now());
        pointAccountMapper.update(account);
        saveTransaction(userId, points, before, after, transactionType, refType, refId, description);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumePoints(Long userId, long points, String transactionType, String refType, Long refId, String description) {
        if (points <= 0) {
            return;
        }
        PointAccount account = getOrCreateAccount(userId);
        long before = account.getBalance() == null ? 0L : account.getBalance();
        if (before < points) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分不足，当前积分：" + before + "，需要：" + points);
        }
        long after = before - points;
        account.setBalance(after);
        account.setTotalConsumed((account.getTotalConsumed() == null ? 0L : account.getTotalConsumed()) + points);
        account.setUpdateTime(LocalDateTime.now());
        pointAccountMapper.update(account);
        saveTransaction(userId, -points, before, after, transactionType, refType, refId, description);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePointsOrder(PaymentOrder order) {
        PointPackage pointPackage = pointPackageMapper.selectOneByQuery(QueryWrapper.create()
                .eq("packageCode", order.getProductCode())
                .eq("status", "active")
                .eq("isDelete", 0));
        if (pointPackage == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "积分包不存在或已下架");
        }
        grantPoints(order.getUserId(), pointPackage.getPoints(), "purchase", "payment_order", order.getId(),
                "购买积分包：" + pointPackage.getPackageName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(PointAdjustRequest request) {
        if (request == null || request.getUserId() == null || request.getPoints() == null || request.getPoints() == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (request.getPoints() > 0) {
            grantPoints(request.getUserId(), request.getPoints(), "admin_adjust", "admin", null, request.getDescription());
        } else {
            consumePoints(request.getUserId(), -request.getPoints(), "admin_adjust", "admin", null, request.getDescription());
        }
    }

    @Override
    public Page<PointTransaction> listUserTransactions(Long userId, long pageNum, long pageSize) {
        return pointTransactionMapper.paginate(pageNum, pageSize, QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false));
    }

    private void saveTransaction(Long userId, long changeAmount, long before, long after, String type,
                                 String refType, Long refId, String description) {
        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .changeAmount(changeAmount)
                .balanceBefore(before)
                .balanceAfter(after)
                .transactionType(type)
                .refType(refType)
                .refId(refId)
                .description(description)
                .createTime(LocalDateTime.now())
                .build();
        pointTransactionMapper.insert(transaction);
    }
}
