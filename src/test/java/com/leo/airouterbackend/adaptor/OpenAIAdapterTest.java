package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.model.dto.chat.ChatMessage;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIAdapterTest {

    private final OpenAIAdapter adapter = new OpenAIAdapter();

    @Test
    void shouldSupportOpenAiProviders() {
        assertTrue(adapter.supports("openai"));
        assertTrue(adapter.supports("gpt"));
        assertFalse(adapter.supports("deepseek"));
    }

    @Test
    void shouldBuildPromptWithReasoningEnabled() {
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatMessage("user", "hello")));
        request.setTemperature(0.3);
        request.setMaxTokens(256);
        request.setEnableReasoning(true);

        Prompt prompt = adapter.buildPrompt(request, "gpt-5.5");
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getOptions();

        assertEquals("gpt-5.5", options.getModel());
        assertEquals(0.3, options.getTemperature());
        assertEquals(256, options.getMaxTokens());
        assertTrue(options.getStreamUsage());
        assertEquals("medium", options.getReasoningEffort());
    }

    @Test
    void shouldBuildPromptWithReasoningDisabled() {
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatMessage("user", "hello")));
        request.setEnableReasoning(false);

        Prompt prompt = adapter.buildPrompt(request, "gpt-5.4-mini");
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getOptions();

        assertEquals("none", options.getReasoningEffort());
    }
}
