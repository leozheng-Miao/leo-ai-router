package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.entity.PointPackage;
import com.leo.airouterbackend.model.entity.SubscriptionPlan;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.vo.MembershipVO;
import com.leo.airouterbackend.service.MembershipService;
import com.leo.airouterbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/membership")
public class MembershipController {

    @Resource
    private MembershipService membershipService;

    @Resource
    private UserService userService;

    @GetMapping("/plans")
    public BaseResponse<List<SubscriptionPlan>> listPlans() {
        return ResultUtils.success(membershipService.listVisiblePlans());
    }

    @GetMapping("/point-packages")
    public BaseResponse<List<PointPackage>> listPointPackages() {
        return ResultUtils.success(membershipService.listVisiblePointPackages());
    }

    @GetMapping("/my")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<MembershipVO> getMyMembership(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(membershipService.getMyMembership(loginUser.getId()));
    }
}
