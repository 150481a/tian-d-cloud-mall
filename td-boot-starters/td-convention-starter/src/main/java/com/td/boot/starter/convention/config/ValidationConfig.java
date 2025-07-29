package com.td.boot.starter.convention.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public class ValidationConfig {
    /**
     * 配置 MessageSource，使其能夠加載 ValidationMessages.properties。
     *
     * @return MessageSource 實例
     */
    @Bean
    public MessageSource validationMessageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("ValidationMessages"); // 指向 src/main/resources/ValidationMessages.properties
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true); // 如果找不到消息，則使用錯誤碼作為默認消息
        return source;
    }

    /**
     * 配置 LocalValidatorFactoryBean，將其與自定義的 MessageSource 關聯起來。
     * 這樣校驗器就可以使用我們定義的國際化消息。
     *
     * @param messageSource 自定義的 MessageSource
     * @return LocalValidatorFactoryBean 實例
     */
    @Bean
    public LocalValidatorFactoryBean validator(@Qualifier("validationMessageSource") MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
