package com.leo.airouterbackend.service.impl;

import com.leo.airouterbackend.mapper.rbac.RolePermissionMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacServiceImplTest {

    @Test
    void setRolePermissionsInvalidatesPermissionCacheByVersionBump() {
        RolePermissionMapper rolePermissionMapper = mock(RolePermissionMapper.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RAtomicLong version = mock(RAtomicLong.class);
        when(redissonClient.getAtomicLong("rbac:permissions:version")).thenReturn(version);

        RbacServiceImpl service = new RbacServiceImpl();
        ReflectionTestUtils.setField(service, "rolePermissionMapper", rolePermissionMapper);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);

        service.setRolePermissions("user", List.of());

        verify(rolePermissionMapper).deleteByQuery(any());
        verify(version).incrementAndGet();
    }
}
