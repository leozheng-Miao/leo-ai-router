package com.leo.airouter.sdk;

import com.leo.airouter.sdk.model.ChatMessage;
import com.leo.airouter.sdk.model.ChatRequest;
import com.leo.airouter.sdk.model.ChatResponse;

import java.util.Arrays;

/**
 * 高级功能示例
 */
public class AdvancedExample {

    public static void main(String[] args) {
        LeoAIClient client = LeoAIClient.builder()
                .apiKey("sk-5b1cfcb279a5496ea5e699591ed755f9")
                .baseUrl("http://localhost:8123/api")
                .connectTimeout(15000)
                .readTimeout(60000)
                .maxRetries(5)
                .build();

        try {
            System.out.println("=== 多轮对话示例 ===\n");

            ChatRequest request = ChatRequest.builder()
                    .messages(Arrays.asList(
                            ChatMessage.system("你是一个编程助手"),
                            ChatMessage.user("什么是 Java？"),
                            ChatMessage.assistant("Java 是一种面向对象的编程语言..."),
                            ChatMessage.user("它的主要特点是什么？")
                    ))
                    .model("deepseek-chat")
                    .temperature(0.7)
                    .build();

            ChatResponse response = client.chat(request);
            System.out.println("回答: " + response.getContent());

        } finally {
            client.close();
        }
    }
}