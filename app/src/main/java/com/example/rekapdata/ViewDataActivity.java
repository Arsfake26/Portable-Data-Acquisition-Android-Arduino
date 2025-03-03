package com.example.rekapdata;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ViewDataActivity extends AppCompatActivity {

    private HelperDatabase databaseHelper;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        listView = findViewById(R.id.listView);
        databaseHelper = new HelperDatabase(this);
        dataList = new ArrayList<>();

        loadDataFromDatabase();
    }

    private void loadDataFromDatabase() {
        Cursor cursor = databaseHelper.getAllData();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String msgCounter = cursor.getString(1);
                String data1 = cursor.getString(2);
                String data2 = cursor.getString(3);
                String data3 = cursor.getString(4);
                String data4 = cursor.getString(5);
                String data5 = cursor.getString(6);
                String data6 = cursor.getString(7);
                String data7 = cursor.getString(8);
                String data8 = cursor.getString(9);

                String displayData = msgCounter +
                        ";" + data1 + ";" + data2 + ";" + data3 + ";" + data4 + ";" +
                        data5 + ";" + data6 + ";" + data7 + ";" + data8;
                dataList.add(displayData);
            } while (cursor.moveToNext());

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
            listView.setAdapter(adapter);
        } else {
            Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show();
        }
    }
}
