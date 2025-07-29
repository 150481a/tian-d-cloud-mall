package com.td.boot.starter.oos.config;

import com.td.boot.starter.oos.properties.OosProperties;
import com.td.boot.starter.oos.service.OosProvider;
import com.td.boot.starter.oos.service.OosTemplate;
import com.td.boot.starter.oos.service.impl.AliyunOosProvider;
import com.td.boot.starter.oos.service.impl.AwsS3OosProvider;
import com.td.boot.starter.oos.service.impl.LocalOosProvider;
import com.td.boot.starter.oos.service.impl.TencentOosProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OOS Starter 的自動配置類。
 * 根據配置的 provider 類型自動裝配 OosProvider 和 OosTemplate。
 */
@Configuration
@EnableConfigurationProperties(OosProperties.class)
@ConditionalOnProperty(prefix = "td.oos", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class OosAutoConfiguration {

    private final OosProperties properties;

    public OosAutoConfiguration(OosProperties properties) {
        this.properties = properties;
    }

    /**
     * 根據配置的 OOS 提供商類型創建對應的 OosProvider 實例。
     */
    @Bean
    @ConditionalOnMissingBean(OosProvider.class) // 如果容器中沒有 OosProvider 實例，則創建
    public OosProvider oosProvider() {
        OosProvider provider;
        switch (properties.getProvider()) {
            case ALIYUN:
                if (!isClassPresent("com.aliyun.oss.OSSClientBuilder")) {
                    log.error("td.oos.provider 設置為 ALIYUN，但未找到 aliyun-sdk-oss 依賴。請檢查 pom.xml。");
                    throw new RuntimeException("缺少 aliyun-sdk-oss 依賴，無法初始化 AliyunOosProvider。");
                }
                provider = new AliyunOosProvider();
                break;
            case TENCENT:
                if (!isClassPresent("com.qcloud.cos.COSClient")) {
                    log.error("td.oos.provider 設置為 TENCENT，但未找到 cos_api 依賴。請檢查 pom.xml。");
                    throw new RuntimeException("缺少 cos_api 依賴，無法初始化 TencentOosProvider。");
                }
                if (!StringUtils.hasText(properties.getRegion())) {
                    throw new IllegalArgumentException("使用騰訊雲 COS 必須配置 td.oos.region 屬性！");
                }
                provider = new TencentOosProvider();
                break;
            case AWS:
                if (!isClassPresent("software.amazon.awssdk.services.s3.S3Client")) {
                    log.error("td.oos.provider 設置為 AWS，但未找到 aws-java-sdk-s3 或 regions 依賴。請檢查 pom.xml。");
                    throw new RuntimeException("缺少 AWS S3 SDK 依賴，無法初始化 AwsS3OosProvider。");
                }
                if (!StringUtils.hasText(properties.getRegion())) {
                    throw new IllegalArgumentException("使用 AWS S3 必須配置 td.oos.region 屬性！");
                }
                provider = new AwsS3OosProvider();
                break;
//            case HUAWEI:
//                if (!isClassPresent("com.huaweicloud.sdk.obs.ObsClient")) {
//                    log.error("td.oos.provider 設置為 HUAWEI，但未找到 huaweicloud-sdk-obs 依賴。請檢查 pom.xml。");
//                    throw new RuntimeException("缺少 華為雲 OBS SDK 依賴，無法初始化 HuaweiOosProvider。");
//                }
//                provider = new HuaweiOosProvider();
//                break;
            case LOCAL:
                if (!isClassPresent("org.apache.commons.io.IOUtils")) {
                    log.error("td.oos.provider 設置為 LOCAL，但未找到 commons-io 依賴。請檢查 pom.xml。");
                    throw new RuntimeException("缺少 commons-io 依賴，無法初始化 LocalOosProvider。");
                }
                if (!StringUtils.hasText(properties.getLocalStoragePath())) {
                    throw new IllegalArgumentException("使用本地存儲必須配置 td.oos.local-storage-path 屬性！");
                }
                provider = new LocalOosProvider();
                break;
            default:
                throw new IllegalArgumentException("不支持的 OOS 提供商類型: " + properties.getProvider());
        }
        provider.initialize(properties); // 初始化 OOS 提供商
        return provider;
    }

    /**
     * 創建 OosTemplate 實例。
     */
    @Bean
    @ConditionalOnMissingBean(OosTemplate.class) // 如果容器中沒有 OosTemplate 實例，則創建
    public OosTemplate oosTemplate(OosProvider oosProvider) {
        return new OosTemplate(oosProvider, properties);
    }

    /**
     * 檢查類是否存在於 classpath 中。
     */
    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
