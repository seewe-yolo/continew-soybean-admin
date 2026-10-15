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

package top.continew.admin.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.continew.admin.test.TestApplicationContext;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 小程序手机号接口调用测试
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
class MiniProgramApiClientTest {

    @BeforeAll
    static void setUpApplicationContext() {
        TestApplicationContext.initialize();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void shouldReuseAccessTokenWhenExchangingPhoneNumber() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        HttpResponse<String> phoneResponse = mock(HttpResponse.class);
        doReturn(200).when(tokenResponse).statusCode();
        doReturn("{\"access_token\":\"server-token\",\"expires_in\":7200}")
            .when(tokenResponse).body();
        doReturn(200).when(phoneResponse).statusCode();
        doReturn("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"13800138000\"}}")
            .when(phoneResponse).body();
        doReturn(tokenResponse, phoneResponse, phoneResponse)
            .when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        MiniProgramApiClient client = new MiniProgramApiClient(httpClient);

        assertEquals("13800138000", client.getPhoneNumber("one-time-code", "wx-app", "secret"));
        assertEquals("13800138000", client.getPhoneNumber("next-code", "wx-app", "secret"));
        verify(httpClient, times(3)).send(any(HttpRequest.class),
            any(HttpResponse.BodyHandler.class));
    }
}
