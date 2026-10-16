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

package top.continew.admin.ocr.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeBusinessLicenseRequest;
import com.aliyun.ocr_api20210707.models.RecognizeBusinessLicenseResponse;
import com.aliyun.ocr_api20210707.models.RecognizeBusinessLicenseResponseBody;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardRequest;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardResponse;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardResponseBody;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceRequest;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceResponse;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.continew.admin.common.api.ocr.OcrResult;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.starter.core.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 OCR API 实现测试。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
class AliyunOcrApiImplTest {

    private OptionApi optionApi;
    private AliyunOcrApiImpl.ClientFactory clientFactory;
    private Client client;
    private Map<String, String> options;
    private AliyunOcrApiImpl ocrApi;

    @BeforeEach
    void setUp() throws Exception {
        optionApi = mock(OptionApi.class);
        options = new HashMap<>();
        options.put("ALIYUN_OCR_ENABLED", "1");
        options.put("ALIYUN_OCR_ACCESS_KEY_ID", "access-key-id");
        options.put("ALIYUN_OCR_ACCESS_KEY_SECRET", "access-key-secret");
        when(optionApi.getByCategory("THIRD_PARTY")).thenReturn(options);
        clientFactory = mock(AliyunOcrApiImpl.ClientFactory.class);
        client = mock(Client.class);
        when(clientFactory.create(any(Config.class))).thenReturn(client);
        ocrApi = new AliyunOcrApiImpl(optionApi, new ObjectMapper(), clientFactory);
    }

    @Test
    void shouldCallAllDocumentRecognitionApis() throws Exception {
        when(client.recognizeIdcard(any(RecognizeIdcardRequest.class))).thenReturn(
            new RecognizeIdcardResponse().setBody(new RecognizeIdcardResponseBody()
                .setRequestId("id-request").setCode("200").setData("{\"name\":\"Ada\"}")));
        when(client.recognizeBusinessLicense(any(RecognizeBusinessLicenseRequest.class)))
            .thenReturn(new RecognizeBusinessLicenseResponse().setBody(
                new RecognizeBusinessLicenseResponseBody().setRequestId("license-request")
                    .setCode("200").setData("{\"name\":\"Example Ltd\"}")));
        when(client.recognizeInvoice(any(RecognizeInvoiceRequest.class))).thenReturn(
            new RecognizeInvoiceResponse().setBody(new RecognizeInvoiceResponseBody()
                .setRequestId("invoice-request").setCode("200").setData("{\"amount\":\"12\"}")));

        OcrResult idCard = ocrApi.recognizeIdCard(new byte[] {1});
        OcrResult license = ocrApi.recognizeBusinessLicense(new byte[] {2});
        OcrResult invoice = ocrApi.recognizeInvoice(new byte[] {3});

        assertEquals("id-request", idCard.requestId());
        assertEquals("Ada", idCard.data().get("name"));
        assertEquals("license-request", license.requestId());
        assertEquals("Example Ltd", license.data().get("name"));
        assertEquals("invoice-request", invoice.requestId());
        assertEquals("12", invoice.data().get("amount"));
        verify(client).recognizeIdcard(any(RecognizeIdcardRequest.class));
        verify(client).recognizeBusinessLicense(any(RecognizeBusinessLicenseRequest.class));
        verify(client).recognizeInvoice(any(RecognizeInvoiceRequest.class));
        verify(clientFactory).create(any(Config.class));
    }

    @Test
    void shouldRejectEmptyImage() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> ocrApi.recognizeIdCard(new byte[0]));

        assertEquals("图片内容不能为空", exception.getMessage());
    }

    @Test
    void shouldRejectCallsWhenOcrIsDisabled() {
        options.put("ALIYUN_OCR_ENABLED", "0");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> ocrApi.recognizeIdCard(new byte[] {1}));

        assertEquals("阿里云 OCR 未启用", exception.getMessage());
    }

    @Test
    void shouldRecreateClientWhenStoredConfigurationChanges() throws Exception {
        when(client.recognizeIdcard(any(RecognizeIdcardRequest.class))).thenReturn(
            new RecognizeIdcardResponse().setBody(new RecognizeIdcardResponseBody()
                .setRequestId("request-id").setCode("200").setData("{\"name\":\"Ada\"}")));

        ocrApi.recognizeIdCard(new byte[] {1});
        options.put("ALIYUN_OCR_ACCESS_KEY_SECRET", "rotated-secret");
        ocrApi.recognizeIdCard(new byte[] {1});

        verify(clientFactory, org.mockito.Mockito.times(2)).create(any(Config.class));
    }

    @Test
    void shouldRejectNullDataInSdkResponse() throws Exception {
        when(client.recognizeIdcard(any(RecognizeIdcardRequest.class))).thenReturn(
            new RecognizeIdcardResponse().setBody(new RecognizeIdcardResponseBody()
                .setRequestId("request-id").setCode("200").setData("null")));

        BusinessException exception = assertThrows(BusinessException.class,
            () -> ocrApi.recognizeIdCard(new byte[] {1}));

        assertEquals("阿里云 OCR 未返回识别数据", exception.getMessage());
    }
}
