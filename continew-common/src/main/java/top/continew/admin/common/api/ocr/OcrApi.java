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

package top.continew.admin.common.api.ocr;

/**
 * 阿里云 OCR 业务 API。
 *
 * @author seewe-yolo
 * @since 4.2.0
 */
public interface OcrApi {

    /**
     * 识别身份证。
     *
     * @param image 图片二进制内容
     * @return 结构化识别结果
     */
    OcrResult recognizeIdCard(byte[] image);

    /**
     * 识别营业执照。
     *
     * @param image 图片二进制内容
     * @return 结构化识别结果
     */
    OcrResult recognizeBusinessLicense(byte[] image);

    /**
     * 识别发票。
     *
     * @param image 图片二进制内容
     * @return 结构化识别结果
     */
    OcrResult recognizeInvoice(byte[] image);
}
