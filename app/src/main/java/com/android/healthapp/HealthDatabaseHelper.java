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

    private final String CREATE_POSTURE_DATA="create table PostureData ("
            +"id integer primary key autoincrement,"
            +"ax real,"
            +"ay real,"
            +"az real)";

    private final String CREATE_PHONE_ACC_DATA="create table PhoneAccData ("
            +"id integer primary key,"
            +"ax real,"
            +"ay real,"
            +"az real)";

    public HealthDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext=context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_HEALTH_DATA);
        sqLiteDatabase.execSQL(CREATE_POSTURE_DATA);
        sqLiteDatabase.execSQL(CREATE_PHONE_ACC_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
