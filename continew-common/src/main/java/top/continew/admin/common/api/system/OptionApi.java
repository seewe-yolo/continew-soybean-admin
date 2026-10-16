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

package top.continew.admin.common.api.system;

import java.util.Map;

/**
 * 系统参数业务 API。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public interface OptionApi {

    /**
     * 根据类别查询参数。
     *
     * @param category 参数类别
     * @return 参数键值
     */
    Map<String, String> getByCategory(String category);
}
