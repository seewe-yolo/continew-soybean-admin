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

package top.continew.admin.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.context.support.GenericApplicationContext;

import jakarta.validation.Validator;

/**
 * Minimal Spring beans required by static project utilities in unit tests
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public final class TestApplicationContext {

    private static boolean initialized;

    private TestApplicationContext() {
    }

    /**
     * Initialize static utility dependencies for unit tests.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("validator", Validator.class, () -> mock(Validator.class));
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getKeys()).thenReturn(mock(RKeys.class));
        context.getBeanFactory().registerSingleton("redissonClient", redissonClient);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
        initialized = true;
    }
}
