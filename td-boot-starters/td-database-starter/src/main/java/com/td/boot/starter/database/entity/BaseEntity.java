package com.td.boot.starter.database.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 實體基類，包含 ID、創建時間、更新時間。
 * 配合 Spring Data JPA Auditing 自動填充時間戳。
 */
@MappedSuperclass // 標記為超類，其屬性映射到子類表
@EntityListeners(AuditingEntityListener.class) // 啟用 JPA 審計功能
@Data
public abstract class BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動增長主鍵
    private Long id;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 可以添加其他通用字段，如 deleted (邏輯刪除標誌), version (樂觀鎖) 等
     @Column(name = "deleted", nullable = false)
     private Boolean deleted = false; // 默認為未刪除

     @Version
     private Long version; // 樂觀鎖
}
