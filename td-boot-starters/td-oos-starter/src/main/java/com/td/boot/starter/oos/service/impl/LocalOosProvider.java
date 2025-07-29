package com.td.boot.starter.oos.service.impl;

import com.td.boot.starter.oos.exception.OosException;
import com.td.boot.starter.oos.model.OosFile;
import com.td.boot.starter.oos.model.OosUploadResult;
import com.td.boot.starter.oos.properties.OosProperties;
import com.td.boot.starter.oos.service.OosProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地文件存儲服務提供商實現。
 * 主要用於開發和測試環境。
 */
@Slf4j
public class LocalOosProvider implements OosProvider {

    private String localStoragePath; // 基礎存儲路徑

    @Override
    public void initialize(OosProperties properties) {
        this.localStoragePath = properties.getLocalStoragePath();
        if (!StringUtils.hasText(localStoragePath)) {
            throw new OosException("本地存儲路徑 td.oos.local-storage-path 未配置！");
        }
        File baseDir = new File(localStoragePath);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (!created) {
                throw new OosException("無法創建本地存儲目錄: " + localStoragePath);
            }
        }
        log.info("LocalOosProvider 初始化成功，本地存儲路徑: {}", localStoragePath);
    }

    private Path getFilePath(String objectKey) {
        if (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1); // 移除開頭的斜槓，避免絕對路徑問題
        }
        return Paths.get(localStoragePath, objectKey);
    }

    @Override
    public OosUploadResult upload(InputStream inputStream, String objectKey, String contentType, Map<String, String> metadata) {
        Path filePath = getFilePath(objectKey);
        try {
            Files.createDirectories(filePath.getParent()); // 確保父目錄存在
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                IOUtils.copy(inputStream, fos);
            }
            log.info("本地文件上傳成功，路徑: {}", filePath);
            // 本地存儲的 URL 可以是文件協議或者配置一個靜態資源服務器
            String url = "file://" + filePath.toAbsolutePath().toString();
            // 這裡沒有 etag 概念，簡單返回一個 UUID
            return new OosUploadResult(objectKey, url, UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("本地文件上傳失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("本地文件上傳失敗: " + objectKey, e);
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
        Path filePath = getFilePath(objectKey);
        try {
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                throw new OosException("文件不存在或無法讀取: " + objectKey);
            }
            log.info("本地文件下載成功，路徑: {}", filePath);
            return new FileInputStream(filePath.toFile());
        } catch (Exception e) {
            log.error("本地文件下載失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("本地文件下載失敗: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path filePath = getFilePath(objectKey);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("本地文件刪除成功，路徑: {}", filePath);
            } else {
                log.warn("嘗試刪除不存在的本地文件: {}", filePath);
            }
        } catch (Exception e) {
            log.error("本地文件刪除失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("本地文件刪除失敗: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path filePath = getFilePath(objectKey);
        return Files.exists(filePath);
    }

    @Override
    public OosFile getFileMetadata(String objectKey) {
        Path filePath = getFilePath(objectKey);
        try {
            if (!Files.exists(filePath)) {
                return null; // 文件不存在
            }
            OosFile oosFile = new OosFile();
            oosFile.setKey(objectKey);
            oosFile.setFileName(filePath.getFileName().toString());
            oosFile.setSize(Files.size(filePath));
            oosFile.setLastModified(LocalDateTime.ofInstant(Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()));
            // 對於本地文件，Content-Type 無法直接從文件系統獲取，需要額外判斷或在元數據中保存
            // oosFile.setContentType("application/octet-stream"); // 默認值
            oosFile.setUrl("file://" + filePath.toAbsolutePath().toString());
            return oosFile;
        } catch (Exception e) {
            log.error("獲取本地文件元數據失敗，Key: {}: {}", objectKey, e.getMessage(), e);
            throw new OosException("獲取本地文件元數據失敗: " + objectKey, e);
        }
    }

    @Override
    public List<OosFile> listFiles(String prefix) {
        Path directoryPath = getFilePath(prefix);
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return new ArrayList<>(); // 目錄不存在或不是目錄
        }
        List<OosFile> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(directoryPath)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        OosFile oosFile = new OosFile();
                        try {
                            // 計算相對路徑作為 key
                            String relativeKey = directoryPath.relativize(path).toString();
                            oosFile.setKey(prefix.endsWith("/") ? prefix + relativeKey : prefix + "/" + relativeKey); // 確保 key 正確
                            oosFile.setFileName(path.getFileName().toString());
                            oosFile.setSize(Files.size(path));
                            oosFile.setLastModified(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault()));
                            oosFile.setUrl("file://" + path.toAbsolutePath().toString());
                            // oosFile.setContentType(...); // 需要額外判斷文件類型
                        } catch (IOException e) {
                            log.warn("獲取本地文件信息失敗: {}", path, e);
                            return null;
                        }
                        return oosFile;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出本地文件失敗，Prefix: {}: {}", prefix, e.getMessage(), e);
            throw new OosException("列出本地文件失敗: " + prefix, e);
        }
        return files;
    }

    @Override
    public URL generatePresignedUrl(String objectKey, long expireInSeconds) {
        Path filePath = getFilePath(objectKey);
        try {
            // 本地文件沒有預簽名URL的概念，直接返回文件URL
            // 在生產環境，你需要一個Web服務器來提供這些文件
            return filePath.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new OosException("生成本地文件 URL 失敗: " + objectKey, e);
        }
    }

    @Override
    public URL generatePresignedPutUrl(String objectKey, long expireInSeconds, String contentType) {
        // 本地文件沒有預簽名 PUT URL 的概念，直接返回文件 URL
        return generatePresignedUrl(objectKey, expireInSeconds);
    }
}
