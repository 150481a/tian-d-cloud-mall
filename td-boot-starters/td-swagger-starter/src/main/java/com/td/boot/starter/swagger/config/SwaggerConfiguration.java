package com.td.boot.starter.swagger.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "td.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfiguration {

    private final SwaggerProperties swaggerProperties;

    @Autowired
    public SwaggerConfiguration(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = swaggerProperties;
    }
    @Bean
    public OpenAPI springShopOpenAPI() {
        Info info = new Info()
                .title(swaggerProperties.getTitle())
                .description(swaggerProperties.getDescription())
                .version(swaggerProperties.getVersion());

        if (swaggerProperties.getTermsOfServiceUrl() != null) {
            info.termsOfService(swaggerProperties.getTermsOfServiceUrl());
        }
        if (swaggerProperties.getLicense() != null && swaggerProperties.getLicenseUrl() != null) {
            info.license(new License().name(swaggerProperties.getLicense()).url(swaggerProperties.getLicenseUrl()));
        }

        OpenAPI openAPI = new OpenAPI().info(info);

        if (swaggerProperties.getExternalDocDescription() != null && swaggerProperties.getExternalDocUrl() != null) {
            openAPI.externalDocs(new ExternalDocumentation()
                    .description(swaggerProperties.getExternalDocDescription())
                    .url(swaggerProperties.getExternalDocUrl()));
        }

        // --- JWT 安全配置 ---
        final String securitySchemeName = "bearerAuth"; // JWT 認證方案的名稱

        openAPI.addSecurityItem(new SecurityRequirement().addList(securitySchemeName)) // 將此安全要求應用到所有 API
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)      // HTTP 認證
                                        .scheme("bearer")                   // 使用 Bearer 方案
                                        .bearerFormat("JWT")                // 格式為 JWT
                                        .description("請在 'Value' 輸入 JWT Token (例如: eyJhbGci...，前面不需要加 Bearer)"))); // 提示

        return openAPI;
    }


}
