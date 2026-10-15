/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.admin.auth.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.continew.admin.auth.client.MiniProgramApiClient;
import top.continew.admin.auth.model.req.MiniProgramLoginReq;
import top.continew.admin.auth.model.resp.LoginResp;
import top.continew.admin.auth.service.AuthTokenService;
import top.continew.admin.common.enums.DisEnableStatusEnum;
import top.continew.admin.common.enums.RoleCodeEnum;
import top.continew.admin.system.enums.OptionCategoryEnum;
import top.continew.admin.system.model.entity.DeptDO;
import top.continew.admin.system.model.entity.user.UserDO;
import top.continew.admin.system.model.resp.ClientResp;
import top.continew.admin.system.service.DeptService;
import top.continew.admin.system.service.OptionService;
import top.continew.admin.system.service.RoleService;
import top.continew.admin.system.service.UserRoleService;
import top.continew.admin.system.service.UserService;
import top.continew.admin.test.TestApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 小程序登录处理器测试
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
class MiniProgramLoginHandlerTest {

    private MiniProgramApiClient miniProgramApiClient;
    private DeptService deptService;
    private UserRoleService userRoleService;
    private OptionService optionService;
    private RoleService roleService;
    private UserService userService;
    private AuthTokenService authTokenService;
    private MiniProgramLoginHandler handler;

    @BeforeAll
    static void setUpApplicationContext() {
        TestApplicationContext.initialize();
    }

    @BeforeEach
    void setUp() {
        miniProgramApiClient = mock(MiniProgramApiClient.class);
        deptService = mock(DeptService.class);
        userRoleService = mock(UserRoleService.class);
        optionService = mock(OptionService.class);
        roleService = mock(RoleService.class);
        userService = mock(UserService.class);
        authTokenService = mock(AuthTokenService.class);
        handler = new MiniProgramLoginHandler(miniProgramApiClient, deptService, userRoleService);
        ReflectionTestUtils.setField(handler, "optionService", optionService);
        ReflectionTestUtils.setField(handler, "roleService", roleService);
        ReflectionTestUtils.setField(handler, "userService", userService);
        ReflectionTestUtils.setField(handler, "authTokenService", authTokenService);
    }

    @Test
    void shouldCreateEnabledUserUnderExternalDepartmentWithMiniProgramRole() {
        when(optionService.getByCategory(OptionCategoryEnum.LOGIN))
            .thenReturn(
                Map.of("MINI_PROGRAM_APP_ID", "wx-app", "MINI_PROGRAM_APP_SECRET", "secret"));
        when(miniProgramApiClient.getPhoneNumber("one-time-code", "wx-app", "secret"))
            .thenReturn("13800138000");
        when(userService.getByPhone("13800138000")).thenReturn(null);
        DeptDO rootDept = new DeptDO();
        rootDept.setId(1L);
        DeptDO externalDept = new DeptDO();
        externalDept.setId(2L);
        externalDept.setStatus(DisEnableStatusEnum.ENABLE);
        when(deptService.getOne(any())).thenReturn(rootDept, externalDept);
        when(roleService.getIdByCode(RoleCodeEnum.MINI_PROGRAM_USER.getCode())).thenReturn(3L);
        when(userService.save(any(UserDO.class))).thenAnswer(invocation -> {
            UserDO user = invocation.getArgument(0);
            user.setId(4L);
            return true;
        });
        LoginResp expected = LoginResp.builder().accessToken("access-token").build();
        when(authTokenService.issue(any(UserDO.class), any(ClientResp.class), any(),
            any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(expected);
        MiniProgramLoginReq req = new MiniProgramLoginReq();
        req.setCode("one-time-code");

        LoginResp actual = handler.login(req, new ClientResp(), mock(HttpServletRequest.class),
            mock(HttpServletResponse.class));

        assertEquals(expected, actual);
        verify(userService).save(org.mockito.ArgumentMatchers
            .argThat(user -> "13800138000".equals(user.getPhone()) && user.getDeptId().equals(2L)
                && user.getStatus() == DisEnableStatusEnum.ENABLE));
        verify(userRoleService).assignRolesToUser(List.of(3L), 4L);
    }
}
