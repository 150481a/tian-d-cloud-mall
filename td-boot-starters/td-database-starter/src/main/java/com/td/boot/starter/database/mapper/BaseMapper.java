package com.td.boot.starter.database.mapper;

/**
 * 通用 MyBatis Mapper 接口。
 * 作為所有業務 Mapper 的基類。
 *
 * @param <T> 實體類型
 */
public interface BaseMapper<T> {
    // 這裡可以定義一些通用的 CRUD 方法，如果沒有使用第三方通用 Mapper 框架
    // 例如：
    // int insert(T entity);
    // int deleteById(Long id);
    // int updateById(T entity);
    // T selectById(Long id);
    // List<T> selectAll();
    // long count();
}
