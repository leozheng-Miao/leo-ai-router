package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.PaymentOrderMapper;
import com.leo.airouterbackend.model.dto.payment.CreatePointsOrderRequest;
import com.leo.airouterbackend.model.dto.payment.CreateSubscriptionOrderRequest;
import com.leo.airouterbackend.model.dto.payment.PaymentCreateResult;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.PointPackage;
import com.leo.airouterbackend.model.entity.SubscriptionPlan;
import com.leo.airouterbackend.model.enums.PaymentDisplayTypeEnum;
import com.leo.airouterbackend.model.enums.PaymentMethodEnum;
import com.leo.airouterbackend.model.vo.PaymentOrderVO;
import com.leo.airouterbackend.service.MembershipService;
import com.leo.airouterbackend.service.PaymentOrderService;
import com.leo.airouterbackend.service.PointService;
import com.leo.airouterbackend.service.payment.PaymentService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements PaymentOrderService {

    private static final String ORDER_PREFIX = "ORD_";

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private PaymentService paymentService;

    @Resource
    private MembershipService membershipService;

    @Resource
    private PointService pointService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO createSubscriptionOrder(Long userId, CreateSubscriptionOrderRequest request) {
        if (request == null || StrUtil.isBlank(request.getPlanCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SubscriptionPlan plan = membershipService.getActivePlan(request.getPlanCode());
        return createOrder(userId, "subscription", plan.getPlanCode(), plan.getPlanName(),
                plan.getPrice(), request.getPaymentMethod(), JSONUtil.toJsonStr(plan));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderVO createPointsOrder(Long userId, CreatePointsOrderRequest request) {
        if (request == null || StrUtil.isBlank(request.getPackageCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PointPackage pointPackage = membershipService.getActivePointPackage(request.getPackageCode());
        return createOrder(userId, "points", pointPackage.getPackageCode(), pointPackage.getPackageName(),
                pointPackage.getPrice(), request.getPaymentMethod(), JSONUtil.toJsonStr(pointPackage));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeOrderByOrderNo(String orderNo, String paymentId) {
        PaymentOrder order = paymentOrderMapper.selectOneByQuery(QueryWrapper.create().eq("orderNo", orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        return completeOrder(order, paymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeOrderByPaymentId(String paymentId) {
        PaymentOrder order = paymentOrderMapper.selectOneByQuery(QueryWrapper.create().eq("paymentId", paymentId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        return completeOrder(order, paymentId);
    }

    @Override
    public Page<PaymentOrder> listUserOrders(Long userId, long pageNum, long pageSize) {
        return paymentOrderMapper.paginate(pageNum, pageSize, QueryWrapper.create()
                .eq("userId", userId)
                .eq("isDelete", 0)
                .orderBy("createTime", false));
    }

    private PaymentOrderVO createOrder(Long userId, String orderType, String productCode, String productName,
                                       java.math.BigDecimal amount, String paymentMethodValue, String extra) {
        PaymentMethodEnum paymentMethod = resolvePaymentMethod(paymentMethodValue);
        if (paymentMethod == PaymentMethodEnum.WECHAT) {
            PaymentOrderVO vo = new PaymentOrderVO();
            vo.setOrderType(orderType);
            vo.setProductCode(productCode);
            vo.setProductName(productName);
            vo.setAmount(amount);
            vo.setPaymentMethod(PaymentMethodEnum.WECHAT.getValue());
            vo.setStatus("coming_soon");
            vo.setDisplayType(PaymentDisplayTypeEnum.DISABLED.getValue());
            return vo;
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderNo(ORDER_PREFIX + IdUtil.simpleUUID())
                .userId(userId)
                .orderType(orderType)
                .productCode(productCode)
                .productName(productName)
                .amount(amount)
                .paymentMethod(paymentMethod.getValue())
                .status("pending")
                .extra(extra)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();
        paymentOrderMapper.insert(order);

        PaymentCreateResult paymentResult = paymentService.createOrder(order, paymentMethod);
        order.setPaymentId(paymentResult.getPaymentId());
        order.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.update(order);

        PaymentOrderVO vo = toVO(order);
        vo.applyPaymentResult(paymentResult);
        return vo;
    }

    private boolean completeOrder(PaymentOrder order, String paymentId) {
        if ("success".equals(order.getStatus())) {
            return true;
        }
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单状态不允许完成：" + order.getStatus());
        }

        order.setStatus("success");
        order.setPaymentId(StrUtil.blankToDefault(paymentId, order.getPaymentId()));
        order.setPaidTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        paymentOrderMapper.update(order);

        if ("subscription".equals(order.getOrderType())) {
            membershipService.activateSubscription(order);
        } else if ("points".equals(order.getOrderType())) {
            pointService.completePointsOrder(order);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未知订单类型");
        }
        return true;
    }

    private PaymentMethodEnum resolvePaymentMethod(String paymentMethodValue) {
        PaymentMethodEnum paymentMethod = PaymentMethodEnum.getEnumByValue(paymentMethodValue);
        if (paymentMethod == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择合法的支付方式");
        }
        return paymentMethod;
    }

    private PaymentOrderVO toVO(PaymentOrder order) {
        PaymentOrderVO vo = new PaymentOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setProductCode(order.getProductCode());
        vo.setProductName(order.getProductName());
        vo.setAmount(order.getAmount());
        vo.setPaymentMethod(order.getPaymentMethod());
        vo.setPaymentId(order.getPaymentId());
        vo.setStatus(order.getStatus());
        vo.setPaidTime(order.getPaidTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
