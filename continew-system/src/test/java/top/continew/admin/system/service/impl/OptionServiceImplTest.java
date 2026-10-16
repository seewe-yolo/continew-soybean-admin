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

package top.continew.admin.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.continew.admin.system.mapper.OptionMapper;
import top.continew.admin.system.model.entity.OptionDO;
import top.continew.admin.system.model.query.OptionQuery;
import top.continew.admin.system.model.req.OptionReq;
import top.continew.admin.system.model.req.OptionValueResetReq;
import top.continew.admin.test.TestApplicationContext;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;

import java.util.List;

/**
 * 小程序密钥配置安全测试
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
class OptionServiceImplTest {

    @BeforeAll
    static void setUpApplicationContext() {
        TestApplicationContext.initialize();
    }

    @Test
    void shouldNotReturnMiniProgramSecretOrClearItWhenFormIsBlank() {
        OptionMapper mapper = mock(OptionMapper.class);
        OptionServiceImpl service = new OptionServiceImpl(mapper);
        OptionDO miniProgramSecret =
            createSecretOption(1L, "MINI_PROGRAM_APP_SECRET", "mini-secret");
        OptionDO aliyunSecret =
            createSecretOption(2L, "ALIYUN_OCR_ACCESS_KEY_SECRET", "ocr-secret");
        OptionDO wechatPrivateKey =
            createSecretOption(3L, "WECHAT_PAY_PRIVATE_KEY", "private-key");
        OptionDO wechatApiV3Key =
            createSecretOption(4L, "WECHAT_PAY_API_V3_KEY", "api-v3-key");
        List<OptionDO> secrets =
            List.of(miniProgramSecret, aliyunSecret, wechatPrivateKey, wechatApiV3Key);
        when(mapper.selectList(any())).thenReturn(secrets);
        when(mapper.selectByIds(any())).thenReturn(secrets);

        service.list(new OptionQuery()).forEach(option -> assertNull(option.getValue()));

        OptionReq miniProgramReq = new OptionReq();
        miniProgramReq.setId(1L);
        miniProgramReq.setCode("MINI_PROGRAM_APP_SECRET");
        miniProgramReq.setValue(null);
        OptionReq aliyunReq = new OptionReq();
        aliyunReq.setId(2L);
        aliyunReq.setCode("ALIYUN_OCR_ACCESS_KEY_SECRET");
        aliyunReq.setValue(null);
        OptionReq wechatPrivateKeyReq = new OptionReq();
        wechatPrivateKeyReq.setId(3L);
        wechatPrivateKeyReq.setCode("WECHAT_PAY_PRIVATE_KEY");
        wechatPrivateKeyReq.setValue(null);
        OptionReq wechatApiV3KeyReq = new OptionReq();
        wechatApiV3KeyReq.setId(4L);
        wechatApiV3KeyReq.setCode("WECHAT_PAY_API_V3_KEY");
        wechatApiV3KeyReq.setValue(null);
        service.update(List.of(miniProgramReq, aliyunReq, wechatPrivateKeyReq, wechatApiV3KeyReq));

        ArgumentCaptor<List<OptionDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("mini-secret", captor.getValue().get(0).getValue());
        assertEquals("ocr-secret", captor.getValue().get(1).getValue());
        assertEquals("private-key", captor.getValue().get(2).getValue());
        assertEquals("api-v3-key", captor.getValue().get(3).getValue());
    }

    private OptionDO createSecretOption(Long id, String code, String secret) {
        OptionDO option = new OptionDO();
        option.setId(id);
        option.setCode(code);
        option.setValue(secret);
        return option;
    }

    @Test
    void shouldRequireThirdPartyPermissionForReadUpdateAndReset() {
        OptionMapper mapper = mock(OptionMapper.class);
        OptionServiceImpl service = new OptionServiceImpl(mapper);
        OptionDO secret = createSecretOption(1L, "WECHAT_PAY_PRIVATE_KEY", "private-key");
        secret.setCategory("THIRD_PARTY");
        when(mapper.selectList(any())).thenReturn(List.of(secret));
        when(mapper.selectByIds(any())).thenReturn(List.of(secret));
        OptionReq update = new OptionReq();
        update.setId(1L);
        update.setCode(secret.getCode());
        OptionValueResetReq reset = new OptionValueResetReq();
        reset.setCode(List.of(secret.getCode()));
        try (var permissions = mockStatic(StpUtil.class)) {
            permissions.when(() -> StpUtil.checkPermission("system:thirdPartyConfig:get"))
                .thenThrow(new BusinessException("forbidden"));
            permissions.when(() -> StpUtil.checkPermission("system:thirdPartyConfig:update"))
                .thenThrow(new BusinessException("forbidden"));
            assertThrows(BusinessException.class, () -> service.list(new OptionQuery()));
            assertThrows(BusinessException.class, () -> service.update(List.of(update)));
            assertThrows(BusinessException.class, () -> service.resetValue(reset));
            verify(mapper, never()).updateById(any(List.class));
            verify(mapper, never()).lambdaUpdate();
        }
    }

    @Test
    void shouldRejectMismatchedIdAndCode() {
        OptionMapper mapper = mock(OptionMapper.class);
        OptionServiceImpl service = new OptionServiceImpl(mapper);
        OptionDO secret = createSecretOption(1L, "WECHAT_PAY_PRIVATE_KEY", "private-key");
        when(mapper.selectByIds(any())).thenReturn(List.of(secret));
        OptionReq update = new OptionReq();
        update.setId(2L);
        update.setCode(secret.getCode());
        assertThrows(BadRequestException.class, () -> service.update(List.of(update)));
        verify(mapper, never()).updateById(any(List.class));
    }
}
