package com.td.boot.starter.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 通用 JPA Repository 接口。
 * {@code @NoRepositoryBean} 標記表示這個接口不會被 Spring Data JPA 直接掃描為 Repository，
 * 而是作為其他 Repository 接口的父接口。
 *
 * @param <T> 實體類型
 * @param <ID> 主鍵類型
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // 這裡可以定義所有 Repository 都需要的通用方法
    // 例如，自定義的批量操作、查詢等
    // 但通常 JpaRepository 已經提供了很多常用功能
}
