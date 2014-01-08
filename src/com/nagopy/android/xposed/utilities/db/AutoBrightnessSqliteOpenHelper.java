
package com.nagopy.android.xposed.utilities.db;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AutoBrightnessSqliteOpenHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "brightness.db";
    private static final int DATABASE_VERSION = 2;

    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
            Locale.getDefault());

    public AutoBrightnessSqliteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE brightness(id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "lux real NOT NULL, brightness integer NOT NULL, insert_date TEXT not null);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO
        db.execSQL("drop table brightness;");
        onCreate(db);
    }

    /**
     * センサー値、輝度を保存する.
     * 
     * @param lux
     * @param brightness
     */
    public void insert(float lux, int brightness) {
        SQLiteDatabase db = getWritableDatabase();
        String format = "INSERT INTO brightness(lux, brightness, insert_date) VALUES('%f', '%d', '%s');";
        db.execSQL(String.format(format, lux, brightness, getDate()));
        db.close();
    }

    public String getDate() {
        Calendar calendar = Calendar.getInstance();
        return mSimpleDateFormat.format(calendar.getTime());
    }
}
