package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.dto.payment.CreatePointsOrderRequest;
import com.leo.airouterbackend.model.dto.payment.CreateSubscriptionOrderRequest;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.vo.PaymentOrderVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

public interface PaymentOrderService extends IService<PaymentOrder> {

    PaymentOrderVO createSubscriptionOrder(Long userId, CreateSubscriptionOrderRequest request);

    PaymentOrderVO createPointsOrder(Long userId, CreatePointsOrderRequest request);

    boolean completeOrderByOrderNo(String orderNo, String paymentId);

    boolean completeOrderByPaymentId(String paymentId);

    Page<PaymentOrder> listUserOrders(Long userId, long pageNum, long pageSize);
}
