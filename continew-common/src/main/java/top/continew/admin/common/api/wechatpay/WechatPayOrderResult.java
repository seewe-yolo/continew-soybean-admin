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
 * 微信支付订单结果。
 *
 * @param appId AppID
 * @param timeStamp 调起支付时间戳
 * @param nonceStr 调起支付随机串
 * @param packageValue 调起支付 package 参数
 * @param signType 签名类型
 * @param paySign 调起支付签名
 * @param prepayId 预支付交易会话标识
 * @param transactionId 微信支付订单号
 * @param outTradeNo 商户订单号
 * @param tradeState 交易状态
 * @param tradeStateDescription 交易状态描述
 * @param totalAmount 订单金额，单位为分
 * @param currency 货币类型
 * @author seewe-yolo
 * @since 4.2.0
 */
public record WechatPayOrderResult(String appId, String timeStamp, String nonceStr,
    String packageValue, String signType, String paySign, String prepayId, String transactionId,
    String outTradeNo, String tradeState, String tradeStateDescription, Integer totalAmount,
    String currency) {
}
