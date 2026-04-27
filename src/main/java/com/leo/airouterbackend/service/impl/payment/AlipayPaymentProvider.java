package com.leo.airouterbackend.service.impl.payment;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayApiException;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.leo.airouterbackend.config.AlipayPaymentConfig;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.entity.RechargeRecord;
import com.leo.airouterbackend.model.enums.PaymentDisplayTypeEnum;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;
import com.leo.airouterbackend.service.RechargeService;
import com.leo.airouterbackend.service.payment.PaymentProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AlipayPaymentProvider implements PaymentProvider {

    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    private static final String ORDER_PREFIX = "RCG_";

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private AlipayPaymentConfig alipayPaymentConfig;

    @Resource
    private RechargeService rechargeService;

    @Override
    public PaymentMethodEnum getPaymentMethod() {
        return PaymentMethodEnum.ALIPAY;
    }

    @Override
    public PaymentCreateResult createRecharge(Long userId, BigDecimal amount) {
        RechargeRecord record = rechargeService.createRechargeRecord(userId, amount, PaymentMethodEnum.ALIPAY.getValue());
        String outTradeNo = buildOutTradeNo(record.getId());
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(alipayPaymentConfig.getNotifyUrl());
            request.setReturnUrl(alipayPaymentConfig.getReturnUrl());

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount(amount.stripTrailingZeros().toPlainString());
            model.setSubject("LeoAI Router 账户充值");
            model.setBody("充值记录ID：" + record.getId());
            model.setProductCode(PRODUCT_CODE);
            request.setBizModel(model);

            String formHtml = alipayClient.pageExecute(request).getBody();
            log.info("支付宝下单成功：recordId={}, outTradeNo={}", record.getId(), outTradeNo);
            return PaymentCreateResult.builder()
                    .recordId(record.getId())
                    .paymentMethod(PaymentMethodEnum.ALIPAY.getValue())
                    .displayType(PaymentDisplayTypeEnum.FORM_HTML.getValue())
                    .paymentId(outTradeNo)
                    .formHtml(formHtml)
                    .build();
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付表单失败：recordId={}", record.getId(), e);
            rechargeService.updateRechargeStatus(record.getId(), "failed", outTradeNo);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建支付宝支付失败：" + e.getMessage());
        }
    }

    @Override
    public boolean handleAsyncNotify(Map<String, String> params) {
        try {
            if (!verifySignature(params)) {
                log.warn("支付宝异步通知验签失败");
                return false;
            }
            return completeByTradeParams(params);
        } catch (AlipayApiException e) {
            log.error("处理支付宝异步通知失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理支付宝异步通知失败");
        }
    }

    @Override
    public boolean handleSyncReturn(Map<String, String> params) {
        try {
            if (!verifySignature(params)) {
                log.warn("支付宝同步回跳验签失败");
                return false;
            }
            String outTradeNo = params.get("out_trade_no");
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            request.setBizContent(JSONUtil.createObj().set("out_trade_no", outTradeNo).toString());
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝查询订单失败：outTradeNo={}, msg={}", outTradeNo, response.getSubMsg());
                return false;
            }
            Map<String, String> tradeParams = new HashMap<>();
            tradeParams.put("out_trade_no", response.getOutTradeNo());
            tradeParams.put("trade_no", response.getTradeNo());
            tradeParams.put("trade_status", response.getTradeStatus());
            tradeParams.put("total_amount", response.getTotalAmount());
            return completeByTradeParams(tradeParams);
        } catch (AlipayApiException e) {
            log.error("处理支付宝同步回跳失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理支付宝同步回跳失败");
        }
    }

    private boolean completeByTradeParams(Map<String, String> params) {
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("支付宝交易未完成：status={}", tradeStatus);
            return false;
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String totalAmount = params.get("total_amount");

        Long recordId = parseRecordId(outTradeNo);
        RechargeRecord record = rechargeService.getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "充值记录不存在");
        }
        BigDecimal notifyAmount = new BigDecimal(totalAmount);
        if (record.getAmount() == null || record.getAmount().compareTo(notifyAmount) != 0) {
            log.error("支付宝通知金额不匹配：recordId={}, recordAmount={}, notifyAmount={}", recordId, record.getAmount(), notifyAmount);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "支付宝通知金额校验失败");
        }
        rechargeService.completeRecharge(recordId, tradeNo);
        return true;
    }

    private boolean verifySignature(Map<String, String> params) throws AlipayApiException {
        String appId = params.get("app_id");
        if (appId != null && !appId.equals(alipayPaymentConfig.getAppId())) {
            log.warn("支付宝通知 appId 不匹配：{}", appId);
            return false;
        }
        return AlipaySignature.rsaCheckV1(
                params,
                alipayPaymentConfig.getAlipayPublicKey(),
                alipayPaymentConfig.getCharset(),
                alipayPaymentConfig.getSignType()
        );
    }

    private String buildOutTradeNo(Long recordId) {
        return ORDER_PREFIX + recordId;
    }

    private Long parseRecordId(String outTradeNo) {
        if (outTradeNo == null || !outTradeNo.startsWith(ORDER_PREFIX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的支付宝订单号");
        }
        return Long.parseLong(outTradeNo.substring(ORDER_PREFIX.length()));
    }
}
