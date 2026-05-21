package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.mapper.ConversationMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceImplTest {

    @Test
    void nextSeqRefreshesSequenceKeyTtl() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ConversationMessageMapper messageMapper = mock(ConversationMessageMapper.class);
        when(redisTemplate.hasKey("conv:seq:99")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(messageMapper.selectMaxSeq(99L)).thenReturn(7L);
        when(valueOperations.increment("conv:seq:99")).thenReturn(8L);

        MessageServiceImpl service = new MessageServiceImpl();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "conversationMessageMapper", messageMapper);

        Method method = MessageServiceImpl.class.getDeclaredMethod("nextSeq", Long.class);
        method.setAccessible(true);
        Long seq = (Long) method.invoke(service, 99L);

        assertEquals(8L, seq);
        verify(redisTemplate).expire(eq("conv:seq:99"), eq(180L), eq(TimeUnit.DAYS));
    }
}
