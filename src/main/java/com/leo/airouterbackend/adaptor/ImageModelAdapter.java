package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;

/**
 * 图片模型适配器接口
 */
public interface ImageModelAdapter {

    /**
     * 调用图片模型
     */
    ImageGenerationResponse generate(Model model, ModelProvider provider, ImageGenerationRequest request, int n);

    /**
     * 是否支持该提供者
     */
    boolean supports(String providerName);
}
