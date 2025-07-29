package com.td.boot.starter.oos.service;

import com.td.boot.starter.oos.exception.OosException;
import com.td.boot.starter.oos.model.OosFile;
import com.td.boot.starter.oos.model.OosUploadResult;
import com.td.boot.starter.oos.properties.OosProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OOS 操作模板。
 * 提供統一的 OOS 操作接口，內部委託給具體的 OosProvider 實現。
 */
@Slf4j
public class OosTemplate {
    private final OosProvider oosProvider;
    private final OosProperties properties;

    public OosTemplate(OosProvider oosProvider, OosProperties properties) {
        this.oosProvider = oosProvider;
        this.properties = properties;
        log.info("OosTemplate 初始化成功，使用提供商: {}", properties.getProvider());
    }

    /**
     * 上傳文件。
     *
     * @param inputStream 文件輸入流
     * @param objectKey   文件在 OOS 中的 Key (完整路徑，例如：images/test/photo.jpg)
     * @param contentType 文件內容類型
     * @param metadata    文件的自定義元數據
     * @return 上傳結果
     */
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
        return oosProvider.upload(inputStream, objectKey, contentType, metadata);
    }

    /**
     * 上傳文件 (簡化版)。
     * @param inputStream 文件輸入流
     * @param objectKey 文件 Key
     * @param contentType 文件內容類型
     * @return 上傳結果
     */
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType) {
        return upload(inputStream, objectKey, contentType, Collections.emptyMap());
    }

    /**
     * 上傳 MultipartFile 文件。
     * @param file MultipartFile 對象
     * @param objectKey 文件 Key
     * @return 上傳結果
     */
    public OosUploadResult upload(MultipartFile file, String objectKey) {
        try {
            return oosProvider.upload(file.getInputStream(), objectKey, file.getContentType(), Collections.emptyMap());
        } catch (IOException e) {
            throw new OosException("讀取 MultipartFile 輸入流失敗", e);
        }
    }

    /**
     * 下載文件。
     * @param objectKey 文件 Key
     * @return 文件輸入流
     */
    public InputStream download(String objectKey) {
        return oosProvider.download(objectKey);
    }

    /**
     * 刪除文件。
     * @param objectKey 文件 Key
     */
    public void delete(String objectKey) {
        oosProvider.delete(objectKey);
    }

    /**
     * 判斷文件是否存在。
     * @param objectKey 文件 Key
     * @return true 如果文件存在，否則 false
     */
    public boolean exists(String objectKey) {
        return oosProvider.exists(objectKey);
    }

    /**
     * 獲取文件的元數據。
     * @param objectKey 文件 Key
     * @return 文件信息
     */
    public OosFile getFileMetadata(String objectKey) {
        return oosProvider.getFileMetadata(objectKey);
    }

    /**
     * 列出指定前綴下的文件。
     * @param prefix 前綴
     * @return 文件列表
     */
    public List<OosFile> listFiles(String prefix) {
        return oosProvider.listFiles(prefix);
    }

    /**
     * 生成用於下載的預簽名 URL (使用默認過期時間)。
     * @param objectKey 文件 Key
     * @return 預簽名 URL
     */
    public URL generatePresignedUrl(String objectKey) {
        return oosProvider.generatePresignedUrl(objectKey, properties.getPreSignedUrlExpireTime());
    }

    /**
     * 生成用於下載的預簽名 URL。
     * @param objectKey 文件 Key
     * @param expireInSeconds URL 有效期 (秒)
     * @return 預簽名 URL
     */
    public URL generatePresignedUrl(String objectKey, long expireInSeconds) {
        return oosProvider.generatePresignedUrl(objectKey, expireInSeconds);
    }

    /**
     * 生成用於上傳的預簽名 URL (使用默認過期時間)。
     * @param objectKey 文件 Key
     * @param contentType 上傳時的內容類型
     * @return 預簽名 URL
     */
    public URL generatePresignedPutUrl(String objectKey, String contentType) {
        return oosProvider.generatePresignedPutUrl(objectKey, properties.getPreSignedUrlExpireTime(), contentType);
    }

    /**
     * 生成用於上傳的預簽名 URL。
     * @param objectKey 文件 Key
     * @param expireInSeconds URL 有效期 (秒)
     * @param contentType 上傳時的內容類型
     * @return 預簽名 URL
     */
    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
        return oosProvider.generatePresignedPutUrl(objectKey, expireInSeconds, contentType);
    }
}
