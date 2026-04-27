package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.config.AlipayPaymentConfig;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.entity.RechargeRecord;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;
import com.leo.airouterbackend.service.RechargeService;
import com.leo.airouterbackend.service.impl.payment.StripePaymentProvider;
import com.leo.airouterbackend.service.payment.PaymentService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/recharge")
@Slf4j
public class RechargeController {

    @Resource
    private RechargeService rechargeService;

    @Resource
    private PaymentService paymentService;

    @Resource
    private UserService userService;

    @Resource
    private StripePaymentProvider stripePaymentProvider;

    @Resource
    private AlipayPaymentConfig alipayPaymentConfig;

    @PostMapping("/create")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "创建充值订单")
    public BaseResponse<CreateRechargeResponse> createRecharge(
            @RequestBody CreateRechargeRequest request,
            HttpServletRequest httpRequest) {
        validateRechargeAmount(request.getAmount());
        PaymentMethodEnum paymentMethod = resolvePaymentMethod(request.getPaymentMethod());
        User loginUser = userService.getLoginUser(httpRequest);
        PaymentCreateResult result = paymentService.createRecharge(loginUser.getId(), request.getAmount(), paymentMethod);
        return ResultUtils.success(toResponse(result));
    }

    /**
     * 创建充值订单（Stripe）
     */
    @PostMapping("/stripe/create")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "创建Stripe充值订单")
    public BaseResponse<CreateRechargeResponse> createStripeRecharge(
            @RequestBody CreateRechargeRequest request,
            HttpServletRequest httpRequest) {
        request.setPaymentMethod(PaymentMethodEnum.STRIPE.getValue());
        return createRecharge(request, httpRequest);
    }

    @PostMapping("/alipay/create")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "创建支付宝充值订单")
    public BaseResponse<CreateRechargeResponse> createAlipayRecharge(
            @RequestBody CreateRechargeRequest request,
            HttpServletRequest httpRequest) {
        request.setPaymentMethod(PaymentMethodEnum.ALIPAY.getValue());
        return createRecharge(request, httpRequest);
    }

    /**
     * 充值成功回调
     */
    @GetMapping("/stripe/success")
    @Operation(summary = "Stripe充值成功回调")
    public void stripeSuccess(
            @RequestParam("session_id") String sessionId,
            @RequestParam(value = "record_id", required = false) Long recordId,
            HttpServletResponse response) throws IOException {
        log.info("收到Stripe支付成功回调，SessionID：{}，recordId：{}", sessionId, recordId);
        String targetUrl = "http://localhost:5173/recharge/cancel?method=stripe";
        try {
            if (!StringUtils.hasText(sessionId) || sessionId.contains("{CHECKOUT_SESSION_ID}")) {
                if (recordId == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "Stripe回调缺少有效的充值记录ID");
                }
                RechargeRecord record = rechargeService.getById(recordId);
                if (record == null || !StringUtils.hasText(record.getPaymentId())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到Stripe支付会话");
                }
                sessionId = record.getPaymentId();
                log.info("Stripe回调占位符未替换，已根据recordId回填真实SessionID：{}", sessionId);
            }
            boolean success = stripePaymentProvider.handlePaymentSuccess(sessionId);
            targetUrl = success
                    ? appendQuery("http://localhost:5173/recharge/success?method=stripe", "session_id", sessionId)
                    : "http://localhost:5173/recharge/cancel?method=stripe";
        } catch (Exception e) {
            log.error("处理Stripe成功回调异常", e);
        }
        response.sendRedirect(targetUrl);
    }

    /**
     * 充值取消回调
     */
    @GetMapping("/stripe/cancel")
    @Operation(summary = "Stripe充值取消回调")
    public void stripeCancel(HttpServletResponse response) throws IOException {
        log.info("用户取消了Stripe支付");
        response.sendRedirect("http://localhost:5173/recharge/cancel?method=stripe");
    }

    @PostMapping("/alipay/notify")
    @Operation(summary = "支付宝异步通知")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("收到支付宝异步通知：outTradeNo={}", params.get("out_trade_no"));
        try {
            boolean success = paymentService.handleAsyncNotify(PaymentMethodEnum.ALIPAY, normalizeParams(params));
            return success ? "success" : "failure";
        } catch (Exception e) {
            log.error("处理支付宝异步通知异常", e);
            return "failure";
        }
    }

    @GetMapping("/alipay/return")
    @Operation(summary = "支付宝同步回跳")
    public void alipayReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        log.info("收到支付宝同步回跳：outTradeNo={}", params.get("out_trade_no"));
        String targetUrl = alipayPaymentConfig.getFrontendCancelUrl();
        String outTradeNo = params.get("out_trade_no");
        try {
            boolean success = paymentService.handleSyncReturn(PaymentMethodEnum.ALIPAY, normalizeParams(params));
            targetUrl = success ? alipayPaymentConfig.getFrontendSuccessUrl() : alipayPaymentConfig.getFrontendCancelUrl();
        } catch (Exception e) {
            log.error("处理支付宝同步回跳异常", e);
        }
        if (StringUtils.hasText(outTradeNo)) {
            targetUrl = appendQuery(targetUrl, "outTradeNo", outTradeNo);
        }
        response.sendRedirect(targetUrl);
    }

    /**
     * 获取我的充值记录（分页）
     */
    @GetMapping("/list/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的充值记录")
    public BaseResponse<Page<RechargeRecord>> getMyRechargeRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<RechargeRecord> page = rechargeService.listUserRechargeRecords(
                loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 创建充值请求
     */
    @Data
    public static class CreateRechargeRequest {
        private BigDecimal amount; // 充值金额（元）
        private String paymentMethod; // stripe / alipay
    }

    /**
     * 创建充值响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRechargeResponse implements Serializable {
        private Long recordId;
        private String paymentMethod;
        private String displayType;
        private String paymentId;
        private String redirectUrl;
        private String formHtml;
        private String checkoutUrl;
        private String sessionId;
    }

    private void validateRechargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "充值金额必须大于0");
        }
        if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("10000")) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "充值金额必须在1-10000元之间");
        }
    }

    private PaymentMethodEnum resolvePaymentMethod(String paymentMethod) {
        PaymentMethodEnum paymentMethodEnum = PaymentMethodEnum.getEnumByValue(paymentMethod);
        if (paymentMethodEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择合法的支付方式");
        }
        return paymentMethodEnum;
    }

    private CreateRechargeResponse toResponse(PaymentCreateResult result) {
        CreateRechargeResponse response = new CreateRechargeResponse();
        response.setRecordId(result.getRecordId());
        response.setPaymentMethod(result.getPaymentMethod());
        response.setDisplayType(result.getDisplayType());
        response.setPaymentId(result.getPaymentId());
        response.setRedirectUrl(result.getRedirectUrl());
        response.setFormHtml(result.getFormHtml());
        if (PaymentMethodEnum.STRIPE.getValue().equals(result.getPaymentMethod())) {
            response.setCheckoutUrl(result.getRedirectUrl());
            response.setSessionId(result.getPaymentId());
        }
        return response;
    }

    private Map<String, String> normalizeParams(Map<String, String> rawParams) {
        return rawParams == null ? Map.of() : new HashMap<>(rawParams);
    }

    private String appendQuery(String url, String key, String value) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
