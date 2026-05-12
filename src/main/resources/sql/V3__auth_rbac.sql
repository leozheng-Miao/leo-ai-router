ALTER TABLE user
    ADD COLUMN IF NOT EXISTS userPhone VARCHAR(32) NULL COMMENT '用户手机号' AFTER userEmail,
    ADD COLUMN IF NOT EXISTS phoneVerified TINYINT DEFAULT 0 NOT NULL COMMENT '手机号是否验证' AFTER userPhone,
    ADD COLUMN IF NOT EXISTS tokenVersion INT DEFAULT 0 NOT NULL COMMENT 'JWT token版本，用于强制失效' AFTER balance;

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
    roleCode       VARCHAR(64)                           NOT NULL,
    permissionCode VARCHAR(128)                          NOT NULL,
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL,
    UNIQUE KEY uk_role_permission (roleCode, permissionCode),
    INDEX idx_permissionCode (permissionCode)
) COMMENT '角色权限关联表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_role
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId     BIGINT                              NOT NULL,
    roleCode   VARCHAR(64)                         NOT NULL,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL,
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
    userId       BIGINT                                NOT NULL,
    identityType VARCHAR(64)                           NOT NULL COMMENT '身份类型：wechat_open',
    identifier   VARCHAR(128)                          NOT NULL COMMENT 'openid或unionid',
    unionId       VARCHAR(128)                          NULL,
    credential   VARCHAR(256)                          NULL COMMENT '冗余凭证，如openid',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL,
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP    NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete     TINYINT  DEFAULT 0                    NOT NULL,
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
