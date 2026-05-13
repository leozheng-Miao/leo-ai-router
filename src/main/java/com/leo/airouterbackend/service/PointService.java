package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.membership.PointAdjustRequest;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.PointAccount;
import com.leo.airouterbackend.model.entity.PointTransaction;
import com.mybatisflex.core.paginate.Page;

public interface PointService {

    PointAccount getOrCreateAccount(Long userId);

    long getBalance(Long userId);

    void grantPoints(Long userId, long points, String transactionType, String refType, Long refId, String description);

    void consumePoints(Long userId, long points, String transactionType, String refType, Long refId, String description);

    void completePointsOrder(PaymentOrder order);

    void adjustPoints(PointAdjustRequest request);

    Page<PointTransaction> listUserTransactions(Long userId, long pageNum, long pageSize);
}
