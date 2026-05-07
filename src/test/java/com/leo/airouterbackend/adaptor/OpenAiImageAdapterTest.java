package com.leo.airouterbackend.adaptor;

import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiImageApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAiImageAdapterTest {

    private final OpenAiImageAdapter adapter = new OpenAiImageAdapter();

    @Test
    void shouldBuildRequestWithNormalizedFields() {
        Model model = Model.builder().modelKey("gpt-image-2").build();
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .prompt("draw a cat")
                .size("1024*1024")
                .quality("standard")
                .user("u-1")
                .build();

        OpenAiImageApi.OpenAiImageRequest imageRequest = adapter.buildRequest(model, request, 1);

        assertEquals("draw a cat", imageRequest.prompt());
        assertEquals("gpt-image-2", imageRequest.model());
        assertEquals("1024x1024", imageRequest.size());
        assertEquals("medium", imageRequest.quality());
        assertEquals("u-1", imageRequest.user());
        assertNull(imageRequest.responseFormat());
    }

    @Test
    void shouldConvertResponseToUnifiedFormat() {
        OpenAiImageApi.OpenAiImageResponse response = new OpenAiImageApi.OpenAiImageResponse(
                123L,
                List.of(new OpenAiImageApi.Data(null, "base64-data", "revised prompt"))
        );

        ImageGenerationResponse result = adapter.convertResponse(response);

        assertEquals(123L, result.getCreated());
        assertEquals(1, result.getData().size());
        assertNull(result.getData().get(0).getUrl());
        assertEquals("base64-data", result.getData().get(0).getB64Json());
        assertEquals("revised prompt", result.getData().get(0).getRevisedPrompt());
    }

    @Test
    void shouldNormalizeLegacyHdQuality() {
        assertEquals("high", adapter.normalizeQuality("hd"));
        assertEquals("medium", adapter.normalizeQuality("standard"));
        assertEquals("1024x1024", adapter.normalizeSize("1024*1024"));
    }
}
