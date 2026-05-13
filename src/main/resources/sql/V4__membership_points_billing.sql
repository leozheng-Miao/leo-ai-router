ALTER TABLE model
    ADD COLUMN IF NOT EXISTS accessTier VARCHAR(32) DEFAULT 'free' NOT NULL COMMENT '访问等级：free/pro/advanced/image/video' AFTER modelType,
    ADD COLUMN IF NOT EXISTS pointCost INT DEFAULT 0 NOT NULL COMMENT '积分成本：图片/视频单次消耗积分' AFTER outputPrice;

UPDATE model
SET accessTier = CASE
    WHEN modelType = 'image' THEN 'image'
    WHEN modelKey LIKE 'gpt-%' THEN 'advanced'
    WHEN modelKey LIKE 'gemini-3.1-pro%' THEN 'advanced'
    WHEN modelKey LIKE 'deepseek-reasoner%' THEN 'advanced'
    WHEN modelKey LIKE 'qwen-turbo%' THEN 'free'
    ELSE 'pro'
END
WHERE accessTier IS NULL OR accessTier = 'free';

UPDATE model
SET pointCost = CASE
    WHEN modelType = 'image' AND pointCost = 0 THEN 10
    ELSE pointCost
END;

CREATE TABLE IF NOT EXISTS subscription_plan
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    planCode           VARCHAR(64)                            NOT NULL COMMENT '套餐编码',
    planName           VARCHAR(128)                           NOT NULL COMMENT '套餐名称',
    description        VARCHAR(512)                           NULL COMMENT '套餐描述',
    price              DECIMAL(12, 4) DEFAULT 0.0000          NOT NULL COMMENT '价格（元）',
    originalPrice      DECIMAL(12, 4)                         NULL COMMENT '原价（元）',
    durationDays       INT            DEFAULT 0               NOT NULL COMMENT '有效天数，0表示永久',
    lifetime           TINYINT        DEFAULT 0               NOT NULL COMMENT '是否永久版',
    dailyProLimit      BIGINT         DEFAULT 0               NOT NULL COMMENT '普通/Pro模型每日次数，-1无限制',
    dailyAdvancedLimit BIGINT         DEFAULT 0               NOT NULL COMMENT '高级模型每日次数，-1无限制',
    monthlyProLimit    BIGINT         DEFAULT -1              NOT NULL COMMENT '普通/Pro模型每月次数，-1无限制',
    monthlyAdvancedLimit BIGINT       DEFAULT -1              NOT NULL COMMENT '高级模型每月次数，-1无限制',
    bonusPoints        BIGINT         DEFAULT 0               NOT NULL COMMENT '购买赠送积分',
    apiKeyLimit        INT            DEFAULT 3               NOT NULL COMMENT 'API Key数量上限，-1无限制',
    allowByok          TINYINT        DEFAULT 0               NOT NULL COMMENT '是否允许BYOK',
    priority           INT            DEFAULT 0               NOT NULL COMMENT '套餐等级，越大越高',
    visible            TINYINT        DEFAULT 1               NOT NULL COMMENT '前台是否展示',
    status             VARCHAR(32)    DEFAULT 'active'        NOT NULL COMMENT '状态：active/inactive',
    createTime         DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime         DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete           TINYINT        DEFAULT 0               NOT NULL,
    UNIQUE KEY uk_planCode (planCode),
    INDEX idx_status_visible (status, visible),
    INDEX idx_priority (priority)
) COMMENT '订阅套餐表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS point_package
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    packageCode    VARCHAR(64)                            NOT NULL COMMENT '积分包编码',
    packageName    VARCHAR(128)                           NOT NULL COMMENT '积分包名称',
    points         BIGINT                                 NOT NULL COMMENT '积分数量',
    price          DECIMAL(12, 4)                         NOT NULL COMMENT '价格（元）',
    originalPrice  DECIMAL(12, 4)                         NULL COMMENT '原价（元）',
    description    VARCHAR(512)                           NULL COMMENT '描述',
    badge          VARCHAR(64)                            NULL COMMENT '标签',
    priority       INT            DEFAULT 0               NOT NULL COMMENT '排序，越大越靠前',
    visible        TINYINT        DEFAULT 1               NOT NULL COMMENT '前台是否展示',
    status         VARCHAR(32)    DEFAULT 'active'        NOT NULL COMMENT '状态：active/inactive',
    createTime     DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime     DATETIME       DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete       TINYINT        DEFAULT 0               NOT NULL,
    UNIQUE KEY uk_packageCode (packageCode),
    INDEX idx_status_visible (status, visible),
    INDEX idx_priority (priority)
) COMMENT '积分包表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_subscription
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId          BIGINT                              NOT NULL COMMENT '用户id',
    planCode        VARCHAR(64)                         NOT NULL COMMENT '套餐编码',
    planName        VARCHAR(128)                        NOT NULL COMMENT '套餐名称快照',
    priority        INT         DEFAULT 0               NOT NULL COMMENT '套餐等级快照',
    startTime       DATETIME                            NOT NULL COMMENT '开始时间',
    endTime         DATETIME                            NULL COMMENT '结束时间，永久版为空',
    lifetime        TINYINT     DEFAULT 0               NOT NULL COMMENT '是否永久',
    status          VARCHAR(32) DEFAULT 'active'        NOT NULL COMMENT '状态：active/expired/cancelled',
    paymentOrderId  BIGINT                              NULL COMMENT '来源订单id',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete        TINYINT     DEFAULT 0               NOT NULL,
    INDEX idx_user_status (userId, status),
    INDEX idx_planCode (planCode),
    INDEX idx_endTime (endTime)
) COMMENT '用户订阅表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS point_account
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId          BIGINT                              NOT NULL COMMENT '用户id',
    balance         BIGINT      DEFAULT 0               NOT NULL COMMENT '可用积分',
    totalGranted    BIGINT      DEFAULT 0               NOT NULL COMMENT '累计获得积分',
    totalConsumed   BIGINT      DEFAULT 0               NOT NULL COMMENT '累计消耗积分',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_userId (userId)
) COMMENT '积分账户表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS point_transaction
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId          BIGINT                              NOT NULL COMMENT '用户id',
    changeAmount    BIGINT                              NOT NULL COMMENT '变动积分，正数增加，负数扣减',
    balanceBefore   BIGINT                              NOT NULL COMMENT '变动前积分',
    balanceAfter    BIGINT                              NOT NULL COMMENT '变动后积分',
    transactionType VARCHAR(32)                         NOT NULL COMMENT '类型：purchase/plan_bonus/image_consume/video_consume/admin_adjust/refund',
    refType         VARCHAR(32)                         NULL COMMENT '关联类型',
    refId           BIGINT                              NULL COMMENT '关联id',
    description     VARCHAR(512)                        NULL COMMENT '说明',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_user_time (userId, createTime),
    INDEX idx_ref (refType, refId),
    INDEX idx_type (transactionType)
) COMMENT '积分流水表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_order
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    orderNo         VARCHAR(64)                         NOT NULL COMMENT '业务订单号',
    userId          BIGINT                              NOT NULL COMMENT '用户id',
    orderType       VARCHAR(32)                         NOT NULL COMMENT '订单类型：subscription/points',
    productCode     VARCHAR(64)                         NOT NULL COMMENT '商品编码',
    productName     VARCHAR(128)                        NOT NULL COMMENT '商品名称快照',
    amount          DECIMAL(12, 4)                      NOT NULL COMMENT '支付金额（元）',
    paymentMethod   VARCHAR(32)                         NOT NULL COMMENT '支付方式：alipay/stripe/wechat',
    paymentId       VARCHAR(256)                        NULL COMMENT '支付平台订单ID',
    status          VARCHAR(32) DEFAULT 'pending'       NOT NULL COMMENT '状态：pending/success/failed/cancelled/refunded',
    extra           TEXT                                NULL COMMENT '订单扩展快照JSON',
    paidTime        DATETIME                            NULL COMMENT '支付成功时间',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    isDelete        TINYINT     DEFAULT 0               NOT NULL,
    UNIQUE KEY uk_orderNo (orderNo),
    INDEX idx_user_time (userId, createTime),
    INDEX idx_paymentId (paymentId),
    INDEX idx_status (status),
    INDEX idx_type_product (orderType, productCode)
) COMMENT '统一支付订单表' COLLATE = utf8mb4_unicode_ci;

INSERT INTO subscription_plan (planCode, planName, description, price, originalPrice, durationDays, lifetime, dailyProLimit, dailyAdvancedLimit, monthlyProLimit, monthlyAdvancedLimit, bonusPoints, apiKeyLimit, allowByok, priority, visible, status)
VALUES ('free', '免费版', '注册用户默认权益', 0, NULL, 0, 0, 20, 0, 300, 0, 0, 3, 0, 0, 0, 'active'),
       ('basic_day', '基础版日卡体验', '日卡体验套餐', 9.9000, NULL, 1, 0, 50, 10, -1, -1, 10, 3, 0, 10, 1, 'active'),
       ('supreme_month', '至尊版月卡', '个人主力套餐', 89.9000, NULL, 30, 0, 80, 50, -1, -1, 30, 5, 1, 30, 1, 'active'),
       ('flagship_year', '旗舰版年卡', '长期用户套餐', 199.0000, NULL, 365, 0, 100, 50, -1, -1, 50, 10, 1, 50, 1, 'active'),
       ('lifetime', '永久版', '永久身份，额度按周期刷新', 299.0000, NULL, 0, 1, -1, -1, 9999, 3000, 50, 20, 1, 100, 1, 'active')
ON DUPLICATE KEY UPDATE planName = VALUES(planName),
                        description = VALUES(description),
                        priority = VALUES(priority),
                        visible = VALUES(visible),
                        status = VALUES(status);

INSERT INTO point_package (packageCode, packageName, points, price, originalPrice, description, badge, priority, visible, status)
VALUES ('points_50', '50积分', 50, 18.0000, NULL, '图片生成积分包', NULL, 10, 1, 'active'),
       ('points_200', '200积分', 200, 58.0000, NULL, '图片生成积分包', NULL, 20, 1, 'active'),
       ('points_680', '680积分', 680, 180.0000, NULL, '图片生成积分包', NULL, 30, 1, 'active'),
       ('points_1200', '1200积分', 1200, 308.0000, NULL, '图片生成积分包', '热门', 40, 1, 'active'),
       ('points_3888', '3888积分', 3888, 888.0000, NULL, '图片生成积分包', '超值', 50, 1, 'active'),
       ('points_5888', '5888积分', 5888, 1314.0000, NULL, '图片生成积分包', NULL, 60, 1, 'active'),
       ('points_18888', '18888积分', 18888, 2800.0000, NULL, '图片生成积分包', '最划算', 70, 1, 'active'),
       ('points_58888', '58888积分', 58888, 8888.0000, NULL, '图片生成积分包', '企业级', 80, 1, 'active')
ON DUPLICATE KEY UPDATE packageName = VALUES(packageName),
                        priority = VALUES(priority),
                        visible = VALUES(visible),
                        status = VALUES(status);
