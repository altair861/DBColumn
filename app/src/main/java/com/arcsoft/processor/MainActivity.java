package com.arcsoft.processor;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


import com.arcsoft.db_library.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HashMap<String, String> map = new HashMap<>();
        map.put("1", "123");
        map.put("2", "124");
        map.put("3", "125");

        //DBHelper dbHelper = new DBHelper(this,"stu_db",null,dbVersion);
//        DBHelper dbHelper = new DBHelper(this,"stu_db",null,PApplication.dbVersion);
//        SQLiteDatabase db =dbHelper.getReadableDatabase();

        findViewById(R.id.btnTest).setOnClickListener(v -> test());

        //DBMananger.getDBProtocol(User.class).create(DBCipherManager.getInstance(MainActivity.this).getDB());


//        User$DBAdapter adapter = new User$DBAdapter();
//        adapter.updateTableColumn();

        String addColumn = String.format("alter table User add %s %s", "aaa", "bbbb");
    }

    private void test(){
        Log.i("huangxiaoguo", "insert");
        List<User> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setId(i);
            user.setName("name:"+ i);
            user.setNum("num:" + i);
            //user.setTest2("test"+ PApplication.dbVersion);
            //DBHelper.getInstance().getDBProtocol(User.class).insertOrUpdate(user);
            list.add(user);
        }
        DBHelper.getInstance().getDBProtocol(User.class).insertOrUpdates(list);
        Log.i("huangxiaoguo", "insert end");

        list =  DBHelper.getInstance().getDBProtocol(User.class).queryAll();
        Log.i("huangxiaoguo", "queryAll result:" + list.size());
    }
}
