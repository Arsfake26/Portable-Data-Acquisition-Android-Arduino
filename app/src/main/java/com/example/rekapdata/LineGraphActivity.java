package com.example.rekapdata;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineGraphActivity extends AppCompatActivity {

    private ScatterChart[] scatterCharts = new ScatterChart[8];
    private HelperDatabase databaseHelper;
    private final Handler handler = new Handler();
    private final int updateInterval = 2000; // Update every 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        databaseHelper = new HelperDatabase(this);

        scatterCharts[0] = findViewById(R.id.scatterChart1); // data1
        scatterCharts[1] = findViewById(R.id.scatterChart2); // data2
        scatterCharts[2] = findViewById(R.id.scatterChart3); // data3
        scatterCharts[3] = findViewById(R.id.scatterChart4); // data4
        scatterCharts[4] = findViewById(R.id.scatterChart5); // data5
        scatterCharts[5] = findViewById(R.id.scatterChart6); // data6
        scatterCharts[6] = findViewById(R.id.scatterChart7); // data7
        scatterCharts[7] = findViewById(R.id.scatterChart8); // data8

        // Initial display
        displayData();

        // Start periodic updates
        handler.postDelayed(updateRunnable, updateInterval);
    }

    private void displayData() {
        List<Entry>[] entriesList = new List[8];
        for (int i = 0; i < 8; i++) {
            entriesList[i] = new ArrayList<>();
        }

        Cursor cursor = null;
        try {
            cursor = databaseHelper.getLatestEntries();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int messageCounterIndex = cursor.getColumnIndex(HelperDatabase.COLUMN_MESSAGE_COUNTER);
                    int[] dataIndexes = new int[8];
                    for (int i = 0; i < 8; i++) {
                        dataIndexes[i] = cursor.getColumnIndex("data" + (i + 1));
                    }

                    if (messageCounterIndex != -1) {
                        int msgCounter = Integer.parseInt(cursor.getString(messageCounterIndex).replace("#", ""));

                        for (int i = 0; i < 8; i++) {
                            if (dataIndexes[i] != -1) {
                                int value = cursor.getInt(dataIndexes[i]);
                                entriesList[i].add(new Entry(msgCounter, value));
                            }
                        }
                    }
                } while (cursor.moveToNext());

                for (List<Entry> entries : entriesList) {
                    Collections.reverse(entries);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY, Color.LTGRAY};

        for (int i = 0; i < 8; i++) {
            scatterCharts[i].clear();
            if (!entriesList[i].isEmpty()) {
                ScatterDataSet scatterDataSet = new ScatterDataSet(entriesList[i], "Data" + (i + 1));
                scatterDataSet.setColor(colors[i]);
                scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
                scatterDataSet.setScatterShapeSize(8f);

                ScatterData scatterData = new ScatterData(scatterDataSet);
                scatterCharts[i].setData(scatterData);
            }
            scatterCharts[i].invalidate();
        }
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            displayData();
            handler.postDelayed(this, updateInterval);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable); // Stop updates when activity is destroyed
    }
}
