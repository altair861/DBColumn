package com.arcsoft.db_annotation;

import java.util.List;

/**
 * author : cy
 * date   : 2020-01-21 15:32
 * desc   :
 */
public interface IDBConditionBuilder<T> {
    IDBConditionBuilder where(String[] selection);

    IDBConditionBuilder and(String[] selection);

    IDBConditionBuilder or(String[] selection);

    List<T> query();
}
