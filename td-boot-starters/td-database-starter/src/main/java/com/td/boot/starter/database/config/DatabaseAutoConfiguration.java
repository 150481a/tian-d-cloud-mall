package com.td.boot.starter.database.config;

import com.td.boot.starter.database.properties.DatabaseProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 數據庫自動配置類。
 * 整合 HikariCP, Spring Data JPA 和 MyBatis。
 */
@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
@ConditionalOnProperty(prefix = "td.database", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableTransactionManagement // 啟用事務管理
@AutoConfigureBefore(DataSourceAutoConfiguration.class) // 優先於 Spring Boot 的默認數據源配置
@Slf4j
public class DatabaseAutoConfiguration {

    private final DatabaseProperties properties;

    public DatabaseAutoConfiguration(DatabaseProperties properties) {
        this.properties = properties;
        log.info("td-database-starter 自動配置已啟用。");
    }

    /**
     * 配置 HikariCP 數據源。
     */
    @Bean
    @Primary // 標記為主要的數據源
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "td.database", name = {"url", "username", "password", "driver-class-name"})
    public DataSource dataSource() {
        if (!properties.isEnabled()) {
            return null;
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setDriverClassName(properties.getDriverClassName());

        // 配置 HikariCP 特定屬性
        DatabaseProperties.HikariPoolProperties hikariProps = properties.getHikari();
        dataSource.setMinimumIdle(hikariProps.getMinimumIdle());
        dataSource.setMaximumPoolSize(hikariProps.getMaximumPoolSize());
        dataSource.setIdleTimeout(hikariProps.getIdleTimeout());
        dataSource.setConnectionTimeout(hikariProps.getConnectionTimeout());
        dataSource.setMaxLifetime(hikariProps.getMaxLifetime());
        dataSource.setPoolName(hikariProps.getPoolName());

        log.info("數據源 [HikariCP] 配置成功: URL={}, PoolName={}", properties.getUrl(), hikariProps.getPoolName());
        return dataSource;
    }

    // --- Spring Data JPA 配置 ---
    @Configuration
    @ConditionalOnClass(JpaProperties.class) // 只有當JPA相關類存在時才配置JPA
    @ConditionalOnProperty(prefix = "td.database.jpa", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableJpaRepositories(basePackages = {"com.td.boot.starter.database.repository"}) // 默認掃描路徑
    @EnableJpaAuditing // 啟用 JPA 審計功能
    public static class JpaConfiguration {

        private final DatabaseProperties databaseProperties;

        public JpaConfiguration(DatabaseProperties databaseProperties) {
            this.databaseProperties = databaseProperties;
            log.info("td-database-starter JPA 自動配置已啟用。");
        }

        /**
         * 配置 JPA 實體管理器工廠。
         */
        @Bean
        @ConditionalOnMissingBean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("dataSource") DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            // 設置實體掃描包
            if (databaseProperties.getJpa().getEntityPackages() != null && databaseProperties.getJpa().getEntityPackages().length > 0) {
                em.setPackagesToScan(databaseProperties.getJpa().getEntityPackages());
                log.info("JPA 實體掃描包: {}", String.join(",", databaseProperties.getJpa().getEntityPackages()));
            } else {
                // 默認掃描 Starter 的 entity 包，以及最終應用程序的 entity 包
                em.setPackagesToScan("com.td.cloud.mall.tddatabasestarter.entity");
                log.warn("td.database.jpa.entity-packages 未配置，將默認掃描 [com.td.cloud.mall.tddatabasestarter.entity]");
            }

            // JPA 屬性配置
            Map<String, Object> jpaProperties = new HashMap<>();
            jpaProperties.put("hibernate.hbm2ddl.auto", databaseProperties.getJpa().getDdlAuto());
            jpaProperties.put("hibernate.show_sql", databaseProperties.getJpa().isShowSql());
            jpaProperties.put("hibernate.format_sql", databaseProperties.getJpa().isFormatSql());
            if (StringUtils.hasText(databaseProperties.getJpa().getDatabasePlatform())) {
                jpaProperties.put("hibernate.dialect", databaseProperties.getJpa().getDatabasePlatform());
            }
            if (databaseProperties.getJpa().getProperties() != null) {
                jpaProperties.putAll(databaseProperties.getJpa().getProperties());
            }
            em.setJpaPropertyMap(jpaProperties);

            log.info("JPA 實體管理器工廠配置成功: DDL Auto={}, Show SQL={}",
                    databaseProperties.getJpa().getDdlAuto(), databaseProperties.getJpa().isShowSql());
            return em;
        }

        /**
         * 配置 JPA 事務管理器。
         */
        @Bean
        @ConditionalOnMissingBean(PlatformTransactionManager.class)
        public PlatformTransactionManager jpaTransactionManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
            log.info("JPA 事務管理器配置成功。");
            return new JpaTransactionManager(entityManagerFactory.getObject());
        }
    }

    // --- MyBatis 配置 ---
    @Configuration
    @ConditionalOnClass(SqlSessionFactory.class) // 只有當MyBatis相關類存在時才配置MyBatis
    @ConditionalOnProperty(prefix = "td.database.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @MapperScan(basePackages = {"com.td.boot.starter.database.mapper"}) // 默認掃描路徑
    public static class MyBatisConfiguration {

        private final DatabaseProperties databaseProperties;

        public MyBatisConfiguration(DatabaseProperties databaseProperties) {
            this.databaseProperties = databaseProperties;
            log.info("td-database-starter MyBatis 自動配置已啟用。");
        }

        /**
         * 配置 MyBatis SqlSessionFactory。
         */
        @Bean
        @ConditionalOnMissingBean
        public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);

            // 設置 Mapper XML 路徑
            if (databaseProperties.getMybatis().getMapperLocations() != null && databaseProperties.getMybatis().getMapperLocations().length > 0) {
                factoryBean.setMapperLocations(org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver(null)
                        .getResources("classpath*:" + String.join(",", databaseProperties.getMybatis().getMapperLocations())));
                log.info("MyBatis Mapper XML 路徑: {}", String.join(",", databaseProperties.getMybatis().getMapperLocations()));
            } else {
                log.warn("td.database.mybatis.mapper-locations 未配置。");
            }

            // 設置 MyBatis 配置文件的路徑
            if (StringUtils.hasText(databaseProperties.getMybatis().getConfigLocation())) {
                factoryBean.setConfigLocation(org.springframework.core.io.support.ResourcePatternUtils.getResourcePatternResolver(null)
                        .getResource("classpath:" + databaseProperties.getMybatis().getConfigLocation()));
                log.info("MyBatis 配置加載: {}", databaseProperties.getMybatis().getConfigLocation());
            }

            // 設置 TypeAliasesPackage
            if (StringUtils.hasText(databaseProperties.getMybatis().getTypeAliasesPackage())) {
                factoryBean.setTypeAliasesPackage(databaseProperties.getMybatis().getTypeAliasesPackage());
                log.info("MyBatis TypeAliasesPackage: {}", databaseProperties.getMybatis().getTypeAliasesPackage());
            }

            // 可以添加其他 MyBatis 配置，例如配置類
             org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
             configuration.setMapUnderscoreToCamelCase(true); // 設置駝峰命名自動映射
             factoryBean.setConfiguration(configuration);

            log.info("MyBatis SqlSessionFactory 配置成功。");
            return factoryBean.getObject();
        }

        /**
         * 配置 MyBatis SqlSessionTemplate。
         */
        @Bean
        @ConditionalOnMissingBean
        public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
            log.info("MyBatis SqlSessionTemplate 配置成功。");
            return new SqlSessionTemplate(sqlSessionFactory);
        }

        /**
         * 配置 MyBatis 事務管理器。
         * 如果沒有 JPA，則需要配置一個 DataSourceTransactionManager。
         * 如果同時存在 JPA 和 MyBatis，Spring 會優先使用 JPA 的 PlatformTransactionManager。
         */
        @Bean
        @ConditionalOnMissingBean(PlatformTransactionManager.class)
        @ConditionalOnProperty(prefix = "td.database.jpa", name = "enabled", havingValue = "false") // 只有在JPA未啟用時才配置
        public PlatformTransactionManager mybatisTransactionManager(@Qualifier("dataSource") DataSource dataSource) {
            log.info("MyBatis 事務管理器 (DataSourceTransactionManager) 配置成功。");
            return new DataSourceTransactionManager(dataSource);
        }
    }

}
