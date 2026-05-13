package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.PermissionConstant;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.membership.ModelAccessTierUpdateRequest;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.service.ModelService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/models")
public class AdminModelController {

    @Resource
    private ModelService modelService;

    @PostMapping("/access-tier")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE, mustPermissions = PermissionConstant.ADMIN_MODEL)
    public BaseResponse<Boolean> updateAccessTier(@RequestBody ModelAccessTierUpdateRequest request) {
        if (request == null || request.getModelId() == null || request.getAccessTier() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Model model = new Model();
        model.setId(request.getModelId());
        model.setAccessTier(request.getAccessTier());
        if (request.getPointCost() != null) {
            model.setPointCost(request.getPointCost());
        }
        return ResultUtils.success(modelService.updateById(model));
    }
}
