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

package top.continew.admin.ocr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.continew.admin.common.api.ocr.OcrApi;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.admin.ocr.api.AliyunOcrApiImpl;

/**
 * 阿里云 OCR 插件配置。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Configuration(proxyBeanMethods = false)
public class AliyunOcrConfiguration {

    /**
     * 注册进程内 OCR 业务 API。
     *
     * @param optionApi 系统参数 API
     * @param objectMapper JSON 映射器
     * @return OCR 业务 API
     */
    @Bean
    public OcrApi ocrApi(OptionApi optionApi, ObjectMapper objectMapper) {
        return new AliyunOcrApiImpl(optionApi, objectMapper);
    }
}
