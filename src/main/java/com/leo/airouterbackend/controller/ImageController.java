package com.leo.airouterbackend.controller;

import cn.hutool.core.util.StrUtil;
import com.leo.airouterbackend.annotation.AuthCheck;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.common.ResultUtils;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.ApiKey;
import com.leo.airouterbackend.model.entity.ImageGenerationRecord;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.ApiKeyService;
import com.leo.airouterbackend.service.ImageGenerationService;
import com.leo.airouterbackend.service.UserService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片生成控制器
 */
@RestController
@RequestMapping("/v1/images")
@Tag(name = "图片生成")
@Slf4j
public class ImageController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private UserService userService;

    @Resource
    private ApiKeyService apiKeyService;

    /**
     * 生成图片（OpenAI 兼容接口）
     */
    @PostMapping("/generations")
    @Operation(summary = "生成图片")
    public ImageGenerationResponse generateImage(
            @RequestBody ImageGenerationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        Long userId = null;
        Long apiKeyId = null;

        // 认证：优先使用 API Key，否则使用 Session
        if (StrUtil.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            String apiKeyValue = authorization.substring(7);
            ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);

            if (apiKey == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 无效或已失效");
            }
            if (!"active".equals(apiKey.getStatus())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "API Key 已被禁用");
            }

            userId = apiKey.getUserId();
            apiKeyId = apiKey.getId();
        } else {
            // 尝试从 Session 获取用户信息
            try {
                User loginUser = userService.getLoginUser(httpRequest);
                userId = loginUser.getId();
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录或提供有效的 API Key");
            }
        }

        String clientIp = httpRequest.getRemoteAddr();

        // 调用服务生成图片
        return imageGenerationService.generateImage(request, userId, apiKeyId, clientIp);
    }

    /**
     * 获取我的图片生成记录
     */
    @GetMapping("/my/records")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的图片生成记录")
    public BaseResponse<Page<ImageGenerationRecord>> getMyRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ImageGenerationRecord> page = imageGenerationService.listUserRecords(
                loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }

}