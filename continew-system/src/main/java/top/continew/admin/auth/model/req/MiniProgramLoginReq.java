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

package top.continew.admin.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serial;

/**
 * 微信小程序手机号授权登录请求参数
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Data
@Schema(description = "微信小程序手机号授权登录请求参数")
public class MiniProgramLoginReq extends LoginReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * wx.getPhoneNumber 返回的一次性授权码
     */
    @Schema(description = "wx.getPhoneNumber 返回的一次性授权码", example = "071abc...xyz")
    @NotBlank(message = "手机号授权码不能为空")
    private String code;
}
