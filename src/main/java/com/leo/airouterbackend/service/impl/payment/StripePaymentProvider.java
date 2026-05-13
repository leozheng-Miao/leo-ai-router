package com.leo.airouterbackend.service.impl.payment;

import com.leo.airouterbackend.config.StripeConfig;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.RechargeRecord;
import com.leo.airouterbackend.model.enums.PaymentDisplayTypeEnum;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;
import com.leo.airouterbackend.service.RechargeService;
import com.leo.airouterbackend.service.PaymentOrderService;
import com.leo.airouterbackend.service.payment.PaymentProvider;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    @Resource
    private StripeConfig stripeConfig;

    @Resource
    private RechargeService rechargeService;

    @Lazy
    @Resource
    private PaymentOrderService paymentOrderService;

    @Override
    public PaymentMethodEnum getPaymentMethod() {
        return PaymentMethodEnum.STRIPE;
    }

    @Override
    public PaymentCreateResult createRecharge(Long userId, BigDecimal amount) {
        try {
            RechargeRecord record = rechargeService.createRechargeRecord(userId, amount, PaymentMethodEnum.STRIPE.getValue());
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(buildStripeSuccessUrl(record.getId()))
                    .setCancelUrl(stripeConfig.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("cny")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("LeoAI Router 账户充值")
                                                                    .setDescription("充值金额：¥" + amount)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("userId", userId.toString())
                    .putMetadata("recordId", record.getId().toString())
                    .putMetadata("amount", amount.toString())
                    .build();
            Session session = Session.create(params);
            rechargeService.updateRechargeStatus(record.getId(), "pending", session.getId());
            log.info("Stripe 下单成功：recordId={}, sessionId={}", record.getId(), session.getId());
            return PaymentCreateResult.builder()
                    .recordId(record.getId())
                    .paymentMethod(PaymentMethodEnum.STRIPE.getValue())
                    .displayType(PaymentDisplayTypeEnum.REDIRECT_URL.getValue())
                    .paymentId(session.getId())
                    .redirectUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("创建 Stripe 支付会话失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建 Stripe 支付会话失败：" + e.getMessage());
        }
    }

    @Override
    public PaymentCreateResult createOrder(PaymentOrder order) {
        try {
            long amountInCents = order.getAmount().multiply(new BigDecimal("100")).longValue();
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}&order_no=" + order.getOrderNo())
                    .setCancelUrl(stripeConfig.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("cny")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("LeoAI Router " + order.getProductName())
                                                                    .setDescription("订单号：" + order.getOrderNo())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("userId", order.getUserId().toString())
                    .putMetadata("orderId", order.getId().toString())
                    .putMetadata("orderNo", order.getOrderNo())
                    .putMetadata("orderType", order.getOrderType())
                    .putMetadata("amount", order.getAmount().toString())
                    .build();
            Session session = Session.create(params);
            log.info("Stripe 统一订单下单成功：orderId={}, sessionId={}", order.getId(), session.getId());
            return PaymentCreateResult.builder()
                    .recordId(order.getId())
                    .paymentMethod(PaymentMethodEnum.STRIPE.getValue())
                    .displayType(PaymentDisplayTypeEnum.REDIRECT_URL.getValue())
                    .paymentId(session.getId())
                    .redirectUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            log.error("创建 Stripe 统一订单支付会话失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建 Stripe 支付会话失败：" + e.getMessage());
        }
    }

    public boolean handlePaymentSuccess(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            if (!"paid".equals(session.getPaymentStatus())) {
                log.warn("Stripe 会话未支付成功：sessionId={}, status={}", sessionId, session.getPaymentStatus());
                return false;
            }
            String orderNo = session.getMetadata().get("orderNo");
            if (orderNo != null) {
                paymentOrderService.completeOrderByOrderNo(orderNo, sessionId);
                return true;
            }
            String recordIdStr = session.getMetadata().get("recordId");
            if (recordIdStr == null) {
                log.error("Stripe 会话缺少 recordId 元数据：sessionId={}", sessionId);
                return false;
            }
            rechargeService.completeRecharge(Long.parseLong(recordIdStr), sessionId);
            return true;
        } catch (StripeException e) {
            log.error("处理 Stripe 成功回调失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理 Stripe 支付成功失败");
        }
    }

    public Event constructWebhookEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Stripe Webhook 验签失败", e);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "Stripe Webhook 验签失败");
        }
    }

    private String buildStripeSuccessUrl(Long recordId) {
        return stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}&record_id=" + recordId;
    }
}
