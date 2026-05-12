-- 创建库
create database if not exists leo_ai_router;

-- 切换库
use leo_ai_router;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    userStatus   varchar(32)  default 'active' comment '用户状态：active-正常，disabled-禁用',
    tokenQuota   bigint       default -1 comment 'Token配额（-1表示无限制）',
    usedTokens   bigint       default 0 comment '已使用Token数',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName),
    INDEX idx_userStatus (userStatus)
) comment '用户' collate = utf8mb4_unicode_ci;
ALTER TABLE user ADD COLUMN balance decimal(12, 4) default 0.0000 comment '账户余额（元）';

use leo_ai_router;
-- 密码是 12345678(MD5 加密 + 盐值 yupi)
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, tokenQuota)
VALUES (1, 'admin', 'f6558f4cfe0b984f475e2eff21768a63', '管理员', 'https://www.codefather.cn/logo.png', '系统管理员', 'admin', -1),
       (2, 'user', 'f6558f4cfe0b984f475e2eff21768a63', '普通用户', 'https://www.codefather.cn/logo.png', '我是一个普通用户', 'user', 100000),
       (3, 'test', 'f6558f4cfe0b984f475e2eff21768a63', '测试账号', 'https://www.codefather.cn/logo.png', '这是一个测试账号', 'user', 50000);

ALTER TABLE user
    ADD COLUMN userEmail VARCHAR(128) NULL COMMENT '用户邮箱' AFTER userAccount;

-- 请求日志表（用于记录每次请求和Token消耗）
create table if not exists request_log
(
    id               bigint auto_increment comment 'id' primary key,
    userId           bigint                                null comment '用户id',
    apiKeyId         bigint                                null comment 'API Key id',
    modelName        varchar(128)                          not null comment '使用的模型名称',
    promptTokens     int         default 0                 not null comment '输入Token数',
    completionTokens int         default 0                 not null comment '输出Token数',
    totalTokens      int         default 0                 not null comment '总Token数',
    duration         int         default 0                 not null comment '请求耗时（毫秒）',
    status           varchar(32) default 'success'         not null comment '状态：success/failed',
    errorMessage     text                                  null comment '错误信息',
    createTime       datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    INDEX idx_userId (userId),
    INDEX idx_apiKeyId (apiKeyId),
    INDEX idx_createTime (createTime)
) comment '请求日志' collate = utf8mb4_unicode_ci;

ALTER TABLE request_log

-- 链路追踪
    ADD COLUMN traceId         VARCHAR(64)       NULL COMMENT '链路追踪ID' AFTER id,

-- 模型相关
    ADD COLUMN modelId         BIGINT            NULL COMMENT '实际调用的模型id' AFTER apiKeyId,
    ADD COLUMN requestModel    VARCHAR(128)      NULL COMMENT '请求的模型标识' AFTER modelId,

-- 请求维度
    ADD COLUMN requestType     VARCHAR(32)       NULL COMMENT '请求类型：chat/embedding/image' AFTER modelName,
    ADD COLUMN source          VARCHAR(32)       NULL COMMENT '调用来源：web/api' AFTER requestType,

-- 错误信息增强
    ADD COLUMN errorCode       VARCHAR(64)       NULL COMMENT '错误码' AFTER errorMessage,

-- 路由 & 容灾
    ADD COLUMN routingStrategy VARCHAR(64)       NULL COMMENT '使用的路由策略' AFTER errorCode,
    ADD COLUMN isFallback      TINYINT DEFAULT 0 NULL COMMENT '是否为Fallback请求' AFTER routingStrategy,

-- 客户端信息
    ADD COLUMN clientIp        VARCHAR(64)       NULL COMMENT '客户端IP' AFTER isFallback,
    ADD COLUMN userAgent       VARCHAR(255)      NULL COMMENT 'User-Agent' AFTER clientIp;
alter table request_log
    add column cost decimal(12,6) default 0 comment '本次请求费用（元）';


-- API Key 表
create table if not exists api_key
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                                not null comment '用户id',
    keyValue     varchar(128)                          not null comment 'API Key值（sk-xxx格式）',
    keyName      varchar(128)                          null comment 'Key名称/备注',
    status       varchar(32) default 'active'          not null comment '状态：active/inactive/revoked',
    totalTokens  bigint      default 0                 not null comment '已使用Token总数',
    lastUsedTime datetime                              null comment '最后使用时间',
    createTime   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint     default 0                 not null comment '是否删除',
    UNIQUE KEY uk_keyValue (keyValue),
    INDEX idx_userId (userId),
    INDEX idx_status (status)
) comment 'API Key' collate = utf8mb4_unicode_ci;

create table if not exists model_provider
(
    id           bigint auto_increment comment 'id' primary key,
    providerName varchar(64)                             not null comment '提供者名称（如：qwen/zhipu/deepseek/openai/gemini）',
    displayName  varchar(128)                            not null comment '显示名称（如：通义千问/智谱AI/DeepSeek）',
    baseUrl      varchar(512)                            not null comment 'API基础URL',
    apiKey       varchar(512)                            not null comment 'API密钥',
    status       varchar(32)   default 'active'          not null comment '状态：active/inactive/maintenance',
    healthStatus varchar(32)   default 'unknown'         not null comment '健康状态：healthy/unhealthy/degraded/unknown',
    avgLatency   int           default 0                 not null comment '平均延迟（毫秒）',
    successRate  decimal(5, 2) default 100.00            not null comment '成功率（百分比）',
    priority     int           default 100               not null comment '优先级（越大越优先）',
    config       text                                    null comment '额外配置（JSON格式）',
    createTime   datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint       default 0                 not null comment '是否删除',
    UNIQUE KEY uk_providerName (providerName),
    INDEX idx_status (status),
    INDEX idx_healthStatus (healthStatus)
) comment '模型提供者' collate = utf8mb4_unicode_ci;

create table if not exists model
(
    id               bigint auto_increment comment 'id' primary key,
    providerId       bigint                                   not null comment '提供者id',
    modelKey         varchar(128)                             not null comment '模型标识（如：qwen-plus）',
    modelName        varchar(128)                             not null comment '模型显示名称',
    modelType        varchar(32)    default 'chat'            not null comment '模型类型：chat/embedding/image/audio',
    description      varchar(512)                             null comment '模型描述',
    contextLength    int            default 4096              not null comment '上下文长度限制',
    inputPrice       decimal(10, 6) default 0                 not null comment '输入价格（元/千Token）',
    outputPrice      decimal(10, 6) default 0                 not null comment '输出价格（元/千Token）',
    status           varchar(32)    default 'active'          not null comment '状态：active/inactive/deprecated',
    healthStatus     varchar(32)    default 'unknown'         not null comment '健康状态：healthy/unhealthy/degraded/unknown',
    avgLatency       int            default 0                 not null comment '平均延迟（毫秒）',
    successRate      decimal(5, 2)  default 100.00            not null comment '成功率（百分比）',
    score            decimal(10, 4) default 0                 not null comment '综合得分（越低越好）',
    priority         int            default 100               not null comment '优先级（越大越优先）',
    defaultTimeout   int            default 60000             not null comment '默认超时时间（毫秒）',
    supportReasoning tinyint        default 0                 not null comment '是否支持深度思考：0=不支持，1=支持',
    capabilities     varchar(512)                             null comment '能力标签（JSON数组）',
    createTime       datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete         tinyint        default 0                 not null comment '是否删除',
    UNIQUE KEY uk_modelKey (modelKey),
    INDEX idx_providerId (providerId),
    INDEX idx_modelType (modelType),
    INDEX idx_status (status),
    INDEX idx_healthStatus (healthStatus)
) comment '模型' collate = utf8mb4_unicode_ci;

-- 初始化模型提供者
INSERT INTO model_provider (providerName, displayName, baseUrl, apiKey, status, priority)
VALUES ('qwen', '通义千问', 'https://dashscope.aliyuncs.com/compatible-mode', 'YOUR_QWEN_API_KEY', 'active', 100),
       ('zhipu', '智谱AI', 'https://open.bigmodel.cn/api/paas', 'YOUR_ZHIPU_API_KEY', 'active', 90),
       ('deepseek', 'DeepSeek', 'https://api.deepseek.com', 'YOUR_DEEPSEEK_API_KEY', 'active', 80),
       ('openai', 'OpenAI', 'https://api.openai.com', 'YOUR_OPENAI_API_KEY', 'active', 95),
       ('gemini', 'Gemini', 'https://generativelanguage.googleapis.com', 'YOUR_GEMINI_API_KEY', 'active', 92);

-- 初始化模型数据
INSERT INTO model (providerId, modelKey, modelName, modelType, description, contextLength, inputPrice, outputPrice,
                   priority, supportReasoning)
VALUES
-- 通义千问模型
(1, 'qwen-plus', 'Qwen Plus', 'chat', '通义千问增强版，性能更强', 32768, 0.004, 0.004, 100, 1),
(1, 'qwen-turbo', 'Qwen Turbo', 'chat', '通义千问快速版，响应更快', 8192, 0.002, 0.002, 90, 0),
(1, 'qwen-max', 'Qwen Max', 'chat', '通义千问旗舰版，能力最强，支持深度思考', 32768, 0.04, 0.04, 110, 1),

-- 智谱AI模型
(2, 'glm-4.7-flash', 'GLM-4.7 Flash', 'chat', '智谱AI免费模型', 204800, 0.0001, 0.0001, 80, 0),
(2, 'glm-4.7', 'GLM-4.7', 'chat', '智谱AI高智能旗舰模型，通用对话、推理与智能体能力全面升级', 204800, 0.05, 0.05, 100, 1),
(2, 'glm-4.6', 'GLM-4.6', 'chat', '智谱AI超强性能模型，高级编码能力、强大推理以及工具调用能力', 204800, 0.05, 0.05, 90,
 1),

-- DeepSeek模型
(3, 'deepseek-reasoner', 'DeepSeek Reasoner', 'chat', 'DeepSeek对话模型，支持深度思考', 32768, 0.001, 0.002, 100, 1),
(3, 'deepseek-chat', 'DeepSeek Chat', 'chat', 'DeepSeek对话模型', 32768, 0.001, 0.002, 100, 0),
(3, 'deepseek-coder', 'DeepSeek Coder', 'chat', 'DeepSeek代码模型', 32768, 0.001, 0.002, 90, 0),

-- OpenAI 模型
(4, 'gpt-5.5', 'GPT-5.5', 'chat', 'OpenAI 旗舰推理与编码模型', 1000000, 0, 0, 105, 1),
(4, 'gpt-5.4', 'GPT-5.4', 'chat', 'OpenAI 高性能专业工作模型', 1000000, 0, 0, 100, 1),
(4, 'gpt-5.4-mini', 'GPT-5.4 Mini', 'chat', 'OpenAI 轻量高吞吐推理模型', 400000, 0, 0, 95, 1),

-- Gemini 模型
(5, 'gemini-3.1-pro-preview', 'Gemini 3.1 Pro', 'chat', 'Gemini 3.1 Pro 预览模型，适用于复杂推理与智能体工作流', 1048576, 0, 0, 102, 1),
(5, 'gemini-3-flash-preview', 'Gemini-3-Flash', 'chat', 'Gemini 3 Flash 预览模型，适用于高吞吐低延迟任务', 1048576, 0, 0, 98, 1),
(5, 'gemini-3.1-flash-lite', 'Gemini-3.1-Flash-Lite', 'chat', 'Gemini 3.1 Flash-Lite，轻量高性价比模型', 1048576, 0, 0, 94, 1);

create table if not exists recharge_record
(
    id            bigint auto_increment comment 'id' primary key,
    userId        bigint                                not null comment '用户id',
    amount        decimal(12, 4)                        not null comment '充值金额（元）',
    status        varchar(32) default 'pending'         not null comment '充值状态：pending/success/failed',
    paymentMethod varchar(32)                           null comment '支付方式：stripe/alipay/wechat',
    paymentId     varchar(256)                          null comment '支付平台订单ID',
    description   varchar(512)                          null comment '充值说明',
    createTime    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId),
    index idx_status (status),
    index idx_paymentId (paymentId)
) comment '充值记录' collate = utf8mb4_unicode_ci;

create table if not exists billing_record
(
    id            bigint auto_increment comment 'id' primary key,
    userId        bigint                                not null comment '用户id',
    requestLogId  bigint                                null comment '关联的请求日志ID',
    amount        decimal(12, 4)                        not null comment '消费金额（元）',
    balanceBefore decimal(12, 4)                        not null comment '消费前余额（元）',
    balanceAfter  decimal(12, 4)                        not null comment '消费后余额（元）',
    description   varchar(512)                          null comment '消费说明',
    billingType   varchar(32) default 'api_call'        not null comment '账单类型：api_call/recharge/refund',
    createTime    datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    index idx_billingType (billingType),
    index idx_createTime (createTime),
    index idx_requestLogId (requestLogId),
    index idx_userId (userId)
) comment '消费账单' collate = utf8mb4_unicode_ci;

-- 图片生成记录表
create table image_generation_record
(
    id            bigint auto_increment comment 'id'
        primary key,
    userId        bigint                                   not null comment '用户id',
    apiKeyId      bigint                                   null comment 'API Key id',
    modelId       bigint                                   not null comment '使用的模型id',
    modelKey      varchar(128)                             not null comment '模型标识',
    prompt        text                                     not null comment '生成提示词',
    revisedPrompt text                                     null comment '修订后的提示词',
    imageUrl      varchar(1024)                            null comment '图片URL',
    imageData     longtext                                 null comment 'Base64图片数据',
    size          varchar(32)                              null comment '图片尺寸',
    quality       varchar(32)                              null comment '图片质量',
    status        varchar(32)    default 'success'         not null comment '状态：success/failed',
    cost          decimal(12, 4) default 0.0000            not null comment '生成费用（元）',
    duration      int                                      null comment '耗时（毫秒）',
    errorMessage  varchar(512)                             null comment '错误信息',
    clientIp      varchar(128)                             null comment '客户端IP',
    createTime    datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    INDEX idx_userId (userId),
    INDEX idx_modelId (modelId)
) comment '图片生成记录' collate = utf8mb4_unicode_ci;

INSERT INTO model (providerId, modelKey, modelName, modelType, description, contextLength, inputPrice, outputPrice, priority, supportReasoning)
VALUES
    -- 阶段七：绘图模型
    (1, 'qwen-image-plus', 'Qwen Image Plus', 'image', '通义万相文生图模型', 0, 0.08, 0, 100, 0),
    (4, 'gpt-image-2', 'GPT Image 2', 'image', 'OpenAI 高质量图像生成模型', 0, 0, 0, 98, 0),
    (4, 'gpt-image-1.5', 'GPT Image 1.5', 'image', 'OpenAI 图像生成模型', 0, 0, 0, 96, 0),
    (5, 'gemini-3.1-flash-image-preview', 'Nano Banana 2', 'image', 'Gemini 3.1 Flash 图片预览模型，支持原生图片生成', 0, 0, 0, 97, 0),
    (5, 'gemini-3-pro-image-preview', 'Nano Banana Pro', 'image', 'Gemini 3 Pro 图片预览模型，适用于高质量视觉创作', 0, 0, 0, 95, 0);

-- 插件配置表
create table if not exists plugin_config
(
    id          bigint auto_increment comment 'id' primary key,
    pluginKey   varchar(64)                           not null comment '插件标识：web_search/pdf_parser/image_recognition',
    pluginName  varchar(128)                          not null comment '插件名称',
    pluginType  varchar(32) default 'builtin'         not null comment '插件类型：builtin/custom',
    description varchar(512)                          null comment '插件描述',
    config      text                                  null comment '插件配置（JSON）',
    status      varchar(32) default 'active'          not null comment '状态：active/inactive',
    priority    int         default 100               not null comment '优先级',
    createTime  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint     default 0                 not null comment '是否删除',
    UNIQUE KEY uk_pluginKey (pluginKey)
) comment '插件配置' collate = utf8mb4_unicode_ci;

-- 初始化插件数据
-- 注意：Web搜索插件的 API Key 配置在 application-local.yml 中（plugin.serpapi.api-key）
-- SerpApi 注册地址：https://serpapi.com/
INSERT INTO plugin_config (pluginKey, pluginName, pluginType, description, config, status, priority)
VALUES ('web_search', 'Web搜索', 'builtin', '实时联网搜索（SerpApi）', '{"maxResults":5,"searchEngine":"google","timeout":15000}', 'active', 100),
       ('pdf_parser', 'PDF解析', 'builtin', '解析PDF文档内容，提取文本信息', '{"maxPages":50,"maxTextLength":50000}', 'active', 90),
       ('image_recognition', '图片识别', 'builtin', '识别图片内容，返回图片描述', '{"model":"qwen-vl-plus","maxImageSize":4194304}', 'active', 80);


create table if not exists user_provider_key
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                                 not null comment '用户 ID',
    providerId   bigint                                 not null comment '提供者 ID',
    providerName varchar(64)                            not null comment '提供者名称（冗余字段，便于查询）',
    apiKey       varchar(512)                           not null comment 'API Key（加密存储）',
    status       varchar(32)  default 'active'          not null comment '状态：active/inactive',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    KEY uk_user_provider (userId, providerId)
) comment '用户提供者密钥（BYOK）' collate = utf8mb4_unicode_ci;
#     (2, 'cogview-3-plus', 'CogView-3-Plus', 'image', '智谱AI文生图模型', 0, 0.1, 0, 90, 0);
CREATE TABLE IF NOT EXISTS conversation
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED                 NOT NULL,
    title           VARCHAR(255) DEFAULT '新对话'   NULL,
    conv_type       TINYINT      DEFAULT 1          NOT NULL,
    last_message_at DATETIME(3)                     NOT NULL,
    created_at      DATETIME(3)  DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    is_deleted      TINYINT      DEFAULT 0          NOT NULL,
    INDEX idx_user_list (user_id, is_deleted, last_message_at DESC)
) COMMENT '会话表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversation_message
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    role            VARCHAR(16)     NOT NULL,
    content         MEDIUMTEXT      NOT NULL,
    mode            TINYINT DEFAULT 1 NOT NULL,
    token_count     INT             NULL,
    seq             BIGINT UNSIGNED NOT NULL,
    created_at      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    INDEX idx_conv_seq (conversation_id, seq ASC),
    INDEX idx_user_id (user_id)
) COMMENT '会话消息表' COLLATE = utf8mb4_unicode_ci;

-- 登录认证与 RBAC 扩展
ALTER TABLE user
    ADD COLUMN userPhone VARCHAR(32) NULL COMMENT '用户手机号' AFTER userEmail,
    ADD COLUMN phoneVerified TINYINT DEFAULT 0 NOT NULL COMMENT '手机号是否验证' AFTER userPhone,
    ADD COLUMN tokenVersion INT DEFAULT 0 NOT NULL COMMENT 'JWT token版本，用于强制失效' AFTER balance;

CREATE TABLE IF NOT EXISTS role
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    roleCode    VARCHAR(64)                           NOT NULL COMMENT '角色编码',
    roleName    VARCHAR(128)                          NOT NULL COMMENT '角色名称',
    description VARCHAR(512)                          NULL COMMENT '角色描述',
    status      VARCHAR(32) DEFAULT 'active'          NOT NULL COMMENT '状态：active/inactive',
    priority    INT         DEFAULT 0                 NOT NULL COMMENT '优先级',
    createTime  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime  DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete    TINYINT     DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_roleCode (roleCode)
) COMMENT '角色表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permission
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    permissionCode VARCHAR(128)                          NOT NULL COMMENT '权限编码',
    permissionName VARCHAR(128)                          NOT NULL COMMENT '权限名称',
    resourceType   VARCHAR(32)                           NULL COMMENT '资源类型',
    resourceKey    VARCHAR(128)                          NULL COMMENT '资源标识',
    description    VARCHAR(512)                          NULL COMMENT '权限描述',
    status         VARCHAR(32) DEFAULT 'active'          NOT NULL,
    createTime     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete       TINYINT     DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_permissionCode (permissionCode)
) COMMENT '权限表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permission
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    roleCode       VARCHAR(64)                        NOT NULL,
    permissionCode VARCHAR(128)                       NOT NULL,
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_role_permission (roleCode, permissionCode),
    INDEX idx_permissionCode (permissionCode)
) COMMENT '角色权限关联表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_role
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId     BIGINT                             NOT NULL,
    roleCode   VARCHAR(64)                        NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_role (userId, roleCode),
    INDEX idx_roleCode (roleCode)
) COMMENT '用户角色关联表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_plan_limit
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    roleCode            VARCHAR(64)                           NOT NULL,
    tokenQuota          BIGINT      DEFAULT 0                 NOT NULL COMMENT 'token额度，-1无限制',
    dailyRequestLimit   BIGINT      DEFAULT 0                 NOT NULL COMMENT '每日对话次数，-1无限制',
    monthlyRequestLimit BIGINT      DEFAULT 0                 NOT NULL COMMENT '每月对话次数，-1无限制',
    dailyImageLimit     BIGINT      DEFAULT 0                 NOT NULL COMMENT '每日图片次数，-1无限制',
    dailyPluginLimit    BIGINT      DEFAULT 0                 NOT NULL COMMENT '每日插件次数，-1无限制',
    apiKeyLimit         INT         DEFAULT 0                 NOT NULL COMMENT 'API Key数量上限，-1无限制',
    allowByok           TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否允许BYOK',
    createTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_roleCode (roleCode)
) COMMENT '角色套餐额度表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_auth_identity
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId       BIGINT                             NOT NULL,
    identityType VARCHAR(64)                        NOT NULL COMMENT '身份类型：wechat_open',
    identifier   VARCHAR(128)                       NOT NULL COMMENT 'openid或unionid',
    unionId      VARCHAR(128)                       NULL,
    credential   VARCHAR(256)                       NULL COMMENT '冗余凭证，如openid',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT  DEFAULT 0                 NOT NULL,
    UNIQUE KEY uk_identity (identityType, identifier),
    INDEX idx_userId (userId)
) COMMENT '用户第三方身份表' COLLATE = utf8mb4_unicode_ci;

INSERT INTO role (roleCode, roleName, description, status, priority)
VALUES ('user', '普通用户', '基础模型与基础额度', 'active', 10),
       ('vip', 'VIP用户', '高级模型、联网插件与更高额度', 'active', 50),
       ('admin', '管理员', '系统管理与全部资源权限', 'active', 100)
ON DUPLICATE KEY UPDATE roleName = VALUES(roleName), description = VALUES(description), status = VALUES(status), priority = VALUES(priority);

INSERT INTO permission (permissionCode, permissionName, resourceType, resourceKey, description, status)
VALUES ('chat:use', '对话调用', 'feature', 'chat', '允许使用文本对话', 'active'),
       ('image:use', '图片生成', 'feature', 'image', '允许使用图片生成', 'active'),
       ('plugin:use', '插件使用', 'feature', 'plugin', '允许执行插件', 'active'),
       ('apikey:manage', 'API Key管理', 'feature', 'apikey', '允许管理API Key', 'active'),
       ('byok:manage', 'BYOK管理', 'feature', 'byok', '允许配置自有模型密钥', 'active'),
       ('model:*', '全部模型', 'model', '*', '允许使用全部模型', 'active'),
       ('plugin:*', '全部插件', 'plugin', '*', '允许使用全部插件', 'active'),
       ('model:qwen-plus', 'Qwen Plus', 'model', 'qwen-plus', '允许使用Qwen Plus', 'active'),
       ('model:qwen-turbo', 'Qwen Turbo', 'model', 'qwen-turbo', '允许使用Qwen Turbo', 'active'),
       ('model:deepseek-chat', 'DeepSeek Chat', 'model', 'deepseek-chat', '允许使用DeepSeek Chat', 'active'),
       ('model:deepseek-coder', 'DeepSeek Coder', 'model', 'deepseek-coder', '允许使用DeepSeek Coder', 'active'),
       ('plugin:web_search', '联网搜索', 'plugin', 'web_search', '允许使用联网搜索', 'active'),
       ('plugin:pdf_parser', 'PDF解析', 'plugin', 'pdf_parser', '允许使用PDF解析', 'active'),
       ('plugin:image_recognition', '图片识别', 'plugin', 'image_recognition', '允许使用图片识别', 'active'),
       ('admin:user', '用户管理', 'admin', 'user', '允许管理用户', 'active'),
       ('admin:model', '模型管理', 'admin', 'model', '允许管理模型', 'active'),
       ('admin:provider', '提供者管理', 'admin', 'provider', '允许管理模型提供者', 'active'),
       ('admin:plugin', '插件管理', 'admin', 'plugin', '允许管理插件', 'active'),
       ('admin:rbac', '权限管理', 'admin', 'rbac', '允许管理角色权限', 'active'),
       ('stats:view', '统计查看', 'admin', 'stats', '允许查看统计数据', 'active')
ON DUPLICATE KEY UPDATE permissionName = VALUES(permissionName), description = VALUES(description), status = VALUES(status);

INSERT INTO role_permission (roleCode, permissionCode)
VALUES ('user', 'chat:use'),
       ('user', 'image:use'),
       ('user', 'apikey:manage'),
       ('user', 'model:qwen-plus'),
       ('user', 'model:qwen-turbo'),
       ('user', 'model:deepseek-chat'),
       ('user', 'model:deepseek-coder'),
       ('vip', 'chat:use'),
       ('vip', 'image:use'),
       ('vip', 'plugin:use'),
       ('vip', 'apikey:manage'),
       ('vip', 'byok:manage'),
       ('vip', 'model:*'),
       ('vip', 'plugin:*'),
       ('admin', '*')
ON DUPLICATE KEY UPDATE permissionCode = VALUES(permissionCode);

INSERT INTO role_plan_limit (roleCode, tokenQuota, dailyRequestLimit, monthlyRequestLimit, dailyImageLimit, dailyPluginLimit, apiKeyLimit, allowByok)
VALUES ('user', 100000, 100, 2000, 10, 0, 3, 0),
       ('vip', 1000000, 1000, 30000, 100, 500, 10, 1),
       ('admin', -1, -1, -1, -1, -1, -1, 1)
ON DUPLICATE KEY UPDATE tokenQuota = VALUES(tokenQuota),
                        dailyRequestLimit = VALUES(dailyRequestLimit),
                        monthlyRequestLimit = VALUES(monthlyRequestLimit),
                        dailyImageLimit = VALUES(dailyImageLimit),
                        dailyPluginLimit = VALUES(dailyPluginLimit),
                        apiKeyLimit = VALUES(apiKeyLimit),
                        allowByok = VALUES(allowByok);

INSERT IGNORE INTO user_role (userId, roleCode)
SELECT id, userRole FROM user WHERE isDelete = 0 AND userRole IS NOT NULL;