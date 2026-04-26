package com.leo.airouterbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.DeleteRequest;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.exception.ThrowUtils;
import com.leo.airouterbackend.model.dto.provider.ProviderAddRequest;
import com.leo.airouterbackend.model.dto.provider.ProviderQueryRequest;
import com.leo.airouterbackend.model.dto.provider.ProviderUpdateRequest;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.model.enums.HealthStatusEnum;
import com.leo.airouterbackend.model.enums.ProviderStatusEnum;
import com.leo.airouterbackend.model.vo.ProviderVO;
import com.leo.airouterbackend.service.ModelProviderService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/provider")
public class ModelProviderController {

    @Resource
    private ModelProviderService modelProviderService;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addProvider(@RequestBody ProviderAddRequest providerAddRequest) {
        ThrowUtils.throwIf(providerAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(providerAddRequest.getProviderName() == null || providerAddRequest.getProviderName().isBlank(),
                ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(providerAddRequest.getDisplayName() == null || providerAddRequest.getDisplayName().isBlank(),
                ErrorCode.PARAMS_ERROR);

        ModelProvider provider = new ModelProvider();
        BeanUtil.copyProperties(providerAddRequest, provider);

        if (provider.getStatus() == null || provider.getStatus().isBlank()) {
            provider.setStatus(ProviderStatusEnum.ACTIVE.getValue());
        }
        if (provider.getHealthStatus() == null) {
            provider.setHealthStatus(HealthStatusEnum.UNKNOWN.getValue());
        }
        if (provider.getPriority() == null) {
            provider.setPriority(100);
        }
        provider.setIsDelete(0);

        boolean result = modelProviderService.save(provider);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(provider.getId());
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateProvider(@RequestBody ProviderUpdateRequest providerUpdateRequest) {
        ThrowUtils.throwIf(providerUpdateRequest == null || providerUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);

        ModelProvider provider = new ModelProvider();
        BeanUtil.copyProperties(providerUpdateRequest, provider);

        boolean result = modelProviderService.updateById(provider);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteProvider(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        boolean result = modelProviderService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo/{id}")
    public BaseResponse<ProviderVO> getProviderVOById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ModelProvider provider = modelProviderService.getById(id);
        ThrowUtils.throwIf(provider == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(modelProviderService.getProviderVO(provider));
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ProviderVO>> listProviderVOByPage(@RequestBody ProviderQueryRequest providerQueryRequest) {
        long pageNum = providerQueryRequest.getPageNum();
        long pageSize = providerQueryRequest.getPageSize();

        QueryWrapper queryWrapper = modelProviderService.getQueryWrapper(providerQueryRequest);
        Page<ModelProvider> providerPage = modelProviderService.page(Page.of(pageNum, pageSize), queryWrapper);

        Page<ProviderVO> providerVOPage = new Page<>(pageNum, pageSize, providerPage.getTotalRow());
        providerVOPage.setRecords(modelProviderService.getProviderVOList(providerPage.getRecords()));
        return ResultUtils.success(providerVOPage);
    }

    @GetMapping("/list/vo")
    public BaseResponse<List<ProviderVO>> listProviderVO() {
        List<ModelProvider> providers = modelProviderService.list();
        return ResultUtils.success(modelProviderService.getProviderVOList(providers));
    }
}
