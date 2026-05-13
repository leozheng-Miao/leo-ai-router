package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.ApiKeyMapper;
import com.leo.airouterbackend.model.entity.ApiKey;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.enums.ApiKeyStatusEnum;
import com.leo.airouterbackend.service.ApiKeyService;
import com.leo.airouterbackend.service.EntitlementService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {

    @Resource
    private EntitlementService entitlementService;

    @Override
    public ApiKey createApiKey(String keyName, User loginUser) {
        long currentCount = this.count(QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("isDelete", 0)
                .ne("status", ApiKeyStatusEnum.REVOKED.getValue()));
        entitlementService.checkApiKeyCreate(loginUser.getId(), currentCount);
        // 生成 API Key（sk- 前缀 + 32位随机字符）
        String keyValue = "sk-" + IdUtil.simpleUUID();

        // 创建 API Key 对象
        ApiKey apiKey = ApiKey.builder()
                .userId(loginUser.getId())
                .keyValue(keyValue)
                .keyName(keyName)
                .status(ApiKeyStatusEnum.ACTIVE.getValue())
                .totalTokens(0L)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();


        // 保存到数据库
        this.save(apiKey);

        return apiKey;
    }
  
    @Override
    public List<ApiKey> listUserApiKeys(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        return this.list(queryWrapper);
    }

    @Override
    public boolean revokeApiKey(Long id, Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", id)
                .eq("userId", userId);

        ApiKey apiKey = this.getOne(queryWrapper);
        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "API Key 不存在");
        }

        // 更新状态为 revoked
        apiKey.setStatus(ApiKeyStatusEnum.REVOKED.getValue());
        apiKey.setUpdateTime(LocalDateTime.now());
        return this.updateById(apiKey);
    }

    @Override
    public ApiKey getByKeyValue(String keyValue) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("keyValue", keyValue)
                .eq("status", ApiKeyStatusEnum.ACTIVE.getValue())
                .eq("isDelete", 0);

        return this.getOne(queryWrapper);
    }

    @Override
    public void updateUsageStats(Long apiKeyId, Integer tokens) {
        ApiKey apiKey = this.getById(apiKeyId);
        if (apiKey != null) {
            apiKey.setTotalTokens(apiKey.getTotalTokens() + tokens);
            apiKey.setLastUsedTime(LocalDateTime.now());
            apiKey.setUpdateTime(LocalDateTime.now());
            this.updateById(apiKey);
        }
    }
}
