package com.leo.airouterbackend.controller;

import com.leo.airouterbackend.auth.UserContext;
import com.leo.airouterbackend.model.entity.User;
import com.leo.airouterbackend.service.ConversationService;
import com.leo.airouterbackend.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationControllerTest {

    private static final long LOGIN_USER_ID = 411701201353248768L;
    private static final long SPOOFED_HEADER_USER_ID = 123L;

    private ConversationService conversationService;
    private MessageService messageService;
    private MockMvc mockMvc;
    private AtomicReference<Long> createConversationUserId;
    private AtomicReference<Long> streamMessageUserId;

    @BeforeEach
    void setUp() {
        createConversationUserId = new AtomicReference<>();
        streamMessageUserId = new AtomicReference<>();
        conversationService = conversationServiceProxy();
        messageService = messageServiceProxy();

        ConversationController controller = new ConversationController();
        ReflectionTestUtils.setField(controller, "conversationService", conversationService);
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        User loginUser = new User();
        loginUser.setId(LOGIN_USER_ID);
        UserContext.set(loginUser, null);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createConversationUsesAuthenticatedUserIdInsteadOfClientHeader() throws Exception {
        mockMvc.perform(post("/conversations")
                        .header("X-User-Id", SPOOFED_HEADER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"convType\":1}"))
                .andExpect(status().isOk());

        assertEquals(LOGIN_USER_ID, createConversationUserId.get());
    }

    @Test
    void streamMessageUsesAuthenticatedUserIdInsteadOfClientHeader() throws Exception {
        mockMvc.perform(post("/conversations/24/messages/stream")
                        .header("X-User-Id", SPOOFED_HEADER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\",\"mode\":1}"))
                .andExpect(status().isOk());

        assertEquals(LOGIN_USER_ID, streamMessageUserId.get());
    }

    private ConversationService conversationServiceProxy() {
        return (ConversationService) Proxy.newProxyInstance(
                ConversationService.class.getClassLoader(),
                new Class[]{ConversationService.class},
                (proxy, method, args) -> {
                    if ("createConversation".equals(method.getName())) {
                        createConversationUserId.set((Long) args[0]);
                        return 24L;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private MessageService messageServiceProxy() {
        return (MessageService) Proxy.newProxyInstance(
                MessageService.class.getClassLoader(),
                new Class[]{MessageService.class},
                (proxy, method, args) -> {
                    if ("streamMessage".equals(method.getName())) {
                        streamMessageUserId.set((Long) args[0]);
                        return Flux.just("[DONE]");
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE || returnType == Long.TYPE || returnType == Short.TYPE || returnType == Byte.TYPE) {
            return 0;
        }
        if (returnType == Float.TYPE || returnType == Double.TYPE) {
            return 0D;
        }
        return null;
    }
}
