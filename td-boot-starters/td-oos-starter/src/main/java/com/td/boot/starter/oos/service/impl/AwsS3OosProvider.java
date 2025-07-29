package com.td.boot.starter.oos.service.impl;

import com.td.boot.starter.oos.exception.OosException;
import com.td.boot.starter.oos.model.OosFile;
import com.td.boot.starter.oos.model.OosUploadResult;
import com.td.boot.starter.oos.properties.OosProperties;
import com.td.boot.starter.oos.service.OosProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Amazon S3 服務提供商實現。
 * 使用 AWS SDK for Java v2。
 */
@Slf4j
public class AwsS3OosProvider implements OosProvider, DisposableBean {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private OosProperties properties;

    @Override
    public void initialize(OosProperties properties) {
        this.properties = properties;
        try {
            Region awsRegion = Region.of(properties.getRegion()); // AWS S3 必須有 Region

            this.s3Client = S3Client.builder()
                    .region(awsRegion)
                    // 如果 Access Key / Secret Key 在環境變量或 EC2 角色中，可以不顯式設置
                    // .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getAccessKeySecret())))
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(awsRegion)
                    // .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKeyId(), properties.getAccessKeySecret())))
                    .build();

            log.info("AwsS3OosProvider 初始化成功，Region: {}, Bucket: {}",
                    properties.getRegion(), properties.getBucketName());
        } catch (Exception e) {
            log.error("AwsS3OosProvider 初始化失敗: {}", e.getMessage(), e);
            throw new OosException("初始化 AWS S3 失敗", e);
        }
    }

    @Override
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType);

            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }
            PutObjectRequest putObjectRequest = requestBuilder.build();

            PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, inputStream.available()));

            String url = generatePresignedUrl(objectKey, properties.getPreSignedUrlExpireTime()).toString();

            log.info("文件上傳成功，Key: {}, URL: {}, ETag: {}", objectKey, url, putObjectResponse.eTag());
            return new OosUploadResult(objectKey, url, putObjectResponse.eTag());
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
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("文件下載失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件下載失敗: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            log.info("文件刪除成功，Key: {}", objectKey);
        } catch (Exception e) {
            log.error("文件刪除失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("文件刪除失敗: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) { // 文件不存在會拋出此異常
            return false;
        } catch (Exception e) {
            log.error("檢查文件是否存在失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("檢查文件是否存在失敗: " + objectKey, e);
        }
    }

    @Override
    public OosFile getFileMetadata(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .build();
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

            OosFile oosFile = new OosFile();
            oosFile.setKey(objectKey);
            oosFile.setFileName(objectKey.substring(objectKey.lastIndexOf("/") + 1));
            oosFile.setSize(headObjectResponse.contentLength());
            oosFile.setContentType(headObjectResponse.contentType());
            oosFile.setLastModified(headObjectResponse.lastModified().atZone(ZoneId.systemDefault()).toLocalDateTime());
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
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(properties.getBucketName())
                    .prefix(prefix)
                    .build();
            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

            return listObjectsResponse.contents().stream().map(s -> {
                OosFile oosFile = new OosFile();
                oosFile.setKey(s.key());
                oosFile.setFileName(s.key().substring(s.key().lastIndexOf("/") + 1));
                oosFile.setSize(s.size());
                oosFile.setLastModified(s.lastModified().atZone(ZoneId.systemDefault()).toLocalDateTime());
                oosFile.setUrl(generatePresignedUrl(s.key(), properties.getPreSignedUrlExpireTime()).toString());
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
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expireInSeconds))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(objectKey)
                            .build())
                    .build();
            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
            return presignedGetObjectRequest.url();
        } catch (Exception e) {
            log.error("生成預簽名 URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
        try {
            PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expireInSeconds))
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(objectKey)
                            .contentType(contentType) // 設置上傳時的內容類型
                            .build())
                    .build();
            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);
            return presignedPutObjectRequest.url();
        } catch (Exception e) {
            log.error("生成預簽名 Put URL 失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("生成預簽名 Put URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
            log.info("AwsS3OosProvider S3 客戶端已關閉。");
        }
        if (s3Presigner != null) {
            s3Presigner.close();
            log.info("AwsS3OosProvider S3 預簽名器已關閉。");
        }
    }
}
