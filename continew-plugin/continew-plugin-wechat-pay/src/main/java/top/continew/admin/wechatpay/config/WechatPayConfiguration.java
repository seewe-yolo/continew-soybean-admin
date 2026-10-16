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

package top.continew.admin.wechatpay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.admin.common.api.wechatpay.WechatPayApi;
import top.continew.admin.wechatpay.api.WechatPayApiImpl;

/**
 * 微信支付插件配置。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Configuration(proxyBeanMethods = false)
public class WechatPayConfiguration {

    /**
     * 注册进程内微信支付业务 API。
     *
     * @param optionApi 系统参数 API
     * @return 微信支付业务 API
     */
    @Bean
    public WechatPayApi wechatPayApi(OptionApi optionApi) {
        return new WechatPayApiImpl(optionApi);
    }
}
