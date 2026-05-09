package com.leo.airouterbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leo.airouterbackend.common.BaseResponse;
import com.leo.airouterbackend.model.vo.ConversationVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConversationSerializationTest {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void shouldSerializeConversationPageToHttpJson() {
        BaseResponse<Page<ConversationVO>> response = new BaseResponse<>(0, buildConversationPage(), "ok");

        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(response));

        assertTrue(json.contains("2026-05-09T22:00:00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSerializeConversationPageToRedisJson() {
        RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        byte[] bytes = assertDoesNotThrow(() -> valueSerializer.serialize(buildConversationPage()));

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
