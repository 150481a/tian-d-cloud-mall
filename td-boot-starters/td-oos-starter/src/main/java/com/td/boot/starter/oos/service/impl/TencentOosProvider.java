package com.td.boot.starter.oos.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
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
 * 騰訊雲 COS 服務提供商實現。
 */
@Slf4j
public class TencentOosProvider implements OosProvider, DisposableBean {

    private COSClient cosClient;
    private OosProperties properties;

    @Override
    public void initialize(OosProperties properties) {
        this.properties = properties;
        COSCredentials cred = new BasicCOSCredentials(properties.getAccessKeyId(), properties.getAccessKeySecret());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion())); // 騰訊雲 COS 需要 region
        this.cosClient = new COSClient(cred, clientConfig);

        log.info("TencentOosProvider 初始化成功，Region: {}, Bucket: {}",
                properties.getRegion(), properties.getBucketName());
    }

    @Override
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            if (metadata != null && !metadata.isEmpty()) {
                metadata.forEach(objectMetadata::setHeader); // COS 通常用 user-meta- 開頭
            }

            PutObjectRequest putObjectRequest = new PutObjectRequest(properties.getBucketName(), objectKey, inputStream, objectMetadata);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            // COS 的 URL 結構通常是 bucketName-APPID.cos.region.myqcloud.com/objectKey
            // 或者通過預簽名URL
            String url = cosClient.generatePresignedUrl(properties.getBucketName(), objectKey,
                    Date.from(LocalDateTime.now().plusSeconds(properties.getPreSignedUrlExpireTime()).atZone(ZoneId.systemDefault()).toInstant()),
                    HttpMethodName.GET).toString();

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
            GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), objectKey);
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            log.info("文件下載成功，Key: {}", objectKey);
            return cosObject.getObjectContent();
        } catch (Exception e) {
            log.error("文件下載失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件下載失敗: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            cosClient.deleteObject(properties.getBucketName(), objectKey);
            log.info("文件刪除成功，Key: {}", objectKey);
        } catch (Exception e) {
            log.error("文件刪除失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件刪除失敗: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            return cosClient.doesObjectExist(properties.getBucketName(), objectKey);
        } catch (Exception e) {
            log.error("檢查文件是否存在失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("檢查文件是否存在失敗: " + objectKey, e);
        }
    }

    @Override
    public OosFile getFileMetadata(String objectKey) {
        try {
            ObjectMetadata objectMetadata = cosClient.getObjectMetadata(properties.getBucketName(), objectKey);
            OosFile oosFile = new OosFile();
            oosFile.setKey(objectKey);
            oosFile.setFileName(objectKey.substring(objectKey.lastIndexOf("/") + 1));
            oosFile.setSize(objectMetadata.getContentLength());
            oosFile.setContentType(objectMetadata.getContentType());
            oosFile.setLastModified(objectMetadata.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
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
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            listObjectsRequest.setBucketName(properties.getBucketName());
            listObjectsRequest.setPrefix(prefix);
            ObjectListing objectListing = cosClient.listObjects(listObjectsRequest);
            return objectListing.getObjectSummaries().stream().map(s -> {
                OosFile oosFile = new OosFile();
                oosFile.setKey(s.getKey());
                oosFile.setFileName(s.getKey().substring(s.getKey().lastIndexOf("/") + 1));
                oosFile.setSize(s.getSize());
                oosFile.setLastModified(s.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
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
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey, HttpMethodName.GET);
            req.setExpiration(expiration);
            return cosClient.generatePresignedUrl(req);
        } catch (Exception e) {
            log.error("生成預簽名 URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireInSeconds * 1000);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(properties.getBucketName(), objectKey, HttpMethodName.PUT);
            req.setExpiration(expiration);
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentType(contentType);
            req.setResponseHeaders(responseHeaders); // 為PUT請求設置Content-Type
            return cosClient.generatePresignedUrl(req);
        } catch (Exception e) {
            log.error("生成預簽名 Put URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 Put URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
            log.info("TencentOosProvider COS 客戶端已關閉。");
        }
    }
}
