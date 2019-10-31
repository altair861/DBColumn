package com.arcsoft.processor;

import com.arcsoft.db_annotation.DBColumn;

/**
 * author : cy
 * date   : 2019-10-29 11:23
 * desc   :
 */
public class Student {
    @DBColumn(primary = true)
    private int id;
    @DBColumn
    private String name;
    @DBColumn
    private String age;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }
}
