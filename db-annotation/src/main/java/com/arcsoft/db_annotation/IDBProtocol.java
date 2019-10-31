package com.arcsoft.db_annotation;

import java.util.List;

/**
 * author : cy
 * date   : 2019-10-29 09:39
 * desc   :
 */
public interface IDBProtocol<S,T> {
    void init(S db);
    void create();
    long insert(T model);
    long insertOrUpdate(T model);

    /**
     * 批量插入事务
     * @param models
     */
    void insertOrUpdates(List<T> models);
    List<T> queryAll();
    int delete(String whereClauses, String[] whereArgs);

    /**
     * 以主键为删除依据
     * @param whereArgs
     * @return
     */
    int deleteByPrimary(String whereArgs);

    /**
     * 默认数据库升级操作，仅支持表新增列，新增表
     */
    void updateTableColumn();

}
