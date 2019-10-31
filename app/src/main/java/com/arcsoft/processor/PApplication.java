package com.arcsoft.processor;

import android.app.Application;
import android.content.Context;

import com.arcsoft.db_library.DBHelper;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * author : cy
 * date   : 2019-10-28 16:45
 * desc   :
 */
public class PApplication extends Application {
    public static final int dbVersion = 2;
    private static SQLiteDatabase db;
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        DBHelper.init(this);

    }

    public static SQLiteDatabase getDB(){
        return db;
    }

    public static Context getContext(){
        return context;
    }
}
