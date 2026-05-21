package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceImplTest {

    @Test
    void getLoginUserDoesNotCreateSpringSessionWhenJwtContextIsAbsent() {
        UserServiceImpl service = new UserServiceImpl();
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(BusinessException.class, () -> service.getLoginUser(request));

        assertNull(request.getSession(false));
    }
}
