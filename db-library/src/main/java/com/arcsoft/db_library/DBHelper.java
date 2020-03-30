package com.arcsoft.db_library;

import android.content.Context;
import android.util.Log;

import com.arcsoft.db_annotation.DBConstant;
import com.arcsoft.db_annotation.IDBProtocol;
import com.arcsoft.db_annotation.IDBRegisterModel;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;


/**
 * author : cy
 * date   : 2019-10-23 18:55
 * desc   :
 */
public class DBHelper extends SQLiteOpenHelper {
    private Set<String> paths;
    private static final String TAG = "TestSQLite";
    private volatile static boolean debuggable = false;
    //数据库名字
    private static String DB_NAME = "test";
    //数据库密码
    public static String DB_PASSWORD = "123456";
    //数据库版本
    private static int DB_VERSION = 1;
    private static DBHelper instance;
    private SQLiteDatabase db;

    public static synchronized void openDebug() {
        debuggable = true;
    }

    public static void setConfig(String dbName, int dbVersion, String dbPwd){
        DB_NAME = dbName;
        DB_VERSION = dbVersion;
        DB_PASSWORD = dbPwd;
    }
    public static void init(Context context){
        if(null == instance){
            instance = new DBHelper(context);
        }
    }

    public static DBHelper getInstance(){
        return instance;
    }

    public static boolean debuggable() {
        return debuggable;
    }


    public synchronized  <T> IDBProtocol getDBProtocol(Class<T> model){
        try {
            String fullPath = DBConstant.MODEL_REGISTER_PKG + "." + model.getName() + DBConstant.PROCESSOR_CLASS_SUFFIX;
            IDBProtocol protocol = Warehouse.providers.get(fullPath);
            if(null == protocol){
                Class cursorClass = Class.forName(fullPath);
                protocol = (IDBProtocol) cursorClass.newInstance();
                protocol.init(getWritableDatabase());
                Warehouse.providers.put(fullPath, protocol);
                Log.i(TAG, "create protocol:" + protocol.hashCode());
            }
            Log.i(TAG, "getDBProtocol:" + protocol.hashCode());
            return protocol;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public IDBProtocol getDBProtocol(String path){
        try {
            Class cursorClass = Class.forName(DBConstant.MODEL_REGISTER_PKG + "." + path + DBConstant.PROCESSOR_CLASS_SUFFIX);
            IDBProtocol protocol = (IDBProtocol) cursorClass.newInstance();
            protocol.init(getWritableDatabase());
            return protocol;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private IDBRegisterModel getDBRegisterProtocol(String path){
        try {
            Class cursorClass = Class.forName(path);
            IDBRegisterModel protocol = (IDBRegisterModel) cursorClass.newInstance();
            return protocol;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();;
        }
        return null;
    }

    public DBHelper(Context context) {
        this(context, DB_NAME, null, DB_VERSION);
        this.paths = new HashSet<>();
        Set<String> routerMap = RegisterModelManager.getRegisterModelPaths(context);
        if(null != routerMap){
            for(String path : routerMap){
                IDBRegisterModel model = getDBRegisterProtocol(path);
                if(null != model){
                    String[] modelPaths = model.getPaths();
                    if(null != modelPaths){
                        for(String p : modelPaths){
                            this.paths.add(p);
                        }
                    }
                }
            }
        }

    }
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        SQLiteDatabase.loadLibs(context);

    }

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(DB_PASSWORD);
    }

    public SQLiteDatabase getWritableDatabase() {
        if(null == db){
            db = super.getWritableDatabase(DB_PASSWORD);
        }
        return db;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        for(String path : paths){
            getDBProtocol(path).create();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        this.db = db;
        for(String path : paths){
            getDBProtocol(path).updateTableColumn();
        }
    }
}
