package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.entity.BillingRecord;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.BalanceService;
import com.leo.airouterbackend.service.BillingRecordService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.math.BigDecimal;

@RestController
@RequestMapping("/balance")
@Slf4j
public class BalanceController {

    @Resource
    private BalanceService balanceService;

    @Resource
    private BillingRecordService billingRecordService;

    @Resource
    private UserService userService;

    /**
     * 获取我的余额信息
     */
    @GetMapping("/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的余额信息")
    public BaseResponse<BalanceVO> getMyBalance(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        BigDecimal balance = balanceService.getUserBalance(loginUser.getId());
        BigDecimal totalSpending = billingRecordService.getUserTotalSpending(loginUser.getId());
        BigDecimal totalRecharge = billingRecordService.getUserTotalRecharge(loginUser.getId());

        BalanceVO balanceVO = new BalanceVO();
        balanceVO.setBalance(balance);
        balanceVO.setTotalSpending(totalSpending);
        balanceVO.setTotalRecharge(totalRecharge);

        return ResultUtils.success(balanceVO);
    }

    /**
     * 获取我的消费账单（分页）
     */
    @GetMapping("/billing/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的消费账单")
    public BaseResponse<Page<BillingRecord>> getMyBillingRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<BillingRecord> page = billingRecordService.listUserBillingRecords(
                loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 余额信息VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceVO implements Serializable {
        private BigDecimal balance; // 当前余额
        private BigDecimal totalSpending; // 总消费
        private BigDecimal totalRecharge; // 总充值
    }
}