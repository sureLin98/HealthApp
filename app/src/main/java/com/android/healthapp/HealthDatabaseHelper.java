package com.android.healthapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class HealthDatabaseHelper extends SQLiteOpenHelper {

    private final Context mContext;

    private final String CREATE_HEALTH_DATA="create table HealthData ("
            +"date integer primary key,"
            +"temperature real,"
            +"SpO2 integer,"
            +"heartRate integer,"
            +"Sp integer,"
            +"Dp integer,"
            +"Mc integer,"
            +"acdata blob)";

    public HealthDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_HEALTH_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
