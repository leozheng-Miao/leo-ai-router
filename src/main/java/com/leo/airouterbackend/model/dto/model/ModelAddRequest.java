package com.leo.airouterbackend.model.dto.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模型创建请求
 *
 */
@Data
public class ModelAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供者id
     */
    private Long providerId;

    /**
     * 模型标识（如：qwen-plus）
     */
    private String modelKey;

    /**
     * 模型显示名称
     */
    private String modelName;

    /**
     * 模型类型：chat/embedding/image/audio
     */
    private String modelType;

    /**
     * 访问等级：free/pro/advanced/image/video
     */
    private String accessTier;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 上下文长度限制
     */
    private Integer contextLength;

    /**
     * 输入价格（元/千Token）
     */
    private BigDecimal inputPrice;

    /**
     * 输出价格（元/千Token）
     */
    private BigDecimal outputPrice;

    /**
     * 积分成本：图片/视频单次消耗积分
     */
    private Integer pointCost;

    /**
     * 状态：active/inactive/deprecated
     */
    private String status;

    /**
     * 优先级（越大越优先）
     */
    private Integer priority;

    /**
     * 默认超时时间（毫秒）
     */
    private Integer defaultTimeout;

    /**
     * 能力标签（JSON数组）
     */
    private String capabilities;

    /**
     * 是否支持深度思考：0=不支持，1=支持
     */
    private Integer supportReasoning;
}
