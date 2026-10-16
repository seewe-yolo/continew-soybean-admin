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

package top.continew.admin.wechatpay.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.admin.common.api.wechatpay.WechatPayOrderRequest;
import top.continew.starter.core.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 微信支付 API 参数校验测试。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
class WechatPayApiImplTest {

    private OptionApi optionApi;
    private Map<String, String> options;
    private WechatPayApiImpl wechatPayApi;

    @BeforeEach
    void setUp() {
        optionApi = mock(OptionApi.class);
        options = new HashMap<>();
        options.put("WECHAT_PAY_ENABLED", "1");
        options.put("WECHAT_PAY_APP_ID", "app-id");
        options.put("WECHAT_PAY_MCH_ID", "merchant-id");
        options.put("WECHAT_PAY_MCH_SERIAL_NO", "123456");
        options.put("WECHAT_PAY_PRIVATE_KEY", "private-key");
        options.put("WECHAT_PAY_API_V3_KEY", "0123456789abcdef0123456789abcdef");
        options.put("WECHAT_PAY_NOTIFY_URL", "https://pay.example.com/notify");
        options.put("WECHAT_PAY_PUBLIC_KEY_ID", "PUB_KEY_ID_TEST");
        options.put("WECHAT_PAY_PUBLIC_KEY", "public-key");
        when(optionApi.getByCategory("THIRD_PARTY")).thenReturn(options);
        wechatPayApi = new WechatPayApiImpl(optionApi);
    }

    @Test
    void shouldRejectOrderWhenPaymentIsDisabled() {
        options.put("WECHAT_PAY_ENABLED", "0");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> wechatPayApi.createJsapiOrder(validRequest()));

        assertEquals("微信支付未启用", exception.getMessage());
        verify(optionApi).getByCategory("THIRD_PARTY");
    }

    @Test
    void shouldRejectOrderWithInvalidAmountBeforeLoadingOptions() {
        WechatPayOrderRequest request = new WechatPayOrderRequest("app-id", "description",
            "order-123", 0, "openid", null, null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> wechatPayApi.createJsapiOrder(request));

        assertEquals("微信支付订单参数不完整", exception.getMessage());
    }

    @Test
    void shouldRejectNotificationWithoutSignatureHeaders() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> wechatPayApi.parseNotification("{}", "timestamp", "nonce", "", "serial"));

        assertEquals("微信支付通知参数不完整", exception.getMessage());
    }

    @Test
    void shouldMapJsapiOrderAndReturnPaymentParameters() throws Exception {
        WxPayService service = mock(WxPayService.class);
        wechatPayApi = new WechatPayApiImpl(optionApi, () -> service);
        WxPayUnifiedOrderV3Result.JsapiResult result = new WxPayUnifiedOrderV3Result.JsapiResult();
        result.setAppId("app-id");
        result.setPackageValue("prepay_id=prepay-id");
        result.setPaySign("signature");
        when(service.createOrderV3(eq(TradeTypeEnum.JSAPI), any())).thenReturn(result);

        var response = wechatPayApi.createJsapiOrder(validRequest());

        ArgumentCaptor<WxPayUnifiedOrderV3Request> captor =
            ArgumentCaptor.forClass(WxPayUnifiedOrderV3Request.class);
        verify(service).createOrderV3(eq(TradeTypeEnum.JSAPI), captor.capture());
        assertEquals("merchant-id", captor.getValue().getMchid());
        assertEquals("https://pay.example.com/notify", captor.getValue().getNotifyUrl());
        assertEquals(100, captor.getValue().getAmount().getTotal());
        assertEquals("CNY", captor.getValue().getAmount().getCurrency());
        assertEquals("openid", captor.getValue().getPayer().getOpenid());
        assertEquals("signature", response.paySign());
        assertEquals("prepay_id=prepay-id", response.packageValue());
        ArgumentCaptor<WxPayConfig> configCaptor = ArgumentCaptor.forClass(WxPayConfig.class);
        verify(service).setConfig(configCaptor.capture());
        assertEquals("PUB_KEY_ID_TEST", configCaptor.getValue().getPublicKeyId());
        assertEquals("public-key", new String(configCaptor.getValue().getPublicKeyContent(),
            StandardCharsets.UTF_8));
    }

    @Test
    void shouldQueryAndCloseOrderAndReuseUnchangedConfig() throws Exception {
        WxPayService service = mock(WxPayService.class);
        wechatPayApi = new WechatPayApiImpl(optionApi, () -> service);
        WxPayOrderQueryV3Result queryResult = new WxPayOrderQueryV3Result();
        queryResult.setOutTradeNo("order-123");
        queryResult.setTradeState("SUCCESS");
        when(service.queryOrderV3(null, "order-123")).thenReturn(queryResult);

        assertEquals("SUCCESS", wechatPayApi.queryOrder("order-123").tradeState());
        options.put("ALIYUN_OCR_ENABLED", "1");
        wechatPayApi.closeOrder("order-123");

        verify(service).closeOrderV3("order-123");
        verify(service, times(1)).setConfig(any());
    }

    @Test
    void shouldCloseOldClientWhenConfigChangesAndOnShutdown() throws Exception {
        WxPayService service = mock(WxPayService.class);
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        WxPayConfig config = new WxPayConfig();
        config.setApiV3HttpClient(client);
        when(service.getConfig()).thenReturn(config);
        wechatPayApi = new WechatPayApiImpl(optionApi, () -> service);

        wechatPayApi.closeOrder("order-123");
        options.put("WECHAT_PAY_MCH_SERIAL_NO", "new-serial");
        wechatPayApi.closeOrder("order-123");
        wechatPayApi.destroy();

        verify(service, times(2)).setConfig(any());
        verify(client, times(2)).close();
    }

    @Test
    void shouldRejectInvalidOrderAndNotifyUrlBeforeCallingSdk() {
        WxPayService service = mock(WxPayService.class);
        wechatPayApi = new WechatPayApiImpl(optionApi, () -> service);
        assertThrows(BusinessException.class, () -> wechatPayApi.queryOrder("bad/order"));
        options.put("WECHAT_PAY_NOTIFY_URL", "http://pay.example.com/notify");
        assertThrows(BusinessException.class, () -> wechatPayApi.createJsapiOrder(validRequest()));
        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectStaleNotificationBeforeCallingSdk() {
        String timestamp = Long.toString(Instant.now().getEpochSecond() - 3600);
        assertThrows(BusinessException.class,
            () -> wechatPayApi.parseNotification("{}", timestamp, "nonce", "signature", "serial"));
        verifyNoInteractions(optionApi);
    }

    @Test
    void shouldRejectInvalidApiV3Key() {
        options.put("WECHAT_PAY_API_V3_KEY", "short-key");
        assertThrows(BusinessException.class, () -> wechatPayApi.closeOrder("order-123"));
    }

    @Test
    void shouldVerifyAndDecryptNotificationAndRejectTampering() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        options.put("WECHAT_PAY_PRIVATE_KEY", pem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        options.put("WECHAT_PAY_PUBLIC_KEY", pem("PUBLIC KEY", keys.getPublic().getEncoded()));
        String payload =
            """
                {"appid":"app-id","mchid":"merchant-id","out_trade_no":"order-123",
                "transaction_id":"wx-123","trade_state":"SUCCESS","amount":{"total":100,"currency":"CNY"}}
                """;
        String body = encryptedBody(payload);
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String signature = sign(keys, timestamp, body);
        try {
            var result = wechatPayApi.parseNotification(body, timestamp, "nonce", signature,
                "PUB_KEY_ID_TEST");
            assertEquals(100, result.totalAmount());
            assertEquals("SUCCESS", result.tradeState());
            assertEquals("wx-123", result.transactionId());
            assertThrows(BusinessException.class,
                () -> wechatPayApi.parseNotification(body + " ", timestamp, "nonce", signature,
                    "PUB_KEY_ID_TEST"));
            String foreignBody = encryptedBody(payload.replace("merchant-id", "foreign-merchant"));
            String foreignSignature = sign(keys, timestamp, foreignBody);
            assertThrows(BusinessException.class,
                () -> wechatPayApi.parseNotification(foreignBody, timestamp, "nonce",
                    foreignSignature,
                    "PUB_KEY_ID_TEST"));
        } finally {
            wechatPayApi.destroy();
        }
    }

    private String encryptedBody(String payload) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
            new SecretKeySpec(options.get("WECHAT_PAY_API_V3_KEY").getBytes(StandardCharsets.UTF_8),
                "AES"),
            new GCMParameterSpec(128, "123456789012".getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("transaction".getBytes(StandardCharsets.UTF_8));
        String ciphertext = Base64.getEncoder()
            .encodeToString(cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        return "{\"resource\":{\"ciphertext\":\"" + ciphertext
            + "\",\"nonce\":\"123456789012\",\"associated_data\":\"transaction\"}}";
    }

    private String sign(KeyPair keys, String timestamp, String body) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keys.getPrivate());
        signer.update((timestamp + "\nnonce\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private String pem(String type, byte[] content) {
        return "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(content)
            + "\n-----END " + type + "-----";
    }

    private WechatPayOrderRequest validRequest() {
        return new WechatPayOrderRequest("app-id", "description", "order-123", 100,
            "openid", null, null);
    }
}
