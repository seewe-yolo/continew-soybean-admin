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
 * 微信支付 V3 支付通知。
 *
 * @param appId AppID
 * @param mchId 商户号
 * @param outTradeNo 商户订单号
 * @param transactionId 微信支付订单号
 * @param tradeState 交易状态
 * @param totalAmount 订单金额，单位为分
 * @param currency 货币类型
 * @param successTime 支付完成时间
 * @author seewe-yolo
 * @since 4.2.0
 */
public record WechatPayNotification(String appId, String mchId, String outTradeNo,
    String transactionId, String tradeState, Integer totalAmount, String currency,
    String successTime) {
}
