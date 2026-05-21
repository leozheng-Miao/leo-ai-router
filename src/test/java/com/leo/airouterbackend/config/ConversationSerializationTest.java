package com.leo.airouterbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.model.vo.ConversationVO;
import com.mybatisflex.core.paginate.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationSerializationTest {

    private ObjectMapper objectMapper;
    private RedisSerializer<Object> redisSerializer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Test
    void shouldSerializeConversationPageToHttpJson() {
        BaseResponse<Page<ConversationVO>> response = new BaseResponse<>(0, buildConversationPage(), "ok");

        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(response));

        assertTrue(json.contains("2026-05-09T22:00:00"));
    }

    @Test
    void shouldSerializeConversationPageToRedisJson() {
        byte[] bytes = assertDoesNotThrow(() -> redisSerializer.serialize(buildConversationPage()));

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    private Page<ConversationVO> buildConversationPage() {
        ConversationVO conversation = new ConversationVO();
        conversation.setId(1L);
        conversation.setTitle("测试会话");
        conversation.setConvType(1);
        conversation.setLastMessageAt(LocalDateTime.of(2026, 5, 9, 22, 0, 0));
        conversation.setLastMessagePreview("最近一条回复");

        Page<ConversationVO> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(conversation));
        return page;
    }
}
