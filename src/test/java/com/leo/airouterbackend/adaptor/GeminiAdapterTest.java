package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.model.dto.chat.ChatMessage;
import com.leo.airouterbackend.model.dto.chat.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAdapterTest {

    private final GeminiAdapter adapter = new GeminiAdapter();

    @Test
    void shouldSupportGeminiProviders() {
        assertTrue(adapter.supports("gemini"));
        assertTrue(adapter.supports("google"));
        assertFalse(adapter.supports("openai"));
    }

    @Test
    void shouldBuildPromptForGemini31ProReasoningEnabled() {
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatMessage("user", "hello")));
        request.setTemperature(0.4);
        request.setMaxTokens(512);
        request.setEnableReasoning(true);

        Prompt prompt = adapter.buildPrompt(request, "gemini-3.1-pro-preview");
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) prompt.getOptions();

        assertEquals("gemini-3.1-pro-preview", options.getModel());
        assertEquals(0.4, options.getTemperature());
        assertEquals(512, options.getMaxOutputTokens());
        assertEquals(GoogleGenAiThinkingLevel.HIGH, options.getThinkingLevel());
        assertNull(options.getThinkingBudget());
    }

    @Test
    void shouldBuildPromptForGemini3FlashReasoningDisabled() {
        ChatRequest request = new ChatRequest();
        request.setMessages(List.of(new ChatMessage("user", "hello")));
        request.setEnableReasoning(false);

        Prompt prompt = adapter.buildPrompt(request, "gemini-3-flash-preview");
        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) prompt.getOptions();

        assertNull(options.getThinkingLevel());
        assertEquals(0, options.getThinkingBudget());
    }

    @Test
    void shouldBuildPromptForGemini31FlashLiteReasoningToggle() {
        ChatRequest enabledRequest = new ChatRequest();
        enabledRequest.setMessages(List.of(new ChatMessage("user", "hello")));
        enabledRequest.setEnableReasoning(true);
        GoogleGenAiChatOptions enabledOptions =
                (GoogleGenAiChatOptions) adapter.buildPrompt(enabledRequest, "gemini-3.1-flash-lite").getOptions();

        ChatRequest disabledRequest = new ChatRequest();
        disabledRequest.setMessages(List.of(new ChatMessage("user", "hello")));
        disabledRequest.setEnableReasoning(false);
        GoogleGenAiChatOptions disabledOptions =
                (GoogleGenAiChatOptions) adapter.buildPrompt(disabledRequest, "gemini-3.1-flash-lite").getOptions();

        assertEquals(-1, enabledOptions.getThinkingBudget());
        assertEquals(0, disabledOptions.getThinkingBudget());
        assertNull(enabledOptions.getThinkingLevel());
    }
}
