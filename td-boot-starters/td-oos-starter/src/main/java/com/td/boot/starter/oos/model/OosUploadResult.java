package com.td.boot.starter.oos.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OOS 文件上傳結果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OosUploadResult {
    /** 文件在 OOS 中的完整路徑，即 Key (例如：images/test.jpg) */
    private String key;
    /** 文件的公共訪問 URL */
    private String url;
    /** 其他可能返回的元數據，例如 ETag (文件指紋) */
    private String etag;
}
