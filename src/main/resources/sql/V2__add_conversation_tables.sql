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
