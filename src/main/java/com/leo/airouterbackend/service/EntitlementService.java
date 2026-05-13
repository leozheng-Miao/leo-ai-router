package com.leo.airouterbackend.service;

import com.leo.airouterbackend.model.entity.Model;

public interface EntitlementService {

    void checkChatAccess(Long userId, Model model);

    boolean canUseChatAccess(Long userId, Model model);

    void recordChatUsage(Long userId, Model model);

    void checkImagePoints(Long userId, Model model, int count);

    void checkImageAccess(Long userId, Model model);

    void deductImagePoints(Long userId, Model model, int count, Long refId);

    void checkApiKeyCreate(Long userId, long currentApiKeyCount);

    void checkByokAllowed(Long userId);

    long getTodayUsed(Long userId, String tier);
}
