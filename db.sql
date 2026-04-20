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
    providerName varchar(64)                             not null comment '提供者名称（如：qwen/zhipu/deepseek）',
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
       ('deepseek', 'DeepSeek', 'https://api.deepseek.com', 'YOUR_DEEPSEEK_API_KEY', 'active', 80);

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
(3, 'deepseek-coder', 'DeepSeek Coder', 'chat', 'DeepSeek代码模型', 32768, 0.001, 0.002, 90, 0);

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