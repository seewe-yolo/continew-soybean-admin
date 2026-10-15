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

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import top.continew.starter.core.util.validation.ValidationUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * 微信小程序手机号授权 API 客户端
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Component
public class MiniProgramApiClient {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL =
        "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long TOKEN_REFRESH_MARGIN_SECONDS = 60;

    private final HttpClient httpClient;
    private volatile AccessToken accessToken;

    public MiniProgramApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    MiniProgramApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 通过微信手机号授权码获取手机号
     *
     * @param code      wx.getPhoneNumber 返回的一次性授权码
     * @param appId     小程序 AppID
     * @param appSecret 小程序 AppSecret
     * @return 手机号
     */
    public String getPhoneNumber(String code, String appId, String appSecret) {
        String token = this.getAccessToken(appId, appSecret);
        URI uri = URI.create(PHONE_NUMBER_URL + "?access_token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers
                .ofString(JSONUtil.createObj().set("code", code).toString()))
            .build();
        JSONObject response = this.send(request);
        ValidationUtils.throwIf(!Integer.valueOf(0).equals(response.getInt("errcode")),
            "微信小程序手机号授权失败，请稍后重试");
        JSONObject phoneInfo = response.getJSONObject("phone_info");
        String phone = phoneInfo == null ? null : phoneInfo.getStr("purePhoneNumber");
        ValidationUtils.throwIf(StrUtil.isBlank(phone), "微信小程序未返回有效手机号");
        return phone;
    }

    private String getAccessToken(String appId, String appSecret) {
        AccessToken current = accessToken;
        if (current != null && current.isValid(appId, appSecret)) {
            return current.value();
        }
        synchronized (this) {
            current = accessToken;
            if (current != null && current.isValid(appId, appSecret)) {
                return current.value();
            }
            String url = ACCESS_TOKEN_URL + "?grant_type=client_credential&appid="
                + URLEncoder.encode(appId, StandardCharsets.UTF_8) + "&secret="
                + URLEncoder.encode(appSecret, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
            JSONObject response = this.send(request);
            String value = response.getStr("access_token");
            Integer expiresIn = response.getInt("expires_in");
            ValidationUtils.throwIf(StrUtil.isBlank(value) || expiresIn == null || expiresIn <= 0,
                "微信小程序登录配置无效，请检查 AppID 和密钥");
            Instant expiresAt = Instant.now()
                .plusSeconds(Math.max(expiresIn - TOKEN_REFRESH_MARGIN_SECONDS, 0));
            accessToken = new AccessToken(appId, appSecret, value, expiresAt);
            return value;
        }
    }

    private JSONObject send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("微信小程序接口调用失败，请稍后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("微信小程序接口调用失败，请稍后重试");
        }
        ValidationUtils.throwIf(response.statusCode() < 200 || response.statusCode() >= 300,
            "微信小程序接口调用失败，请稍后重试");
        JSONObject body;
        try {
            body = JSONUtil.parseObj(response.body());
        } catch (RuntimeException e) {
            throw new IllegalStateException("微信小程序接口响应无效，请稍后重试");
        }
        ValidationUtils.throwIfNull(body, "微信小程序接口响应无效，请稍后重试");
        return body;
    }

    private record AccessToken(String appId, String appSecret, String value, Instant expiresAt) {

        private boolean isValid(String expectedAppId, String expectedAppSecret) {
            return appId.equals(expectedAppId) && appSecret.equals(expectedAppSecret)
                && Instant.now().isBefore(expiresAt);
        }
    }
}
