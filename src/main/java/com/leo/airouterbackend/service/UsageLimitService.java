package com.leo.airouterbackend.service;

public interface UsageLimitService {

    void checkAndRecordChat(Long userId);

    void checkAndRecordImage(Long userId);

    void checkAndRecordPlugin(Long userId);

    void checkApiKeyCreate(Long userId, long currentApiKeyCount);

    void checkByokAllowed(Long userId);
}
