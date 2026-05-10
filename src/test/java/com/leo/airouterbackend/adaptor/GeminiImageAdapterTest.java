package com.leo.airouterbackend.adaptor;

import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.leo.airouterbackend.exception.BusinessException;
import com.leo.airouterbackend.model.dto.image.ImageGenerationRequest;
import com.leo.airouterbackend.model.dto.image.ImageGenerationResponse;
import com.leo.airouterbackend.model.entity.Model;
import com.leo.airouterbackend.model.entity.ModelProvider;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiImageAdapterTest {

    private final GeminiImageAdapter adapter = new GeminiImageAdapter();

    @Test
    void shouldConvertInlineDataToBase64Response() {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        GenerateContentResponse response = GenerateContentResponse.builder()
                .candidates(Candidate.builder()
                        .content(Content.fromParts(
                                Part.builder().inlineData(Blob.builder()
                                        .mimeType("image/png")
                                        .data(imageBytes)
                                        .build()).build(),
                                Part.fromText("ignored text")
                        ))
                        .build())
                .build();

        ImageGenerationResponse result = adapter.convertResponse(response);

        assertEquals(1, result.getData().size());
        assertEquals(Base64.getEncoder().encodeToString(imageBytes), result.getData().get(0).getB64Json());
        assertEquals(null, result.getData().get(0).getRevisedPrompt());
    }

    @Test
    void shouldRejectMultipleImagesRequest() {
        Model model = Model.builder().modelKey("gemini-3.1-flash-image-preview").build();
        ModelProvider provider = ModelProvider.builder()
                .providerName("gemini")
                .baseUrl("https://generativelanguage.googleapis.com")
                .apiKey("test")
                .build();
        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .prompt("draw a cat")
                .build();

        assertThrows(BusinessException.class, () -> adapter.generate(model, provider, request, 2));
    }
}
