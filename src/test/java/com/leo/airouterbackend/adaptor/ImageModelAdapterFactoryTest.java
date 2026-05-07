package com.leo.airouterbackend.adaptor;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ImageModelAdapterFactoryTest {

    @Test
    void shouldReturnOpenAiAdapterForOpenAiProvider() {
        ImageModelAdapterFactory factory = new ImageModelAdapterFactory();
        ReflectionTestUtils.setField(factory, "adapters", List.of(
                new OpenAiImageAdapter(),
                new DashscopeImageAdapter()
        ));

        ImageModelAdapter adapter = factory.getAdapter("openai");
        assertInstanceOf(OpenAiImageAdapter.class, adapter);
    }

    @Test
    void shouldReturnDashscopeAdapterForQwenProvider() {
        ImageModelAdapterFactory factory = new ImageModelAdapterFactory();
        ReflectionTestUtils.setField(factory, "adapters", List.of(
                new OpenAiImageAdapter(),
                new DashscopeImageAdapter()
        ));

        ImageModelAdapter adapter = factory.getAdapter("qwen");
        assertInstanceOf(DashscopeImageAdapter.class, adapter);
    }
}
