package com.td.boot.starter.oos.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表示一個在 OOS 中的文件信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OosFile {
    /** 文件在 OOS 中的完整路徑，即 Key (例如：images/test.jpg) */
    private String key;
    /** 文件名 (不包含路徑) */
    private String fileName;
    /** 文件大小 (字節) */
    private long size;
    /** 文件內容類型 (例如：image/jpeg) */
    private String contentType;
    /** 最後修改時間 */
    private LocalDateTime lastModified;
    /** 文件的公共訪問 URL */
    private String url;
}
