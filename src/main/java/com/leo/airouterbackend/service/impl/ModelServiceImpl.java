package com.leo.airouterbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.mapper.ModelMapper;
import com.leo.airouterbackend.constant.PermissionConstant;
import com.leo.airouterbackend.model.dto.model.ModelQueryRequest;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.model.enums.ModelStatusEnum;
import com.leo.airouterbackend.model.vo.ModelVO;
import com.leo.airouterbackend.service.ModelProviderService;
import com.leo.airouterbackend.service.ModelService;
import com.leo.airouterbackend.service.RbacService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型 服务层实现
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {

    @Resource
    private ModelProviderService modelProviderService;

    @Resource
    private RbacService rbacService;

    @Override
    public QueryWrapper getQueryWrapper(ModelQueryRequest modelQueryRequest) {
        if (modelQueryRequest == null) {
            return QueryWrapper.create();
        }

        Long providerId = modelQueryRequest.getProviderId();
        String modelKey = modelQueryRequest.getModelKey();
        String modelName = modelQueryRequest.getModelName();
        String modelType = modelQueryRequest.getModelType();
        String status = modelQueryRequest.getStatus();

        // 构造查询条件
        return QueryWrapper.create()
                .eq("providerId", providerId, providerId != null)
                .like("modelKey", modelKey, StrUtil.isNotBlank(modelKey))
                .like("modelName", modelName, StrUtil.isNotBlank(modelName))
                .eq("modelType", modelType, StrUtil.isNotBlank(modelType))
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("priority", false)
                .orderBy("createTime", false);
    }

    @Override
    public ModelVO getModelVO(Model model) {
        if (model == null) {
            return null;
        }
        ModelVO modelVO = new ModelVO();
        BeanUtil.copyProperties(model, modelVO);

        // 查询提供者信息
        Long providerId = model.getProviderId();
        if (providerId != null) {
            ModelProvider provider = modelProviderService.getById(providerId);
            if (provider != null) {
                modelVO.setProviderName(provider.getProviderName());
                modelVO.setProviderDisplayName(provider.getDisplayName());
            }
        }

        return modelVO;
    }

    @Override
    public List<ModelVO> getModelVOList(List<Model> modelList) {
        if (CollUtil.isEmpty(modelList)) {
            return new ArrayList<>();
        }

        // 批量查询提供者信息
        List<Long> providerIds = modelList.stream()
                .map(Model::getProviderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ModelProvider> providerMap = modelProviderService.listByIds(providerIds)
                .stream()
                .collect(Collectors.toMap(ModelProvider::getId, provider -> provider));

        // 组装 VO
        return modelList.stream().map(model -> {
            ModelVO modelVO = new ModelVO();
            BeanUtil.copyProperties(model, modelVO);

            // 设置提供者信息
            ModelProvider provider = providerMap.get(model.getProviderId());
            if (provider != null) {
                modelVO.setProviderName(provider.getProviderName());
                modelVO.setProviderDisplayName(provider.getDisplayName());
            }

            return modelVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Model getByModelKey(String modelKey) {
        if (StrUtil.isBlank(modelKey)) {
            return null;
        }
        return this.getOne(QueryWrapper.create().eq("modelKey", modelKey));
    }

    @Override
    public List<Model> getActiveModels() {
        return this.list(QueryWrapper.create()
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .orderBy("priority", false));
    }

    @Override
    public List<Model> getAvailableModels(Long userId) {
        List<Model> activeModels = getActiveModels();
        if (userId == null) {
            return new ArrayList<>();
        }
        return activeModels.stream()
                .filter(model -> rbacService.hasPermission(userId, PermissionConstant.MODEL_USE_PREFIX + model.getModelKey()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Model> getActiveModelsByProviderId(Long providerId) {
        if (providerId == null) {
            return new ArrayList<>();
        }
        return this.list(QueryWrapper.create()
                .eq("providerId", providerId)
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .orderBy("priority", false));
    }

    @Override
    public List<Model> getActiveModelsByType(String modelType) {
        if (StrUtil.isBlank(modelType)) {
            return new ArrayList<>();
        }
        return this.list(QueryWrapper.create()
                .eq("modelType", modelType)
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .orderBy("priority", false));
    }

    @Override
    public void updateModelMetrics(Long modelId, String healthStatus, Integer avgLatency,
                                   BigDecimal successRate, BigDecimal score) {
        if (modelId == null) {
            return;
        }
        UpdateChain.of(Model.class)
                .set(Model::getHealthStatus, healthStatus)
                .set(Model::getAvgLatency, avgLatency)
                .set(Model::getSuccessRate, successRate)
                .set(Model::getScore, score)
                .where(Model::getId).eq(modelId)
                .update();
    }
}
