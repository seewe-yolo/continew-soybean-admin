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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.continew.admin.system.mapper.OptionMapper;
import top.continew.admin.system.model.entity.OptionDO;
import top.continew.admin.system.model.query.OptionQuery;
import top.continew.admin.system.model.req.OptionReq;
import top.continew.admin.test.TestApplicationContext;

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
        OptionDO option = new OptionDO();
        option.setId(1L);
        option.setCode("MINI_PROGRAM_APP_SECRET");
        option.setValue("stored-secret");
        when(mapper.selectList(any())).thenReturn(List.of(option));
        when(mapper.selectByIds(any())).thenReturn(List.of(option));

        assertNull(service.list(new OptionQuery()).get(0).getValue());

        OptionReq req = new OptionReq();
        req.setId(1L);
        req.setCode("MINI_PROGRAM_APP_SECRET");
        req.setValue(null);
        service.update(List.of(req));

        ArgumentCaptor<List<OptionDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).updateById(captor.capture());
        assertEquals("stored-secret", captor.getValue().get(0).getValue());
    }
}
