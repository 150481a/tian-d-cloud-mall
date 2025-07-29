package com.td.boot.starter.oos.service;

import com.td.boot.starter.oos.model.OosFile;
import com.td.boot.starter.oos.model.OosUploadResult;
import com.td.boot.starter.oos.properties.OosProperties;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * OOS 服務提供商接口。
 * 定義了所有 OOS 實現必須支持的操作。
 */
public interface OosProvider {

    /**
     * 初始化 OOS 提供商。
     * @param properties OOS 配置屬性
     */
    void initialize(OosProperties properties);

    /**
     * 上傳文件。
     *
     * @param inputStream 文件輸入流
     * @param objectKey   文件在 OOS 中的 Key (完整路徑，例如：images/test/photo.jpg)
     * @param contentType 文件內容類型 (例如：image/jpeg, application/json)
     * @param metadata    文件的自定義元數據
     * @return 上傳結果，包含文件 Key 和 URL
     */
    OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata);

    /**
     * 下載文件。
     *
     * @param objectKey 文件在 OOS 中的 Key
     * @return 文件輸入流
     */
    InputStream download(String objectKey);

    /**
     * 刪除文件。
     *
     * @param objectKey 文件在 OOS 中的 Key
     */
    void delete(String objectKey);

    /**
     * 判斷文件是否存在。
     *
     * @param objectKey 文件在 OOS 中的 Key
     * @return true 如果文件存在，否則 false
     */
    boolean exists(String objectKey);

    /**
     * 獲取文件的元數據。
     *
     * @param objectKey 文件在 OOS 中的 Key
     * @return OosFile 對象，包含文件信息和元數據
     */
    OosFile getFileMetadata(String objectKey);

    /**
     * 列出指定前綴下的文件。
     *
     * @param prefix 前綴 (例如：images/test/)
     * @return 文件列表
     */
    List<OosFile> listFiles(String prefix);

    /**
     * 生成用於下載的預簽名 URL。
     *
     * @param objectKey 文件在 OOS 中的 Key
     * @param expireInSeconds URL 有效期 (秒)
     * @return 預簽名 URL
     */
    URL generatePresignedUrl(String objectKey, long expireInSeconds);

    /**
     * 生成用於上傳的預簽名 URL。
     *
     * @param objectKey 文件在 OOS 中的 Key
     * @param expireInSeconds URL 有效期 (秒)
     * @param contentType 上傳時的內容類型
     * @return 預簽名 URL
     */
    URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType);
}
