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

package top.continew.admin.common.api.wechatpay;

/**
 * 微信支付 V3 业务 API。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public interface WechatPayApi {

    /**
     * 创建 JSAPI 或小程序支付订单。
     *
     * @param request 订单参数
     * @return 调起支付所需参数
     */
    WechatPayOrderResult createJsapiOrder(WechatPayOrderRequest request);

    /**
     * 查询商户订单。
     *
     * @param outTradeNo 商户订单号
     * @return 微信订单信息
     */
    WechatPayOrderResult queryOrder(String outTradeNo);

    /**
     * 关闭未支付订单。
     *
     * @param outTradeNo 商户订单号
     */
    void closeOrder(String outTradeNo);

    /**
     * 验签并解密支付通知。
     *
     * @param body 通知请求体
     * @param timestamp Wechatpay-Timestamp
     * @param nonce Wechatpay-Nonce
     * @param signature Wechatpay-Signature
     * @param serial Wechatpay-Serial
     * @return 解密后的支付通知
     */
    WechatPayNotification parseNotification(String body, String timestamp, String nonce,
        String signature, String serial);
}
