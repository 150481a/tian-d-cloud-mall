package com.td.boot.starter.oos.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.td.boot.starter.oos.exception.OosException;
import com.td.boot.starter.oos.model.OosFile;
import com.td.boot.starter.oos.model.OosUploadResult;
import com.td.boot.starter.oos.properties.OosProperties;
import com.td.boot.starter.oos.service.OosProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 阿里云 OSS 服務提供商實現。
 */
@Slf4j
public class AliyunOosProvider implements OosProvider, DisposableBean {

    private OSSClientBuilder ossClientBuilder;
    private OSS ossClient;
    private OosProperties properties;

    @Override
    public void initialize(OosProperties properties) {
        this.properties = properties;
        try {
            // 優先嘗試使用環境變量中的憑證
            // 如果環境變量中沒有，則使用配置中的 accessKeyId 和 accessKeySecret
            // 這種方式更安全，推薦在生產環境使用環境變量
            this.ossClient = new OSSClientBuilder().build(properties.getEndpoint(),
                    properties.getAccessKeyId(), properties.getAccessKeySecret());

            log.info("AliyunOosProvider 初始化成功，Endpoint: {}, Bucket: {}",
                    properties.getEndpoint(), properties.getBucketName());
        } catch (Exception e) {
            log.error("AliyunOosProvider 初始化失敗: {}", e.getMessage(), e);
            throw new OosException("初始化阿里云 OSS 失敗", e);
        }
    }

    @Override
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) {
                metadata.forEach(objectMetadata::setHeader);
            }

            PutObjectResult putObjectResult = ossClient.putObject(properties.getBucketName(), objectKey, inputStream, objectMetadata);
            String url = ossClient.generatePresignedUrl(properties.getBucketName(), objectKey,
                            Date.from(LocalDateTime.now().plusSeconds(properties.getPreSignedUrlExpireTime()).atZone(ZoneId.systemDefault()).toInstant()))
                    .toString(); // 上傳成功後生成一個臨時可訪問的URL

            log.info("文件上傳成功，Key: {}, URL: {}, ETag: {}", objectKey, url, putObjectResult.getETag());
            return new OosUploadResult(objectKey, url, putObjectResult.getETag());
        } catch (Exception e) {
            log.error("文件上傳失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件上傳失敗: " + objectKey, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                log.warn("關閉輸入流失敗: {}", e.getMessage());
            }
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            OSSObject ossObject = ossClient.getObject(properties.getBucketName(), objectKey);
            log.info("文件下載成功，Key: {}", objectKey);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("文件下載失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件下載失敗: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            ossClient.deleteObject(properties.getBucketName(), objectKey);
            log.info("文件刪除成功，Key: {}", objectKey);
        } catch (Exception e) {
            log.error("文件刪除失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件刪除失敗: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            return ossClient.doesObjectExist(properties.getBucketName(), objectKey);
        } catch (Exception e) {
            log.error("檢查文件是否存在失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("檢查文件是否存在失敗: " + objectKey, e);
        }
    }

    @Override
    public OosFile getFileMetadata(String objectKey) {
        try {
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(properties.getBucketName(), objectKey);
            OosFile oosFile = new OosFile();
            oosFile.setKey(objectKey);
            oosFile.setFileName(objectKey.substring(objectKey.lastIndexOf("/") + 1)); // 簡單提取文件名
            oosFile.setSize(objectMetadata.getContentLength());
            oosFile.setContentType(objectMetadata.getContentType());
            oosFile.setLastModified(objectMetadata.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            // 如果需要獲取公共URL，這裡可能需要配置公共訪問域名，或者生成一個預簽名URL
            // 這裡簡單生成一個臨時的預簽名URL作為示例
            oosFile.setUrl(generatePresignedUrl(objectKey, properties.getPreSignedUrlExpireTime()).toString());
            return oosFile;
        } catch (Exception e) {
            log.error("獲取文件元數據失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("獲取文件元數據失敗: " + objectKey, e);
        }
    }

    @Override
    public List<OosFile> listFiles(String prefix) {
        try {
            ObjectListing objectListing = ossClient.listObjects(properties.getBucketName(), prefix);
            return objectListing.getObjectSummaries().stream().map(s -> {
                OosFile oosFile = new OosFile();
                oosFile.setKey(s.getKey());
                oosFile.setFileName(s.getKey().substring(s.getKey().lastIndexOf("/") + 1));
                oosFile.setSize(s.getSize());
                oosFile.setLastModified(s.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                // 列出文件時通常不包含 ContentType，需要單獨獲取
                // oosFile.setContentType(...);
                // 這裡也生成一個臨時的預簽名URL
                oosFile.setUrl(generatePresignedUrl(s.getKey(), properties.getPreSignedUrlExpireTime()).toString());
                return oosFile;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("列出文件失敗，Prefix: {}: {}", prefix, e.getMessage(), e);
            throw new OosException("列出文件失敗: " + prefix, e);
        }
    }

    @Override
    public URL generatePresignedUrl(String objectKey, long expireInSeconds) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireInSeconds * 1000);
            return ossClient.generatePresignedUrl(properties.getBucketName(), objectKey, expiration);
        } catch (Exception e) {
            log.error("生成預簽名 URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireInSeconds * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey);
            request.setExpiration(expiration);
            request.setMethod(com.aliyun.oss.HttpMethod.PUT);
            request.setContentType(contentType); // 指定上傳時的內容類型
            return ossClient.generatePresignedUrl(request);
        } catch (Exception e) {
            log.error("生成預簽名 Put URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 Put URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("AliyunOosProvider OSS 客戶端已關閉。");
        }
    }
}
