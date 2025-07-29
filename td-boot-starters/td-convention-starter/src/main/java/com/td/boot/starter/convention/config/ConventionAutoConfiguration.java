package com.td.boot.starter.convention.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
@EnableConfigurationProperties(ConventionProperties.class) // 启用 ConventionProperties 的绑定
@ConditionalOnProperty(prefix = "td.convention", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({GlobalExceptionHandler.class, ValidationConfig.class})
public class ConventionAutoConfiguration {

    private final ConventionProperties conventionProperties;

    // 通过构造函数注入 ConventionProperties
    public ConventionAutoConfiguration(ConventionProperties conventionProperties) {
        this.conventionProperties = conventionProperties;
    }

    // 重新定义 MessageSource Bean，使其受 enableMessageSource 控制
    // 如果您在 MessageSourceAutoConfiguration 中已经定义了 @Bean MessageSource，
    // 则需要将 MessageSourceAutoConfiguration 的内容合并到这里，或调整其顺序。
    // 这里以将所有相关配置放在 ConventionAutoConfiguration 为例。
    @Bean
    @ConditionalOnProperty(prefix = "td.convention", name = "enable-message-source", havingValue = "true", matchIfMissing = true)
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // 加载 td-convention-starter 自身的 messages 文件
        messageSource.setBasenames("classpath:/messages", "classpath:/common/messages", "classpath:/biz/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(60);
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    // 重新定义 validator Bean，确保它使用上述条件创建的 MessageSource
    // 同时，我们可以利用 ConventionProperties 中的分页配置来优化 PageQuery
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    // 你可以在这里添加其他基于 conventionProperties 控制的 Bean，
    // 例如，如果你想根据配置调整 PageQuery 的默认值
    /*
    @Bean
    public PageQuery defaultPageQuery() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageSize(conventionProperties.getPage().getDefaultPageSize());
        // 可以根据需要添加更多默认值
        return pageQuery;
    }
    */
}
