package com.leo.airouterbackend.adaptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通义万相图片模型适配器
 */
@Slf4j
@Component
public class DashscopeImageAdapter implements ImageModelAdapter {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("qwen", "dashscope", "tongyi", "aliyun");

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public ImageGenerationResponse generate(Model model, ModelProvider provider, ImageGenerationRequest request, int n) {
        try {
            String size = normalizeSize(request.getSize());
            Map<String, Object> body = new HashMap<>();
            body.put("model", model.getModelKey());
            body.put("input", Map.of("prompt", request.getPrompt()));
            body.put("parameters", Map.of("size", size, "n", n));
            String requestBody = objectMapper.writeValueAsString(body);

            String apiUrl = provider.getBaseUrl().replace("/compatible-mode", "")
                    + "/api/v1/services/aigc/text2image/image-synthesis";

            HttpResponse httpResponse = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")
                    .body(requestBody)
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            log.info("通义万相创建任务响应：{}", responseBody);

            JsonNode rootNode = objectMapper.readTree(responseBody);
            if (!rootNode.has("output") || !rootNode.get("output").has("task_id")) {
                String errorMsg = rootNode.has("message") ? rootNode.get("message").asText() : "未返回任务ID";
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建图片生成任务失败：" + errorMsg);
            }

            String taskId = rootNode.get("output").get("task_id").asText();
            String taskApiUrl = provider.getBaseUrl().replace("/compatible-mode", "")
                    + "/api/v1/tasks/" + taskId;

            int maxRetries = 60;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                Thread.sleep(2000);

                HttpResponse taskResponse = HttpRequest.get(taskApiUrl)
                        .header("Authorization", "Bearer " + provider.getApiKey())
                        .timeout(10000)
                        .execute();

                JsonNode taskNode = objectMapper.readTree(taskResponse.body());
                if (!taskNode.has("output") || !taskNode.get("output").has("task_status")) {
                    retryCount++;
                    continue;
                }

                String taskStatus = taskNode.get("output").get("task_status").asText();
                log.info("图片任务状态：{} (轮询次数: {})", taskStatus, retryCount + 1);

                if ("SUCCEEDED".equals(taskStatus)) {
                    List<ImageGenerationResponse.ImageData> imageDataList = new ArrayList<>();
                    JsonNode results = taskNode.path("output").path("results");
                    for (JsonNode result : results) {
                        if (result.has("url")) {
                            imageDataList.add(ImageGenerationResponse.ImageData.builder()
                                    .url(result.get("url").asText())
                                    .revisedPrompt(request.getPrompt())
                                    .build());
                        }
                    }
                    if (imageDataList.isEmpty()) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务成功但未返回图片");
                    }
                    return ImageGenerationResponse.builder()
                            .created(System.currentTimeMillis() / 1000)
                            .data(imageDataList)
                            .build();
                }

                if ("FAILED".equals(taskStatus)) {
                    String errorMsg = taskNode.path("output").path("message").asText("任务失败");
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败：" + errorMsg);
                }
                retryCount++;
            }

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成被中断");
        } catch (Exception e) {
            log.error("调用通义万相图片模型失败", e);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用图片生成API失败: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String providerName) {
        return providerName != null && SUPPORTED_PROVIDERS.contains(providerName.toLowerCase());
    }

    private String normalizeSize(String size) {
        String normalized = StrUtil.isNotBlank(size) ? size.trim() : "1024x1024";
        return normalized.replace('x', '*').replace('X', '*');
    }
}
