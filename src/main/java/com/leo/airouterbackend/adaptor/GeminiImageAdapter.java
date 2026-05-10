package com.leo.airouterbackend.adaptor;

import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.google.genai.types.ProxyOptions;
import com.google.genai.types.ProxyType;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import com.leo.airouterbackend.util.GeminiProviderUtils;
import com.leo.airouterbackend.util.OpenAiProviderUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.netty.transport.ProxyProvider;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Gemini 图片模型适配器
 */
@Slf4j
@Component
public class GeminiImageAdapter implements ImageModelAdapter {

    @Override
    public ImageGenerationResponse generate(Model model, ModelProvider provider, ImageGenerationRequest request, int n) {
        if (n > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Gemini 图片模型当前仅支持生成 1 张图片");
        }
        try (Client client = buildClient(provider)) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities(List.of("TEXT", "IMAGE"))
                    .build();
            GenerateContentResponse response = client.models.generateContent(model.getModelKey(), request.getPrompt(), config);
            return convertResponse(response);
        } catch (Exception e) {
            log.error("Gemini 图片模型调用失败，model={}", model.getModelKey(), e);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用 Gemini 图片生成失败: " + e.getMessage());
        }
    }

    ImageGenerationResponse convertResponse(GenerateContentResponse response) {
        List<ImageGenerationResponse.ImageData> imageDataList = new ArrayList<>();
        if (response == null || response.parts() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Gemini 图片模型未返回图片数据");
        }
        for (Part part : response.parts()) {
            if (part.inlineData().isPresent() && part.inlineData().get().data().isPresent()) {
                String b64 = Base64.getEncoder().encodeToString(part.inlineData().get().data().get());
                imageDataList.add(ImageGenerationResponse.ImageData.builder()
                        .b64Json(b64)
                        .build());
            }
        }
        if (imageDataList.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Gemini 图片模型未返回图片数据");
        }
        return ImageGenerationResponse.builder()
                .created(System.currentTimeMillis() / 1000)
                .data(imageDataList)
                .build();
    }

    private Client buildClient(ModelProvider provider) {
        OpenAiProviderUtils.OpenAiTransportConfig transportConfig = OpenAiProviderUtils.resolveTransportConfig(provider);
        HttpOptions httpOptions = HttpOptions.builder()
                .apiVersion("v1beta")
                .baseUrl(GeminiProviderUtils.normalizeBaseUrl(provider.getBaseUrl()))
                .timeout(transportConfig.responseTimeoutSeconds() * 1000)
                .build();
        Client.Builder builder = Client.builder()
                .apiKey(provider.getApiKey())
                .httpOptions(httpOptions);
        ClientOptions clientOptions = buildClientOptions(transportConfig.proxySettings());
        if (clientOptions != null) {
            builder.clientOptions(clientOptions);
        }
        return builder.build();
    }

    @Override
    public boolean supports(String providerName) {
        return GeminiProviderUtils.isGeminiProvider(providerName);
    }

    private ClientOptions buildClientOptions(OpenAiProviderUtils.ProxySettings proxySettings) {
        if (proxySettings == null || !proxySettings.enabled()) {
            return null;
        }
        ProxyOptions.Builder proxyBuilder = ProxyOptions.builder()
                .type(toProxyType(proxySettings.type()))
                .host(proxySettings.host())
                .port(proxySettings.port());
        if (proxySettings.username() != null) {
            proxyBuilder.username(proxySettings.username());
        }
        if (proxySettings.password() != null) {
            proxyBuilder.password(proxySettings.password());
        }
        return ClientOptions.builder()
                .proxyOptions(proxyBuilder.build())
                .build();
    }

    private ProxyType toProxyType(ProxyProvider.Proxy proxyType) {
        if (proxyType == ProxyProvider.Proxy.SOCKS4 || proxyType == ProxyProvider.Proxy.SOCKS5) {
            return new ProxyType(ProxyType.Known.SOCKS);
        }
        return new ProxyType(ProxyType.Known.HTTP);
    }
}
