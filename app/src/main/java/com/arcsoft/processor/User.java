package com.arcsoft.processor;


import com.arcsoft.db_annotation.DBColumn;

/**
 * author : cy
 * date   : 2019-10-24 16:53
 * desc   :
 */
public class User extends BaseModel{
    @DBColumn(primary = true)
    private int id;
    @DBColumn
    String name;
    @DBColumn
    private String num;
    @DBColumn
    private boolean test;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getNum() {
        return num;
    }
    public void setNum(String num) {
        this.num = num;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
