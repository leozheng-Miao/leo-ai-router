package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.entity.RechargeRecord;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.math.BigDecimal;

public interface RechargeService extends IService<RechargeRecord> {
    
    /**
     * 创建充值记录
     */
    RechargeRecord createRechargeRecord(Long userId, BigDecimal amount, String paymentMethod);
    
    /**
     * 完成充值
     */
    boolean completeRecharge(Long recordId, String paymentId);

    boolean updateRechargeStatus(Long recordId, String status, String paymentId);

    RechargeRecord getByPaymentId(String paymentId);

    /**
     * 获取用户充值记录
     */
    Page<RechargeRecord> listUserRechargeRecords(Long userId, int pageNum, int pageSize);
}