package com.td.boot.starter.convention.page;

import lombok.Data;

import java.io.Serializable;

/**
 * 分頁查詢請求的統一封裝。
 * 用於接收前端或調用者傳來的分頁參數。
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 當前頁碼，從 1 開始。
     * 默認為 1。
     */
    private long pageNum = 1;

    /**
     * 每頁記錄數。
     * 默認為 10，可根據實際業務調整最大值。
     */
    private long pageSize = 10;

    /**
     * 排序字段（可選）。
     * 例如："createTime", "price", "sales"。
     */
    private String sortBy;

    /**
     * 排序順序（可選）。
     * "asc" 或 "desc"。
     */
    private String sortOrder; // 建議使用枚舉來限制合法值，例如 SortOrderEnum.ASC/DESC

    // --- 構造函數 ---
    public PageQuery() {
    }

    public PageQuery(long pageNum, long pageSize) {
        setPageNum(pageNum);
        setPageSize(pageSize);
    }

    public PageQuery(long pageNum, long pageSize, String sortBy, String sortOrder) {
        setPageNum(pageNum);
        setPageSize(pageSize);
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
    }

    // --- Getter 和 Setter 方法 ---

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        if (pageNum < 1) {
            this.pageNum = 1; // 確保頁碼至少為 1
        } else {
            this.pageNum = pageNum;
        }
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        if (pageSize < 1) {
            this.pageSize = 10; // 每頁記錄數最小為 10 (可調)
        } else if (pageSize > 100) { // 設置最大值，防止一次性查詢過多數據
            this.pageSize = 100; // 每頁記錄數最大為 100 (可調)
        } else {
            this.pageSize = pageSize;
        }
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        // 標準化為小寫，方便後端判斷，或可在設置時即進行校驗
        if (sortOrder != null) {
            String lowerCaseOrder = sortOrder.toLowerCase();
            if ("asc".equals(lowerCaseOrder) || "desc".equals(lowerCaseOrder)) {
                return lowerCaseOrder;
            }
        }
        return null; // 如果不合法或為空，返回 null
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
