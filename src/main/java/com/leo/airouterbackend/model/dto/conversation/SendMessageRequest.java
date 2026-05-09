package com.leo.airouterbackend.model.dto.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SendMessageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String content;

    private Integer mode;

    private String model;

    @JsonProperty("routing_strategy")
    private String routingStrategy;
}
