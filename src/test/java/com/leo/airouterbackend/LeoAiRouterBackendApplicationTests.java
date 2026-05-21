package com.leo.airouterbackend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LeoAiRouterBackendApplicationTests {

    @Test
    void applicationClassLoads() {
        assertDoesNotThrow(() -> Class.forName(LeoAiRouterBackendApplication.class.getName()));
    }

}
