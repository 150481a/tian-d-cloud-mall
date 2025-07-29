package com.td.boot.starter.convention.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "td.convention")
public class ConventionProperties {

    /**
     * 是否啟用 Convention，默認為 true
     */
    private boolean enabled = true;

    /**
     * 是否启用 td-convention-starter 的全局异常处理器。
     * 默认为 true。
     */
    private boolean enableGlobalExceptionHandler = true;

    /**
     * 是否启用 td-convention-starter 的国际化消息源配置。
     * 默认为 true。
     */
    private boolean enableMessageSource = true;

    /**
     * 分页查询的默认属性。
     */
    @NestedConfigurationProperty // 标记这是一个嵌套的配置属性
    private PageProperties page = new PageProperties();


    /**
     * 分页相关的默认配置属性。
     */
    @Data
    public static class PageProperties {
        /**
         * 默认的每页大小。
         * 默认为 10。
         */
        private int defaultPageSize = 10;

        /**
         * 最大的每页大小，防止一次性查询过大数据。
         * 默认为 100。
         */
        private int maxPageSize = 100;


    }

}
