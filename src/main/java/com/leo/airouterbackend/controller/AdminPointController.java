package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.PermissionConstant;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.model.dto.membership.PointAdjustRequest;
import com.leo.airouterbackend.service.PointService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/points")
public class AdminPointController {

    @Resource
    private PointService pointService;

    @PostMapping("/adjust")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = PermissionConstant.ADMIN_RBAC)
    public BaseResponse<Boolean> adjustPoints(@RequestBody PointAdjustRequest request) {
        pointService.adjustPoints(request);
        return ResultUtils.success(true);
    }
}
