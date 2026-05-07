package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.adaptor.ImageModelAdapter;
import com.leo.airouterbackend.adaptor.ImageModelAdapterFactory;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.ImageGenerationRecordMapper;
import com.leo.airouterbackend.matrics.AIMetricsCollector;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.ImageGenerationRecord;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.service.BalanceService;
import com.leo.airouterbackend.service.BillingService;
import com.leo.airouterbackend.service.ImageGenerationService;
import com.leo.airouterbackend.service.ModelProviderService;
import com.leo.airouterbackend.service.ModelService;
import com.leo.airouterbackend.service.QuotaService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @program: leo-ai-router-backend
 * @description:
 * @author: Miao Zheng
 * @date: 2026-04-21 13:08
 **/
@Slf4j
@Service
public class ImageGenerationServiceImpl extends ServiceImpl<ImageGenerationRecordMapper, ImageGenerationRecord> implements ImageGenerationService {

    @Resource
    private ModelService modelService;

    @Resource
    private ModelProviderService modelProviderService;

    @Resource
    private QuotaService quotaService;

    @Resource
    private BalanceService balanceService;

    @Resource
    private BillingService billingService;

    @Resource
    private AIMetricsCollector aiMetricsCollector;

    @Resource
    private UserService userService;

    @Resource
    private ImageModelAdapterFactory imageModelAdapterFactory;

    private static final String DEFAULT_SIZE = "1024x1024";
    private static final int DEFAULT_N = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImageGenerationResponse generateImage(ImageGenerationRequest request, Long userId, Long apiKeyId, String clientIp) {
        long startTime = System.currentTimeMillis();
        if (StrUtil.isBlank(request.getPrompt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空");
        }
        // 检查用户状态
        if (userId != null && userService.isUserDisabled(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，无法使用服务");
        }

        // 检查用户配额
        if (userId != null && !quotaService.checkQuota(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Token配额已用尽，请联系管理员增加配额");
        }

        // 设置默认值
        String size = StrUtil.isNotBlank(request.getSize()) ? request.getSize() : DEFAULT_SIZE;
        int n = DEFAULT_N;
        Model model = resolveImageModel(request.getModel());
        if (model == null || !"image".equals(model.getModelType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型不存在或不是图片生成模型");
        }
        String modelKey = model.getModelKey();
        ModelProvider modelProvider = modelProviderService.getById(model.getProviderId());
        if (modelProvider == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在");
        }
        BigDecimal estimatedCost = model.getInputPrice() != null ? model.getInputPrice().multiply(new BigDecimal(n)) : BigDecimal.ZERO;
        if (userId != null && !balanceService.checkBalance(userId, estimatedCost)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "余额不足，生成 " + "n " + "张图片需要¥ " + estimatedCost + ", 请先充值");
        }

        try {
            request.setSize(size);
            ImageModelAdapter imageModelAdapter = imageModelAdapterFactory.getAdapter(modelProvider.getProviderName());
            ImageGenerationResponse response = imageModelAdapter.generate(model, modelProvider, request, n);

            long duration = System.currentTimeMillis() - startTime;
            int actualImageCount = response.getData() != null ? response.getData().size() : n;
            BigDecimal actualCost = model.getInputPrice() != null ? model.getInputPrice().multiply(new BigDecimal(actualImageCount)) : BigDecimal.ZERO;
            for (ImageGenerationResponse.ImageData imageData : response.getData()) {
                ImageGenerationRecord record = ImageGenerationRecord.builder()
                        .userId(userId)
                        .apiKeyId(apiKeyId)
                        .modelId(model.getId())
                        .modelKey(modelKey)
                        .prompt(request.getPrompt())
                        .revisedPrompt(imageData.getRevisedPrompt())
                        .imageUrl(imageData.getUrl())
                        .imageData(imageData.getB64Json())
                        .size(size)
                        .quality(request.getQuality())
                        .status("success")
                        .cost(model.getInputPrice() != null ? model.getInputPrice() : BigDecimal.ZERO)
                        .duration((int) duration)
                        .clientIp(clientIp)
                        .createTime(LocalDateTime.now())
                        .build();
                save(record);
            }

            if (userId != null) {
                int tokenPerImage = 1000;
                int totalTokens = tokenPerImage * actualImageCount;
                quotaService.deductTokens(userId, totalTokens);

                if (actualCost.compareTo(BigDecimal.ZERO) > 0) {
                    String description = apiKeyId != null
                            ? "API Key " + modelKey + " 生成 " + actualImageCount + " 张图片"
                            : "网页生成 " + modelKey + " 生成 " + actualImageCount + " 张图片";
                    balanceService.deductBalance(userId, actualCost, null, description);
                }
                // 收集监控指标
                aiMetricsCollector.recordRequest(model.getModelKey(), userId, apiKeyId != null ? apiKeyId.toString() : null);
                aiMetricsCollector.recordTokens(model.getModelKey(), totalTokens);
                aiMetricsCollector.recordResponseTime(model.getModelKey(), duration);
            }
            log.info("图片生成成功： 用户 {}, 模型 {}, 数量 {}, 生成时间 {}ms", userId, modelKey, actualImageCount, duration);
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            ImageGenerationRecord record = ImageGenerationRecord.builder()
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .modelKey(modelKey)
                    .prompt(request.getPrompt())
                    .size(size)
                    .quality(request.getQuality())
                    .status("failed")
                    .cost(BigDecimal.ZERO)
                    .duration((int) duration)
                    .errorMessage(e.getMessage())
                    .clientIp(clientIp)
                    .createTime(LocalDateTime.now())
                    .build();
            save(record);
            log.error("图片生成失败： 用户 {}, 模型 {}, 数量 {}, 生成时间 {}ms, 错误信息: {}", userId, modelKey, n, duration, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片生成失败：" + e.getMessage());
        }
    }

    @Override
    public Page<ImageGenerationRecord> listUserRecords(Long userId, int pageNum, int pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("userId = ?", userId)
                .orderBy("createTime", false);
        return page(Page.of(pageNum, pageSize), queryWrapper);
    }

    private Model resolveImageModel(String requestedModelKey) {
        if (StrUtil.isNotBlank(requestedModelKey)) {
            return modelService.getByModelKey(requestedModelKey);
        }

        return modelService.getActiveModelsByType("image").stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "没有可用的图片模型"));
    }
}
