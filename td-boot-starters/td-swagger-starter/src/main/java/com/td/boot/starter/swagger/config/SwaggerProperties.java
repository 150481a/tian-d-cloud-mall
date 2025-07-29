package com.td.boot.starter.swagger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "td.swagger")
public class SwaggerProperties {

    /**
     * 是否啟用 Swagger，默認為 true
     */
    private boolean enabled = true;

    /**
     * API 文檔標題
     */
    private String title = "TD Boot Starter Swagger 文檔";

    /**
     * API 文檔描述
     */
    private String description = "百萬級用戶商城系統 API 文檔";

    /**
     * API 版本
     */
    private String version = "v1.0.0";

    /**
     * 服務條款 URL
     */
    private String termsOfServiceUrl;

    /**
     * 許可證信息
     */
    private String license;

    /**
     * 許可證 URL
     */
    private String licenseUrl;

    /**
     * 外部文檔描述
     */
    private String externalDocDescription;

    /**
     * 外部文檔 URL
     */
    private String externalDocUrl;

}
