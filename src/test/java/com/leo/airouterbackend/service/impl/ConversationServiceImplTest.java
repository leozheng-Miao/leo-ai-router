package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.model.entity.Conversation;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class ConversationServiceImplTest {

    @Test
    void deleteConversationRemovesSequenceKey() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ConversationServiceImpl service = spy(new ConversationServiceImpl());
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        Conversation conversation = new Conversation();
        conversation.setId(99L);
        conversation.setUserId(10L);
        doReturn(conversation).when(service).validateOwner(10L, 99L);
        doReturn(true).when(service).updateById(conversation);

        service.deleteConversation(10L, 99L);

        verify(redisTemplate).delete("conv:seq:99");
    }
}
