package com.leo.airouterbackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.config.WechatOAuthProperties;
import com.leo.airouterbackend.constant.UserConstant;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.exception.ErrorCode;
import com.leo.airouterbackend.mapper.UserMapper;
import com.leo.airouterbackend.mapper.rbac.UserAuthIdentityMapper;
import com.leo.airouterbackend.model.dto.auth.AuthLoginVO;
import com.leo.airouterbackend.model.dto.auth.WechatOAuthUrlVO;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.model.entity.rbac.UserAuthIdentity;
import com.leo.airouterbackend.model.enums.UserRoleEnum;
import com.leo.airouterbackend.service.AuthTokenService;
import com.leo.airouterbackend.service.RbacService;
import com.leo.airouterbackend.service.WechatOAuthService;
import com.leo.airouterbackend.utils.AuthPasswordUtils;
import com.leo.airouterbackend.utils.UserDefaultsUtils;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WechatOAuthServiceImpl implements WechatOAuthService {

    private static final String STATE_PREFIX = "wechat:oauth:state:";
    private static final String IDENTITY_TYPE = "wechat_open";

    @Resource
    private WechatOAuthProperties properties;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserAuthIdentityMapper userAuthIdentityMapper;

    @Resource
    private AuthTokenService authTokenService;

    @Resource
    private RbacService rbacService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public WechatOAuthUrlVO buildAuthorizeUrl() {
        if (!properties.isEnabled() || StrUtil.hasBlank(properties.getAppId(), properties.getRedirectUri())) {
            return WechatOAuthUrlVO.builder()
                    .enabled(false)
                    .message("微信开放平台未配置，当前环境暂不支持微信扫码登录")
                    .build();
        }
        String state = IdUtil.fastSimpleUUID();
        redissonClient.<String>getBucket(STATE_PREFIX + state)
                .set("1", properties.getStateTtlSeconds(), TimeUnit.SECONDS);
        String redirectUri = URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8);
        String url = "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + properties.getAppId()
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state
                + "#wechat_redirect";
        return WechatOAuthUrlVO.builder()
                .url(url)
                .state(state)
                .enabled(true)
                .message("ok")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginVO callback(String code, String state, HttpServletRequest request) {
        if (StrUtil.hasBlank(code, state)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        RBucket<String> stateBucket = redissonClient.getBucket(STATE_PREFIX + state);
        if (stateBucket.get() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "微信登录 state 无效或已过期");
        }
        stateBucket.delete();
        Map<String, Object> tokenInfo = requestJson(UriComponentsBuilder.fromUriString("https://api.weixin.qq.com/sns/oauth2/access_token")
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUri());
        assertWechatSuccess(tokenInfo);
        String accessToken = String.valueOf(tokenInfo.get("access_token"));
        String openId = String.valueOf(tokenInfo.get("openid"));
        String unionId = tokenInfo.get("unionid") == null ? null : String.valueOf(tokenInfo.get("unionid"));
        Map<String, Object> userInfo = requestJson(UriComponentsBuilder.fromUriString("https://api.weixin.qq.com/sns/userinfo")
                .queryParam("access_token", accessToken)
                .queryParam("openid", openId)
                .queryParam("lang", "zh_CN")
                .build(true)
                .toUri());
        assertWechatSuccess(userInfo);
        String identifier = StrUtil.isNotBlank(unionId) ? unionId : openId;
        UserAuthIdentity identity = userAuthIdentityMapper.selectOneByQuery(QueryWrapper.create()
                .eq("identityType", IDENTITY_TYPE)
                .eq("identifier", identifier));
        User user;
        if (identity == null) {
            user = new User();
            user.setUserAccount("wx_" + RandomUtil.randomString(12));
            user.setUserPassword(AuthPasswordUtils.createUnsetPassword());
            user.setUserName(StrUtil.blankToDefault((String) userInfo.get("nickname"), "微信用户"));
            user.setUserAvatar((String) userInfo.get("headimgurl"));
            user.setUserRole(UserRoleEnum.USER.getValue());
            user.setTokenVersion(0);
            UserDefaultsUtils.fillNewUserDefaults(user);
            int inserted = userMapper.insert(user);
            if (inserted <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信账号创建失败");
            }
            identity = new UserAuthIdentity();
            identity.setUserId(user.getId());
            identity.setIdentityType(IDENTITY_TYPE);
            identity.setIdentifier(identifier);
            identity.setUnionId(unionId);
            identity.setCredential(openId);
            identity.setCreateTime(LocalDateTime.now());
            identity.setUpdateTime(LocalDateTime.now());
            userAuthIdentityMapper.insert(identity);
            rbacService.ensureDefaultRole(user.getId(), UserConstant.DEFAULT_ROLE);
        } else {
            user = userMapper.selectOneById(identity.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "微信绑定用户不存在");
            }
        }
        return authTokenService.createLoginSession(user, request);
    }

    private Map<String, Object> requestJson(URI uri) {
        try {
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信登录请求失败");
        }
    }

    private void assertWechatSuccess(Map<String, Object> response) {
        if (response.containsKey("errcode")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信登录失败：" + response.get("errmsg"));
        }
    }
}
