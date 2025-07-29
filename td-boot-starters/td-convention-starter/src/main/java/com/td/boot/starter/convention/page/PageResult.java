package com.td.boot.starter.convention.page;


import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分頁查詢結果的統一返回格式。
 * 用於封裝列表數據的分頁信息。
 *
 * @param <T> 列表中元素的類型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 當前頁碼 (從 1 開始)。
     */
    private long pageNum;

    /**
     * 每頁記錄數。
     */
    private long pageSize;

    /**
     * 總記錄數。
     */
    private long total;

    /**
     * 總頁數。
     */
    private long pages;

    /**
     * 當前頁的數據列表。
     */
    private List<T> records;

    // 私有構造函數，強制使用靜態工廠方法或 Builder
    private PageResult() {
    }

    /**
     * 靜態工廠方法，從現有數據構建 PageResult。
     * 通常用於從分頁查詢框架（如 MyBatis-Plus 的 Page 類或 Spring Data JPA 的 Page 接口）轉換。
     *
     * @param pageNum   當前頁碼
     * @param pageSize  每頁記錄數
     * @param total     總記錄數
     * @param records   當前頁的數據列表
     * @param <T>       數據類型
     * @return PageResult 實例
     */
    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.pageNum = pageNum;
        pageResult.pageSize = pageSize;
        pageResult.total = total;
        pageResult.records = records != null ? records : Collections.emptyList(); // 避免 null
        pageResult.pages = (total + pageSize - 1) / pageSize; // 計算總頁數，向上取整
        return pageResult;
    }

    /**
     * 創建一個空的分頁結果。
     *
     * @param pageNum   當前頁碼
     * @param pageSize  每頁記錄數
     * @param <T>       數據類型
     * @return 空的 PageResult 實例
     */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(pageNum, pageSize, 0, Collections.emptyList());
    }



    // 可選：Builder 模式，提供更靈活的構建方式
    public static class Builder<T> {
        private long pageNum;
        private long pageSize;
        private long total;
        private List<T> records;

        public Builder<T> pageNum(long pageNum) {
            this.pageNum = pageNum;
            return this;
        }

        public Builder<T> pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> total(long total) {
            this.total = total;
            return this;
        }

        public Builder<T> records(List<T> records) {
            this.records = records;
            return this;
        }

        public PageResult<T> build() {
            PageResult<T> pageResult = new PageResult<>();
            pageResult.pageNum = this.pageNum;
            pageResult.pageSize = this.pageSize;
            pageResult.total = this.total;
            pageResult.records = this.records != null ? this.records : Collections.emptyList();
            pageResult.pages = (this.total + this.pageSize - 1) / this.pageSize;
            return pageResult;
        }
    }
}
