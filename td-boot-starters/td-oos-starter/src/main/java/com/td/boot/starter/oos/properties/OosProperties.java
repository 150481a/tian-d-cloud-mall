package com.td.boot.starter.oos.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "td.oos")
public class OosProperties {
    /**
     * 是否啟用 OOS 自動配置。
     * 默認為 true。
     */
    private boolean enabled = true;

    /**
     * OOS 服務提供商類型。
     * 可選值：ALIYUN, TENCENT, AWS, HUAWEI, LOCAL
     */
    private OosProviderType provider = OosProviderType.ALIYUN; // 默認為阿里云

    /**
     * Access Key ID。
     */
    private String accessKeyId;

    /**
     * Access Key Secret。
     */
    private String accessKeySecret;

    /**
     * OOS 服務的 Endpoint (例如：oss-cn-hangzhou.aliyuncs.com)。
     * 對於 AWS S3 來說，通常不需要 endpoint，因為 region 決定了 endpoint。
     */
    private String endpoint;

    /**
     * 默認的 Bucket 名稱。
     */
    private String bucketName;

    /**
     * 預簽名 URL 的過期時間 (秒)。
     * 默認為 3600 秒 (1 小時)。
     */
    private long preSignedUrlExpireTime = 3600L; // 1小時

    /**
     * AWS S3 特有的區域配置 (Region)。
     * 例如：us-east-1, ap-northeast-1。
     */
    private String region;

    /**
     * 本地存儲的基礎路徑。
     * 僅在 provider 為 LOCAL 時生效。
     */
    private String localStoragePath;

    /**
     * OOS 提供商類型枚舉。
     */
    public enum OosProviderType {
        ALIYUN,     // 阿里云 OSS
        TENCENT,    // 騰訊雲 COS
        AWS,        // Amazon S3
        HUAWEI,     // 華為雲 OBS
        LOCAL       // 本地存儲 (用於開發測試，非生產建議)
    }
}
