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

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeBusinessLicenseRequest;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardRequest;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceRequest;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import top.continew.admin.common.api.ocr.OcrApi;
import top.continew.admin.common.api.ocr.OcrResult;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.starter.core.exception.BusinessException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 阿里云 OCR API 实现。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public class AliyunOcrApiImpl implements OcrApi {

    private static final String CATEGORY = "THIRD_PARTY";
    private static final String ENABLED = "ALIYUN_OCR_ENABLED";
    private static final String ACCESS_KEY_ID = "ALIYUN_OCR_ACCESS_KEY_ID";
    private static final String ACCESS_KEY_SECRET = "ALIYUN_OCR_ACCESS_KEY_SECRET";
    private static final String ENDPOINT = "ALIYUN_OCR_ENDPOINT";
    private static final String DEFAULT_ENDPOINT = "ocr-api.cn-hangzhou.aliyuncs.com";

    private final OptionApi optionApi;
    private final ObjectMapper objectMapper;
    private final ClientFactory clientFactory;
    private volatile ClientHolder clientHolder;

    public AliyunOcrApiImpl(OptionApi optionApi, ObjectMapper objectMapper) {
        this(optionApi, objectMapper, Client::new);
    }

    AliyunOcrApiImpl(OptionApi optionApi, ObjectMapper objectMapper, ClientFactory clientFactory) {
        this.optionApi = optionApi;
        this.objectMapper = objectMapper;
        this.clientFactory = clientFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OcrResult recognizeIdCard(byte[] image) {
        return execute(image, (client, body) -> {
            var response = client.recognizeIdcard(new RecognizeIdcardRequest().setBody(body));
            return toResult(response.getBody().getRequestId(), response.getBody().getCode(),
                response.getBody().getData());
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OcrResult recognizeBusinessLicense(byte[] image) {
        return execute(image, (client, body) -> {
            var response = client.recognizeBusinessLicense(
                new RecognizeBusinessLicenseRequest().setBody(body));
            return toResult(response.getBody().getRequestId(), response.getBody().getCode(),
                response.getBody().getData());
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OcrResult recognizeInvoice(byte[] image) {
        return execute(image, (client, body) -> {
            var response = client.recognizeInvoice(new RecognizeInvoiceRequest().setBody(body));
            return toResult(response.getBody().getRequestId(), response.getBody().getCode(),
                response.getBody().getData());
        });
    }

    private OcrResult execute(byte[] image, OcrCall ocrCall) {
        if (image == null || image.length == 0) {
            throw new BusinessException("图片内容不能为空");
        }
        try {
            Client client = createClient();
            return ocrCall.execute(client, new ByteArrayInputStream(image));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("阿里云 OCR 调用失败", e);
        }
    }

    private Client createClient() throws Exception {
        Map<String, String> options = optionApi.getByCategory(CATEGORY);
        String enabled = options.get(ENABLED);
        if (!("1".equals(enabled) || "true".equalsIgnoreCase(enabled))) {
            throw new BusinessException("阿里云 OCR 未启用");
        }
        String accessKeyId = options.get(ACCESS_KEY_ID);
        String accessKeySecret = options.get(ACCESS_KEY_SECRET);
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret)) {
            throw new BusinessException("启用阿里云 OCR 时必须配置 AccessKey ID 和 Secret");
        }
        String endpoint = options.get(ENDPOINT);
        endpoint = StringUtils.hasText(endpoint) ? endpoint : DEFAULT_ENDPOINT;
        ClientHolder current = clientHolder;
        if (current != null && current.matches(accessKeyId, accessKeySecret, endpoint)) {
            return current.client;
        }
        synchronized (this) {
            current = clientHolder;
            if (current != null && current.matches(accessKeyId, accessKeySecret, endpoint)) {
                return current.client;
            }
            return createAndCacheClient(accessKeyId, accessKeySecret, endpoint);
        }
    }

    private Client createAndCacheClient(String accessKeyId, String accessKeySecret, String endpoint)
        throws Exception {
        Config config = new Config()
            .setAccessKeyId(accessKeyId)
            .setAccessKeySecret(accessKeySecret)
            .setEndpoint(endpoint);
        Client client = clientFactory.create(config);
        clientHolder = new ClientHolder(accessKeyId, accessKeySecret, endpoint, client);
        return client;
    }

    private OcrResult toResult(String requestId, String code, String dataJson) {
        if (!StringUtils.hasText(dataJson)) {
            String errorCode = StringUtils.hasText(code) ? "，错误码：" + code : "";
            throw new BusinessException("阿里云 OCR 未返回识别数据" + errorCode);
        }
        try {
            Map<String, Object> data = objectMapper.readValue(dataJson,
                new TypeReference<>() {
                });
            if (data == null) {
                throw new BusinessException("阿里云 OCR 未返回识别数据");
            }
            return new OcrResult(requestId, data);
        } catch (JsonProcessingException e) {
            throw new BusinessException("阿里云 OCR 响应解析失败", e);
        }
    }

    @FunctionalInterface
    private interface OcrCall {

        OcrResult execute(Client client, InputStream image) throws Exception;
    }

    @FunctionalInterface
    interface ClientFactory {

        Client create(Config config) throws Exception;
    }

    private static class ClientHolder {

        private final String accessKeyId;
        private final String accessKeySecret;
        private final String endpoint;
        private final Client client;

        ClientHolder(String accessKeyId, String accessKeySecret, String endpoint, Client client) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.endpoint = endpoint;
            this.client = client;
        }

        boolean matches(String accessKeyId, String accessKeySecret, String endpoint) {
            return this.accessKeyId.equals(accessKeyId)
                && this.accessKeySecret.equals(accessKeySecret)
                && this.endpoint.equals(endpoint);
        }
    }
}
