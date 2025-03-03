package com.example.rekapdata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HelperDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DataReceived.db";
    private static final int DATABASE_VERSION = 3; // Increment this if you change the schema

    private static final String TABLE_NAME = "data_table";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_MESSAGE_COUNTER = "msgCounter";
    public static final String COLUMN_DATA1 = "data1";
    public static final String COLUMN_DATA2 = "data2";
    public static final String COLUMN_DATA3 = "data3";
    public static final String COLUMN_DATA4 = "data4";
    public static final String COLUMN_DATA5 = "data5";
    public static final String COLUMN_DATA6 = "data6";
    public static final String COLUMN_DATA7 = "data7";
    public static final String COLUMN_DATA8 = "data8";

    public HelperDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_MESSAGE_COUNTER + " TEXT, " +
                COLUMN_DATA1 + " TEXT, " +
                COLUMN_DATA2 + " TEXT, " +
                COLUMN_DATA3 + " TEXT, " +
                COLUMN_DATA4 + " TEXT, " +
                COLUMN_DATA5 + " TEXT, " +
                COLUMN_DATA6 + " TEXT, " +
                COLUMN_DATA7 + " TEXT, " +
                COLUMN_DATA8 + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String msgCounter, String data1, String data2, String data3, String data4, String data5, String data6, String data7, String data8) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_MESSAGE_COUNTER, msgCounter);
        contentValues.put(COLUMN_DATA1, data1);
        contentValues.put(COLUMN_DATA2, data2);
        contentValues.put(COLUMN_DATA3, data3);
        contentValues.put(COLUMN_DATA4, data4);
        contentValues.put(COLUMN_DATA5, data5);
        contentValues.put(COLUMN_DATA6, data6);
        contentValues.put(COLUMN_DATA7, data7);
        contentValues.put(COLUMN_DATA8, data8);

        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public boolean clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_NAME, null, null);
        db.close();
        return rowsDeleted > 0; // Returns true if rows were deleted
    }
    public boolean clearAllDataAndResetID() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + TABLE_NAME + "'");
        db.close();
        return false;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public Cursor getLatestEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Get the latest entries, sorted by ID in descending order to get the most recent entries
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC LIMIT 100", null);
    }
}
