package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.util.OpenAiProviderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI 图片模型适配器
 */
@Slf4j
@Component
public class OpenAiImageAdapter implements ImageModelAdapter {

    @Override
    public ImageGenerationResponse generate(Model model, ModelProvider provider, ImageGenerationRequest request, int n) {
        try {
            OpenAiImageApi imageApi = OpenAiImageApi.builder()
                    .baseUrl(OpenAiProviderUtils.normalizeBaseUrl(provider.getBaseUrl()))
                    .apiKey(provider.getApiKey())
                    .restClientBuilder(OpenAiProviderUtils.buildRestClientBuilder(provider))
                    .build();

            OpenAiImageApi.OpenAiImageRequest imageRequest = buildRequest(model, request, n);
            ResponseEntity<OpenAiImageApi.OpenAiImageResponse> responseEntity = imageApi.createImage(imageRequest);
            OpenAiImageApi.OpenAiImageResponse response = responseEntity.getBody();
            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OpenAI 图片模型未返回图片数据");
            }
            return convertResponse(response);
        } catch (Exception e) {
            log.error("OpenAI 图片模型调用失败，model={}", model.getModelKey(), e);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用 OpenAI 图片生成失败: " + e.getMessage());
        }
    }

    OpenAiImageApi.OpenAiImageRequest buildRequest(Model model, ImageGenerationRequest request, int n) {
        return new OpenAiImageApi.OpenAiImageRequest(
                request.getPrompt(),
                model.getModelKey(),
                n,
                normalizeQuality(request.getQuality()),
                null,
                normalizeSize(request.getSize()),
                null,
                request.getUser()
        );
    }

    ImageGenerationResponse convertResponse(OpenAiImageApi.OpenAiImageResponse response) {
        List<ImageGenerationResponse.ImageData> imageDataList = response.data().stream()
                .map(item -> ImageGenerationResponse.ImageData.builder()
                        .url(item.url())
                        .b64Json(item.b64Json())
                        .revisedPrompt(item.revisedPrompt())
                        .build())
                .toList();

        return ImageGenerationResponse.builder()
                .created(response.created())
                .data(imageDataList)
                .build();
    }

    String normalizeSize(String size) {
        return OpenAiProviderUtils.normalizeImageSize(size);
    }

    String normalizeQuality(String quality) {
        return OpenAiProviderUtils.normalizeImageQuality(quality);
    }

    @Override
    public boolean supports(String providerName) {
        return OpenAiProviderUtils.isOpenAiProvider(providerName);
    }
}
