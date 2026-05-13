package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.dto.payment.CreatePointsOrderRequest;
import com.leo.airouterbackend.model.dto.payment.CreateSubscriptionOrderRequest;
import com.leo.airouterbackend.model.entity.PaymentOrder;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.vo.PaymentOrderVO;
import com.leo.airouterbackend.service.PaymentOrderService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/orders")
public class PaymentOrderController {

    @Resource
    private PaymentOrderService paymentOrderService;

    @Resource
    private UserService userService;

    @PostMapping("/subscription")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<PaymentOrderVO> createSubscriptionOrder(@RequestBody CreateSubscriptionOrderRequest request,
                                                                HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(paymentOrderService.createSubscriptionOrder(loginUser.getId(), request));
    }

    @PostMapping("/points")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<PaymentOrderVO> createPointsOrder(@RequestBody CreatePointsOrderRequest request,
                                                          HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(paymentOrderService.createPointsOrder(loginUser.getId(), request));
    }

    @GetMapping("/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Page<PaymentOrder>> listMyOrders(@RequestParam(defaultValue = "1") long pageNum,
                                                         @RequestParam(defaultValue = "10") long pageSize,
                                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(paymentOrderService.listUserOrders(loginUser.getId(), pageNum, pageSize));
    }
}
