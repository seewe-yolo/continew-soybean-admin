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

package top.continew.admin.system.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.admin.common.api.system.OptionApi;
import top.continew.admin.system.enums.OptionCategoryEnum;
import top.continew.admin.system.service.OptionService;

import java.util.Map;

/**
 * 系统参数业务 API 实现。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
@Service
@RequiredArgsConstructor
public class OptionApiImpl implements OptionApi {

    private final OptionService baseService;

    @Override
    public Map<String, String> getByCategory(String category) {
        return baseService.getByCategory(OptionCategoryEnum.valueOf(category));
    }
}
