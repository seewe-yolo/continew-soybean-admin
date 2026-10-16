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

import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import org.springframework.util.StringUtils;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.admin.common.api.wechatpay.WechatPayApi;
import top.continew.admin.common.api.wechatpay.WechatPayNotification;
import top.continew.admin.common.api.wechatpay.WechatPayOrderRequest;
import top.continew.admin.common.api.wechatpay.WechatPayOrderResult;
import top.continew.starter.core.exception.BusinessException;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * WxJava 微信支付 V3 API 实现。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public class WechatPayApiImpl implements WechatPayApi {

    private static final String CATEGORY = "THIRD_PARTY";
    private static final String ENABLED = "WECHAT_PAY_ENABLED";
    private static final String APP_ID = "WECHAT_PAY_APP_ID";
    private static final String MCH_ID = "WECHAT_PAY_MCH_ID";
    private static final String CERT_SERIAL_NO = "WECHAT_PAY_MCH_SERIAL_NO";
    private static final String PRIVATE_KEY = "WECHAT_PAY_PRIVATE_KEY";
    private static final String API_V3_KEY = "WECHAT_PAY_API_V3_KEY";
    private static final String NOTIFY_URL = "WECHAT_PAY_NOTIFY_URL";
    private static final String PUBLIC_KEY_ID = "WECHAT_PAY_PUBLIC_KEY_ID";
    private static final String PUBLIC_KEY = "WECHAT_PAY_PUBLIC_KEY";

    private final OptionApi optionApi;
    private final Supplier<WxPayService> serviceFactory;
    private final ReentrantReadWriteLock serviceLock = new ReentrantReadWriteLock();
    private ServiceHolder serviceHolder;

    public WechatPayApiImpl(OptionApi optionApi) {
        this(optionApi, WxPayServiceImpl::new);
    }

    WechatPayApiImpl(OptionApi optionApi, Supplier<WxPayService> serviceFactory) {
        this.optionApi = optionApi;
        this.serviceFactory = serviceFactory;
    }

    @Override
    public WechatPayOrderResult createJsapiOrder(WechatPayOrderRequest request) {
        if (request == null || !StringUtils.hasText(request.description())
            || !StringUtils.hasText(request.outTradeNo()) || request.totalAmount() == null
            || request.totalAmount() <= 0 || !StringUtils.hasText(request.payerOpenId())) {
            throw new BusinessException("微信支付订单参数不完整");
        }
        validateOrderNo(request.outTradeNo());
        if (request.description().getBytes(StandardCharsets.UTF_8).length > 127
            || request.payerOpenId().length() > 128
            || (request.attach() != null
                && request.attach().getBytes(StandardCharsets.UTF_8).length > 128)) {
            throw new BusinessException("微信支付订单参数长度超限");
        }
        try {
            Map<String, String> options = getOptions();
            String appId = required(options, APP_ID);
            if (StringUtils.hasText(request.appId()) && !appId.equals(request.appId())) {
                throw new BusinessException("订单 AppID 与微信支付配置不一致");
            }
            String notifyUrl = textOrDefault(request.notifyUrl(), options.get(NOTIFY_URL));
            validateNotifyUrl(notifyUrl);
            WxPayUnifiedOrderV3Request wxRequest = new WxPayUnifiedOrderV3Request()
                .setAppid(appId)
                .setMchid(options.get(MCH_ID))
                .setDescription(request.description())
                .setOutTradeNo(request.outTradeNo())
                .setNotifyUrl(notifyUrl)
                .setAttach(request.attach())
                .setAmount(new WxPayUnifiedOrderV3Request.Amount()
                    .setTotal(request.totalAmount()).setCurrency("CNY"))
                .setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(request.payerOpenId()));
            WxPayUnifiedOrderV3Result.JsapiResult result =
                execute(options, service -> service.createOrderV3(TradeTypeEnum.JSAPI, wxRequest));
            return new WechatPayOrderResult(result.getAppId(), result.getTimeStamp(),
                result.getNonceStr(), result.getPackageValue(), result.getSignType(),
                result.getPaySign(), result.getPrepayId(), null, request.outTradeNo(), null, null,
                request.totalAmount(), "CNY");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("微信支付下单失败", e);
        }
    }

    @Override
    public WechatPayOrderResult queryOrder(String outTradeNo) {
        validateOrderNo(outTradeNo);
        try {
            WxPayOrderQueryV3Result result =
                execute(getOptions(), service -> service.queryOrderV3(null, outTradeNo));
            Integer total = result.getAmount() == null ? null : result.getAmount().getTotal();
            return new WechatPayOrderResult(result.getAppid(), null, null, null, null, null, null,
                result.getTransactionId(), result.getOutTradeNo(), result.getTradeState(),
                result.getTradeStateDesc(), total,
                result.getAmount() == null ? null : result.getAmount().getCurrency());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("微信支付订单查询失败", e);
        }
    }

    @Override
    public void closeOrder(String outTradeNo) {
        validateOrderNo(outTradeNo);
        try {
            execute(getOptions(), service -> {
                service.closeOrderV3(outTradeNo);
                return null;
            });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("微信支付订单关闭失败", e);
        }
    }

    @Override
    public WechatPayNotification parseNotification(String body, String timestamp, String nonce,
        String signature, String serial) {
        if (!StringUtils.hasText(body) || !StringUtils.hasText(timestamp)
            || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)
            || !StringUtils.hasText(serial)) {
            throw new BusinessException("微信支付通知参数不完整");
        }
        try {
            long notificationTime = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            if (notificationTime < now - 300 || notificationTime > now + 300) {
                throw new BusinessException("微信支付通知时间戳无效");
            }
            Map<String, String> options = getOptions();
            SignatureHeader header = SignatureHeader.builder().timeStamp(timestamp).nonce(nonce)
                .signature(signature).serial(serial).build();
            var result =
                execute(options, service -> service.parseOrderNotifyV3Result(body, header))
                    .getResult();
            if (result == null || !required(options, MCH_ID).equals(result.getMchid())
                || !required(options, APP_ID).equals(result.getAppid())) {
                throw new BusinessException("微信支付通知商户或 AppID 不匹配");
            }
            Integer total = result.getAmount() == null ? null : result.getAmount().getTotal();
            return new WechatPayNotification(result.getAppid(), result.getMchid(),
                result.getOutTradeNo(), result.getTransactionId(), result.getTradeState(), total,
                result.getAmount() == null ? null : result.getAmount().getCurrency(),
                result.getSuccessTime());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("微信支付通知校验或解密失败", e);
        }
    }

    private Map<String, String> getOptions() {
        Map<String, String> options = optionApi.getByCategory(CATEGORY);
        if (!isEnabled(options.get(ENABLED))) {
            throw new BusinessException("微信支付未启用");
        }
        return options.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("WECHAT_PAY_"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private <T> T execute(Map<String, String> options, PayOperation<T> operation)
        throws WxPayException {
        serviceLock.readLock().lock();
        try {
            if (serviceHolder != null && serviceHolder.options.equals(options)) {
                return operation.apply(serviceHolder.service);
            }
        } finally {
            serviceLock.readLock().unlock();
        }
        // 配置切换等待在途请求完成，再关闭旧连接池。
        serviceLock.writeLock().lock();
        WxPayService service;
        try {
            if (serviceHolder == null || !serviceHolder.options.equals(options)) {
                WxPayConfig config = createConfig(options);
                service = serviceFactory.get();
                service.setConfig(config);
                closeClient();
                serviceHolder = new ServiceHolder(options, service);
            }
            service = serviceHolder.service;
            serviceLock.readLock().lock();
        } finally {
            serviceLock.writeLock().unlock();
        }
        try {
            return operation.apply(service);
        } finally {
            serviceLock.readLock().unlock();
        }
    }

    private WxPayConfig createConfig(Map<String, String> options) {
        String apiV3Key = required(options, API_V3_KEY);
        if (!apiV3Key.matches("[\\x20-\\x7E]{32}")) {
            throw new BusinessException("微信支付 API v3 密钥必须为 32 位 ASCII 字符");
        }
        WxPayConfig config = new WxPayConfig();
        config.setAppId(required(options, APP_ID));
        config.setMchId(required(options, MCH_ID));
        config.setCertSerialNo(required(options, CERT_SERIAL_NO));
        config
            .setPrivateKeyContent(required(options, PRIVATE_KEY).getBytes(StandardCharsets.UTF_8));
        config.setApiV3Key(apiV3Key);
        config.setNotifyUrl(options.get(NOTIFY_URL));
        config.setPublicKeyId(required(options, PUBLIC_KEY_ID));
        config.setPublicKeyContent(required(options, PUBLIC_KEY).getBytes(StandardCharsets.UTF_8));
        config.setFullPublicKeyModel(true);
        return config;
    }

    /**
     * 应用关闭时释放微信支付连接池。
     */
    @PreDestroy
    public void destroy() {
        serviceLock.writeLock().lock();
        try {
            closeClient();
            serviceHolder = null;
        } finally {
            serviceLock.writeLock().unlock();
        }
    }

    private void closeClient() {
        if (serviceHolder == null) {
            return;
        }
        var client = serviceHolder.service.getConfig().getApiV3HttpClient();
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                throw new BusinessException("微信支付连接池关闭失败", e);
            }
        }
    }

    private void validateOrderNo(String outTradeNo) {
        if (outTradeNo == null || !outTradeNo.matches("[A-Za-z0-9_\\-*|@]{6,32}")) {
            throw new BusinessException("商户订单号须为 6 至 32 位字母、数字或 _-*|@");
        }
    }

    private void validateNotifyUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("微信支付通知地址须为不带参数的 HTTPS 地址");
        }
    }

    private String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("微信支付配置项未填写：" + key);
        }
        return value;
    }

    private boolean isEnabled(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @FunctionalInterface
    private interface PayOperation<T> {

        T apply(WxPayService service) throws WxPayException;
    }

    private record ServiceHolder(Map<String, String> options, WxPayService service) {
    }
}
