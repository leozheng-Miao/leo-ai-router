package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ImageModelAdapterFactory {

    @Resource
    private List<ImageModelAdapter> adapters;

    public ImageModelAdapter getAdapter(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供者名称不能为空");
        }

        for (ImageModelAdapter adapter : adapters) {
            if (adapter.supports(providerName)) {
                log.debug("为图片提供者 {} 选择适配器: {}", providerName, adapter.getClass().getSimpleName());
                return adapter;
            }
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到可用的图片模型适配器: " + providerName);
    }
}
