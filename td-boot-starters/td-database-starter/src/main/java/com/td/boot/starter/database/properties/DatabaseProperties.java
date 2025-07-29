package com.td.boot.starter.database.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 數據庫配置屬性。
 * 前綴為 "td.database"。
 */
@ConfigurationProperties(prefix = "td.database")
@Data
public class DatabaseProperties {

    /**
     * 是否啟用數據庫自動配置。
     * 默認為 true。
     */
    private boolean enabled = true;

    /**
     * 數據庫連接 URL。
     */
    private String url;

    /**
     * 數據庫用戶名。
     */
    private String username;

    /**
     * 數據庫密碼。
     */
    private String password;

    /**
     * 數據庫驅動類名。
     */
    private String driverClassName;

    /**
     * HikariCP 連接池配置。
     */
    private HikariPoolProperties hikari = new HikariPoolProperties();

    /**
     * JPA 配置。
     */
    private JpaProperties jpa = new JpaProperties();

    /**
     * MyBatis 配置。
     */
    private MyBatisProperties mybatis = new MyBatisProperties();

    @Data
    public static class HikariPoolProperties {
        private int minimumIdle = 5;
        private int maximumPoolSize = 20;
        private long idleTimeout = 600000; // 10 minutes
        private long connectionTimeout = 30000; // 30 seconds
        private long maxLifetime = 1800000; // 30 minutes
        private String poolName = "td-mall-hikari-pool";
    }

    @Data
    public static class JpaProperties {
        /**
         * 是否啟用 JPA 自動配置。
         * 默認為 true。
         */
        private boolean enabled = true;
        /**
         * 實體掃描包路徑，多個包名用逗號分隔。
         * 例如：com.td.cloud.mall.**.entity
         */
        private String[] entityPackages;
        /**
         * DDL 自動生成策略：none, update, create, create-drop, validate。
         */
        private String ddlAuto = "update";
        /**
         * 是否顯示 SQL。
         */
        private boolean showSql = true;
        /**
         * SQL 格式化。
         */
        private boolean formatSql = true;
        /**
         * 數據庫平台方言。
         * 例如：org.hibernate.dialect.MySQL8Dialect
         */
        private String databasePlatform;
        /**
         * 其他 JPA 屬性。
         */
        private Map<String, String> properties;
    }

    @Data
    public static class MyBatisProperties {
        /**
         * 是否啟用 MyBatis 自動配置。
         * 默認為 true。
         */
        private boolean enabled = true;
        /**
         * Mapper 接口掃描包路徑，多個包名用逗號分隔。
         * 例如：com.td.cloud.mall.**.mapper
         */
        private String[] mapperPackages;
        /**
         * Mapper XML 文件路徑，多個路徑用逗號分隔。
         * 例如：classpath*:/mapper/*.xml
         */
        private String[] mapperLocations;
        /**
         * MyBatis 配置文件的路徑。
         */
        private String configLocation;
        /**
         * MyBatis 別名掃描包。
         */
        private String typeAliasesPackage;
    }
}
