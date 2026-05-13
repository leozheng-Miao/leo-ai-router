package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.PermissionConstant;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.dto.membership.PointPackageSaveRequest;
import com.leo.airouterbackend.model.dto.membership.SubscriptionPlanSaveRequest;
import com.leo.airouterbackend.model.dto.membership.UserSubscriptionAdjustRequest;
import com.leo.airouterbackend.service.MembershipService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/membership")
public class AdminMembershipController {

    @Resource
    private MembershipService membershipService;

    @PostMapping("/plans/save")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = PermissionConstant.ADMIN_RBAC)
    public BaseResponse<Boolean> savePlan(@RequestBody SubscriptionPlanSaveRequest request) {
        return ResultUtils.success(membershipService.savePlan(request));
    }

    @PostMapping("/point-packages/save")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = PermissionConstant.ADMIN_RBAC)
    public BaseResponse<Boolean> savePointPackage(@RequestBody PointPackageSaveRequest request) {
        return ResultUtils.success(membershipService.savePointPackage(request));
    }

    @PostMapping("/user-subscription/adjust")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = PermissionConstant.ADMIN_RBAC)
    public BaseResponse<Boolean> adjustUserSubscription(@RequestBody UserSubscriptionAdjustRequest request) {
        membershipService.adjustUserSubscription(request);
        return ResultUtils.success(true);
    }
}
