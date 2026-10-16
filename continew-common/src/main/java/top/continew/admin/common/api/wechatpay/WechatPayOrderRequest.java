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
 * 微信支付 JSAPI/小程序下单参数。
 *
 * @param appId 应用 AppID，为空时使用配置值，非空时必须与配置一致
 * @param description 商品描述
 * @param outTradeNo 商户订单号
 * @param totalAmount 总金额，单位为分
 * @param payerOpenId 支付用户 OpenID
 * @param attach 附加数据
 * @param notifyUrl 回调地址，为空时使用三方配置中的回调地址
 * @author seewe-yolo
 * @since 4.2.0
 */
public record WechatPayOrderRequest(String appId, String description, String outTradeNo,
    Integer totalAmount, String payerOpenId, String attach, String notifyUrl) {
}
