package com.arcsoft.db_processor;

import com.arcsoft.db_annotation.IDBConditionBuilder;
import com.arcsoft.db_annotation.IDBProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * author : cy
 * date   : 2020-01-21 14:35
 * desc   :
 */
public class User_ConditionBuilder<T> implements IDBConditionBuilder {

    private StringBuilder selectionStr = new StringBuilder();
    private List values = new ArrayList<String>();
    private IDBProtocol protocol;


    public User_ConditionBuilder(IDBProtocol protocol){
        this.protocol = protocol;
    }

    public User_ConditionBuilder where(String[] selection){
        selectionStr.append(selection[0]);
        values.add(selection[1]);
        return this;
    }

    public User_ConditionBuilder and(String[] selection){
        selectionStr.append(" AND ");
        where(selection);
        return this;
    }

    public User_ConditionBuilder or(String[] selection){
        selectionStr.append(" OR ");
        where(selection);
        return this;
    }

    public List<T> query(){
        return protocol.queryByCondition(selectionStr.toString(), (String[]) values.toArray(new String[values.size()]));
    }

    public static class age{
        public static String[] is(String value){
            String[] selection = new String[2];
            selection[0] = "age=?";
            selection[1] = value;
            return selection;
        }
    }
}
