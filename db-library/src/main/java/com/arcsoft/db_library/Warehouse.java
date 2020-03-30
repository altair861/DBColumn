package com.arcsoft.db_library;

import com.arcsoft.db_annotation.IDBProtocol;

import java.util.HashMap;
import java.util.Map;

/**
 * author : cy
 * date   : 2020-03-30 14:40
 * desc   :
 */
class Warehouse {
    // Cache provider
    static Map<String, IDBProtocol> providers = new HashMap<>();
}
