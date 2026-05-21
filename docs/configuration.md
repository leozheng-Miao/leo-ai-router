# 配置项汇总

## 启动 Profile

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `local` | `prod` |
| `server.port` | `SERVER_PORT` | `8123` | 按部署端口设置 |

## MySQL

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `spring.datasource.url` | `LEO_DB_URL` | `jdbc:mysql://localhost:3306/leo_ai_router?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai` | 必填 |
| `spring.datasource.username` | `LEO_DB_USERNAME` | `root` | 必填 |
| `spring.datasource.password` | `LEO_DB_PASSWORD` | 见 `application-local.yml` | 必填 |

## Redis / Redisson

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `spring.data.redis.host` | `LEO_REDIS_HOST` | `127.0.0.1` | 必填 |
| `spring.data.redis.port` | `LEO_REDIS_PORT` | `6379` | 必填 |
| `spring.data.redis.database` | `LEO_REDIS_DATABASE` | `1` | 建议独立 DB，默认 `0` |
| `spring.data.redis.password` | `SPRING_DATA_REDIS_PASSWORD` | 不设置 | Redis 有密码时才设置；无密码时不要设置为空字符串 |
| `spring.data.redis.timeout` | `LEO_REDIS_TIMEOUT` | `5s` | 建议 `5s` 或更高 |

local 启动前先确认：

```bash
redis-cli -h 127.0.0.1 -p 6379 -n 1 ping
```

返回 `PONG` 后再启动后端。当前项目的 JWT refresh session、权限缓存、限流、验证码、黑名单、会话列表缓存都依赖 Redis/Redisson，Redis 不可达时后端会启动失败。

## JWT 与加密

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `auth.jwt.secret` | `LEO_AUTH_JWT_SECRET` | 见 `application-local.yml` | 必填，使用独立强随机密钥 |
| `auth.jwt.issuer` | `LEO_AUTH_JWT_ISSUER` | `leo-ai-router` | 必填 |
| `auth.jwt.access-token-ttl-seconds` | `LEO_AUTH_ACCESS_TOKEN_TTL_SECONDS` | `7200` | 按安全策略设置 |
| `auth.jwt.refresh-token-ttl-seconds` | `LEO_AUTH_REFRESH_TOKEN_TTL_SECONDS` | `2592000` | 按安全策略设置 |
| `encryption.secret-key` | `LEO_ENCRYPTION_SECRET_KEY` | `local-dev-encryption-secret-32bytes` | 必填，稳定保存，不能随意更换 |

## CORS 与前端地址

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `app.frontend-base-url` | `LEO_FRONTEND_BASE_URL` | `http://localhost:5173` | 前端线上域名 |
| `app.cors.allowed-origins` | `LEO_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 前端线上域名，多个域名用英文逗号分隔 |
| `app.cors.allowed-methods` | 无 | `GET,POST,PUT,DELETE,OPTIONS` | 通常无需改 |
| `app.cors.max-age-seconds` | 无 | `3600` | 通常无需改 |

## 异步线程池

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `app.async.core-pool-size` | `LEO_ASYNC_CORE_POOL_SIZE` | `8` | 按机器规格设置 |
| `app.async.max-pool-size` | `LEO_ASYNC_MAX_POOL_SIZE` | `32` | 按机器规格设置 |
| `app.async.queue-capacity` | `LEO_ASYNC_QUEUE_CAPACITY` | `200` | 按流量设置 |
| `app.async.request-timeout-millis` | `LEO_ASYNC_REQUEST_TIMEOUT_MILLIS` | `60000` | 按接口超时设置 |

## 邮件

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `mail.from` | `LEO_MAIL_FROM` | `1154027884@qq.com` | 发件邮箱 |
| `mail.code-expire` | `LEO_MAIL_CODE_EXPIRE_SECONDS` | `300` | 验证码有效期 |
| `mail.code-cool-down` | `LEO_MAIL_CODE_COOL_DOWN_SECONDS` | `60` | 验证码冷却时间 |

还需要按 Spring Boot Mail 规范配置：

| 配置项 | 环境变量示例 | 说明 |
| --- | --- | --- |
| `spring.mail.host` | `SPRING_MAIL_HOST` | SMTP host |
| `spring.mail.port` | `SPRING_MAIL_PORT` | SMTP port |
| `spring.mail.username` | `SPRING_MAIL_USERNAME` | SMTP 用户名 |
| `spring.mail.password` | `SPRING_MAIL_PASSWORD` | SMTP 密码或授权码 |
| `spring.mail.protocol` | `SPRING_MAIL_PROTOCOL` | 默认 `smtp` |
| `spring.mail.properties.mail.smtp.auth` | `SPRING_MAIL_SMTP_AUTH` | 默认 `true` |
| `spring.mail.properties.mail.smtp.ssl.enable` | `SPRING_MAIL_SMTP_SSL_ENABLE` | 默认 `true` |
| `spring.mail.properties.mail.smtp.starttls.enable` | `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | 默认 `false` |

## 腾讯短信

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `tencent.sms.enabled` | `TENCENT_SMS_ENABLED` | `false` | 使用短信时设为 `true` |
| `tencent.sms.secret-id` | `TENCENT_SMS_SECRET_ID` | 空 | 启用短信时必填 |
| `tencent.sms.secret-key` | `TENCENT_SMS_SECRET_KEY` | 空 | 启用短信时必填 |
| `tencent.sms.region` | `TENCENT_SMS_REGION` | `ap-guangzhou` | 按短信应用区域设置 |
| `tencent.sms.sdk-app-id` | `TENCENT_SMS_SDK_APP_ID` | 空 | 启用短信时必填 |
| `tencent.sms.sign-name` | `TENCENT_SMS_SIGN_NAME` | 空 | 启用短信时必填 |
| `tencent.sms.login-template-id` | `TENCENT_SMS_LOGIN_TEMPLATE_ID` | 空 | 启用短信时必填 |
| `tencent.sms.local-code-visible` | `TENCENT_SMS_LOCAL_CODE_VISIBLE` | `true` | prod 建议 `false` |
| `tencent.sms.code-expire-minutes` | `TENCENT_SMS_CODE_EXPIRE_MINUTES` | `5` | 验证码有效期 |
| `tencent.sms.code-cool-down-seconds` | `TENCENT_SMS_CODE_COOL_DOWN_SECONDS` | `60` | 验证码冷却时间 |

## 微信 OAuth

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `wechat.oauth.enabled` | `WECHAT_OAUTH_ENABLED` | `false` | 启用微信登录时设为 `true` |
| `wechat.oauth.app-id` | `WECHAT_OAUTH_APP_ID` | 空 | 启用时必填 |
| `wechat.oauth.app-secret` | `WECHAT_OAUTH_APP_SECRET` | 空 | 启用时必填 |
| `wechat.oauth.redirect-uri` | `WECHAT_OAUTH_REDIRECT_URI` | `http://localhost:5173/oauth/wechat/callback` | 线上回调地址 |
| `wechat.oauth.frontend-success-url` | `WECHAT_OAUTH_FRONTEND_SUCCESS_URL` | `http://localhost:5173/oauth/wechat/callback` | 线上前端成功页 |
| `wechat.oauth.state-ttl-seconds` | `WECHAT_OAUTH_STATE_TTL_SECONDS` | `300` | state 有效期 |

## Stripe

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `stripe.api-key` | `STRIPE_API_KEY` | 空 | 启用 Stripe 时必填 |
| `stripe.webhook-secret` | `STRIPE_WEBHOOK_SECRET` | 空 | 启用 Stripe Webhook 时必填 |
| `stripe.success-url` | `STRIPE_SUCCESS_URL` | `http://localhost:8123/api/recharge/stripe/success` | 线上后端成功回调 |
| `stripe.cancel-url` | `STRIPE_CANCEL_URL` | `http://localhost:8123/api/recharge/stripe/cancel` | 线上后端取消回调 |

## 支付宝

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `payment.alipay.app-id` | `ALIPAY_APP_ID` | 空 | 启用支付宝时必填 |
| `payment.alipay.app-private-key` | `ALIPAY_APP_PRIVATE_KEY` | 空 | 启用支付宝时必填 |
| `payment.alipay.alipay-public-key` | `ALIPAY_PUBLIC_KEY` | 空 | 启用支付宝时必填 |
| `payment.alipay.gateway-url` | `ALIPAY_GATEWAY_URL` | `https://openapi-sandbox.dl.alipaydev.com/gateway.do` | 生产用 `https://openapi.alipay.com/gateway.do` |
| `payment.alipay.notify-url` | `ALIPAY_NOTIFY_URL` | `http://localhost:8123/api/recharge/alipay/notify` | 线上后端异步通知地址 |
| `payment.alipay.return-url` | `ALIPAY_RETURN_URL` | `http://localhost:8123/api/recharge/alipay/return` | 线上后端同步回跳地址 |
| `payment.alipay.frontend-success-url` | `ALIPAY_FRONTEND_SUCCESS_URL` | `http://localhost:5173/recharge/success?method=alipay` | 线上前端成功页 |
| `payment.alipay.frontend-cancel-url` | `ALIPAY_FRONTEND_CANCEL_URL` | `http://localhost:5173/recharge/cancel?method=alipay` | 线上前端取消页 |

## Actuator / Knife4j

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `knife4j.enable` | `LEO_KNIFE4J_ENABLE` | `true` | `false` |
| `management.endpoints.web.exposure.include` | 无 | `health,info,prometheus,metrics` | `health,info,prometheus` |
| `management.endpoint.health.show-details` | 无 | `always` | `never` |

## 前端 Vite

| 配置项 | 环境变量 | local 示例值 | prod 要求 |
| --- | --- | --- | --- |
| `VITE_API_BASE_URL` | `VITE_API_BASE_URL` | `http://localhost:8123/api` | 线上后端 API 地址 |
| `VITE_OPENAPI_SCHEMA_URL` | `VITE_OPENAPI_SCHEMA_URL` | `http://localhost:8123/api/v3/api-docs` | 生成 API 类型时使用 |
