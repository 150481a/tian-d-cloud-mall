//package com.td.boot.starter.oos.service.impl;
//
//import com.huaweicloud.sdk.core.auth.BasicCredentials;
//import com.huaweicloud.sdk.core.exception.ServiceResponseException;
//import com.huaweicloud.sdk.obs.v1.ObsClient;
//import com.huaweicloud.sdk.obs.v1.model.*;
//import com.td.boot.starter.oos.exception.OosException;
//import com.td.boot.starter.oos.model.OosFile;
//import com.td.boot.starter.oos.model.OosUploadResult;
//import com.td.boot.starter.oos.properties.OosProperties;
//import com.td.boot.starter.oos.service.OosProvider;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.DisposableBean;
//
//import java.io.InputStream;
//import java.net.URL;
//import java.time.ZoneId;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * 華為雲 OBS 服務提供商實現。
// */
//@Slf4j
//public class HuaweiOosProvider implements OosProvider, DisposableBean {
//    private ObsClient obsClient;
//    private OosProperties properties;
//
//    @Override
//    public void initialize(OosProperties properties) {
//        this.properties = properties;
//        try {
//            BasicCredentials auth = new BasicCredentials(properties.getAccessKeyId(), properties.getAccessKeySecret());
//            this.obsClient = new ObsClient(auth, properties.getEndpoint());
//
//            log.info("HuaweiOosProvider 初始化成功，Endpoint: {}, Bucket: {}",
//                    properties.getEndpoint(), properties.getBucketName());
//        } catch (Exception e) {
//            log.error("HuaweiOosProvider 初始化失敗: {}", e.getMessage(), e);
//            throw new OosException("初始化華為雲 OBS 失敗", e);
//        }
//    }
//
//    @Override
//    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
//        try {
//            PutObjectRequest putObjectRequest = new PutObjectRequest();
//            putObjectRequest.setBucketName(properties.getBucketName());
//            putObjectRequest.setObjectKey(objectKey);
//            putObjectRequest.setInputStream(inputStream);
//            // setContentType 是直接在 PutObjectRequest 上設置的
//            putObjectRequest.setContentType(contentType);
//
//            // 自定義元數據需要作為請求頭設置，且需要以 "x-obs-meta-" 開頭
//            if (metadata != null && !metadata.isEmpty()) {
//                metadata.forEach((key, value) -> putObjectRequest.addHeader("x-obs-meta-" + key, value));
//            }
//
//            PutObjectResponse putObjectResponse = obsClient.putObject(putObjectRequest);
//
//            // 生成一個預簽名URL，以便上傳後可以直接訪問
//            String url = generatePresignedUrl(objectKey, properties.getPreSignedUrlExpireTime()).toString();
//
//            log.info("文件上傳成功，Key: {}, URL: {}, ETag: {}", objectKey, url, putObjectResponse.getEtag());
//            return new OosUploadResult(objectKey, url, putObjectResponse.getEtag());
//        } catch (Exception e) {
//            log.error("文件上傳失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("文件上傳失敗: " + objectKey, e);
//        } finally {
//            // 確保輸入流被關閉
//            try {
//                if (inputStream != null) {
//                    inputStream.close();
//                }
//            } catch (Exception e) {
//                log.warn("關閉輸入流失敗: {}", e.getMessage());
//            }
//        }
//    }
//
//    @Override
//    public InputStream download(String objectKey) {
//        try {
//            GetObjectRequest getObjectRequest = new GetObjectRequest(properties.getBucketName(), objectKey);
//            GetObjectResponse getObjectResponse = obsClient.getObject(getObjectRequest);
//            log.info("文件下載成功，Key: {}", objectKey);
//            return getObjectResponse.getObjectContent();
//        } catch (Exception e) {
//            log.error("文件下載失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("文件下載失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public void delete(String objectKey) {
//        try {
//            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(properties.getBucketName(), objectKey);
//            obsClient.deleteObject(deleteObjectRequest);
//            log.info("文件刪除成功，Key: {}", objectKey);
//        } catch (Exception e) {
//            log.error("文件刪除失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("文件刪除失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public boolean exists(String objectKey) {
//        try {
//            // 在 OBS 中，判斷對象是否存在通常是通過嘗試獲取其元數據來實現
//            // 如果對象不存在，getObjectMetadata 會拋出 ServiceResponseException (狀態碼 404)
//            obsClient.getObjectMetadata(new GetObjectMetadataRequest(properties.getBucketName(), objectKey));
//            return true;
//        } catch (ServiceResponseException e) {
//            if (e.getStatusCode() == 404) {
//                return false; // 對象不存在
//            }
//            log.error("檢查文件是否存在失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("檢查文件是否存在失敗: " + objectKey, e);
//        } catch (Exception e) {
//            log.error("檢查文件是否存在失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("檢查文件是否存在失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public OosFile getFileMetadata(String objectKey) {
//        try {
//            GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(properties.getBucketName(), objectKey);
//            GetObjectMetadataResponse getObjectMetadataResponse = obsClient.getObjectMetadata(getObjectMetadataRequest);
//
//            OosFile oosFile = new OosFile();
//            oosFile.setKey(objectKey);
//            oosFile.setFileName(objectKey.substring(objectKey.lastIndexOf("/") + 1));
//            oosFile.setSize(getObjectMetadataResponse.getContentLength());
//            oosFile.setContentType(getObjectMetadataResponse.getContentType());
//            oosFile.setLastModified(getObjectMetadataResponse.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//            oosFile.setUrl(generatePresignedUrl(objectKey, properties.getPreSignedUrlExpireTime()).toString());
//            return oosFile;
//        } catch (ServiceResponseException e) {
//            if (e.getStatusCode() == 404) {
//                return null; // 對象不存在
//            }
//            log.error("獲取文件元數據失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("獲取文件元數據失敗: " + objectKey, e);
//        } catch (Exception e) {
//            log.error("獲取文件元數據失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("獲取文件元數據失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public List<OosFile> listFiles(String prefix) {
//        try {
//            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
//            listObjectsRequest.setBucketName(properties.getBucketName());
//            listObjectsRequest.setPrefix(prefix);
//            ListObjectsResponse listObjectsResponse = obsClient.listObjects(listObjectsRequest);
//            return listObjectsResponse.getContents().stream().map(obsObject -> {
//                OosFile oosFile = new OosFile();
//                oosFile.setKey(obsObject.getKey());
//                oosFile.setFileName(obsObject.getKey().substring(obsObject.getKey().lastIndexOf("/") + 1));
//                oosFile.setSize(obsObject.getSize());
//                oosFile.setLastModified(obsObject.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//                oosFile.setUrl(generatePresignedUrl(obsObject.getKey(), properties.getPreSignedUrlExpireTime()).toString());
//                return oosFile;
//            }).collect(Collectors.toList());
//        } catch (Exception e) {
//            log.error("列出文件失敗，Prefix: {}: {}", prefix, e.getMessage(), e);
//            throw new OosException("列出文件失敗: " + prefix, e);
//        }
//    }
//
//    @Override
//    public URL generatePresignedUrl(String objectKey, long expireInSeconds) {
//        try {
//            CreateSignedUrlRequest request = new CreateSignedUrlRequest();
//            request.setMethod(Method.GET);
//            request.setBucketName(properties.getBucketName());
//            request.setObjectKey(objectKey);
//            request.setExpires(expireInSeconds); // 秒
//
//            CreateSignedUrlResponse response = obsClient.createSignedUrl(request);
//            return new URL(response.getSignedUrl());
//        } catch (Exception e) {
//            log.error("生成預簽名 URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("生成預簽名 URL 失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
//        try {
//            CreateSignedUrlRequest request = new CreateSignedUrlRequest();
//            request.setMethod(Method.PUT);
//            request.setBucketName(properties.getBucketName());
//            request.setObjectKey(objectKey);
//            request.setExpires(expireInSeconds); // 秒
//            request.setContentType(contentType); // 設置上傳時的內容類型
//
//            CreateSignedUrlResponse response = obsClient.createSignedUrl(request);
//            return new URL(response.getSignedUrl());
//        } catch (Exception e) {
//            log.error("生成預簽名 Put URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
//            throw new OosException("生成預簽名 Put URL 失敗: " + objectKey, e);
//        }
//    }
//
//    @Override
//    public void destroy() {
//        if (obsClient != null) {
//            obsClient.close();
//            log.info("HuaweiOosProvider OBS 客戶端已關閉。");
//        }
//    }
//
//}
