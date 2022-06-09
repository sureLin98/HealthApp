package com.android.healthapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.healthapp.fft.ComplexNumberArray;
import com.android.healthapp.fft.Fourier;
import com.liyu.sqlitetoexcel.SQLiteToExcel;

public class PhoneAccelActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    TextView textView;
    Button buttonStart,buttonStop,buttonClear,buttonFFT;
    SensorManager sensorManager;
    Sensor accelerometer;
    HealthDatabaseHelper healthDatabaseHelper;
    SQLiteDatabase database;
    int id;
    int index;
    float[] data=new float[150];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_accel);

        id=1;
        index=0;

        textView=findViewById(R.id.text);
        buttonStart=findViewById(R.id.start);
        buttonStop=findViewById(R.id.stop);
        buttonClear=findViewById(R.id.clear);
        buttonFFT=findViewById(R.id.fft_test);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        buttonClear.setOnClickListener(this);
        buttonFFT.setOnClickListener(this);
        buttonStop.setEnabled(false);

        sensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        healthDatabaseHelper=new HealthDatabaseHelper(PhoneAccelActivity.this,"HealthDatabase",null,1);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.start:
                sensorManager.registerListener(this,accelerometer,sensorManager.SENSOR_DELAY_UI);
                textView.setText("开始记录");
                buttonStart.setEnabled(false);
                buttonStop.setEnabled(true);
                buttonClear.setEnabled(false);
                database=healthDatabaseHelper.getWritableDatabase();
                break;

            case R.id.stop:
                sensorManager.unregisterListener(this);
                textView.setText("停止记录");
                buttonStart.setEnabled(true);
                buttonStop.setEnabled(false);
                buttonClear.setEnabled(true);
                database.close();
                //数据导出
                new SQLiteToExcel
                        .Builder(this)
                        .setDataBase(this.getDatabasePath("HealthDatabase").getAbsolutePath())
                        .setTables("PhoneAccData")
                        .setOutputFileName("PhoneAccData.xls")
                        .start(new SQLiteToExcel.ExportListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onCompleted(String s) {
                                textView.setText("数据导出成功！保存路径："+s);
                            }

                            @Override
                            public void onError(Exception e) {
                                textView.setText("数据导出失败！");
                            }
                        });
                break;

            case R.id.clear:
                healthDatabaseHelper.getWritableDatabase().execSQL("DELETE FROM PhoneAccData");
                textView.setText("数据库清除成功！");
                id=1;
                break;

            case R.id.fft_test:

                break;

            default:
                break;
        }
    }

    private void fftExample() {
        final String TAG="FFT";
        String res="";
        //时域信号
        //float[] data = {0,1,2,3,4,5,6,7,0,1,2,3,4,5,6,7,0,1,2,3,4,5,6,7,0,1,2,3,4,5,6,7};
        //采样点100，采样频率17Hz
        Fourier fft = new Fourier(100,16);
        //快速傅里叶变换
        ComplexNumberArray cna = fft.fft(data);
        //输出结果
        for(int i=0;i<100;i++){
           res=res+"\t"+cna.toString(i);
        }
        Log.d(TAG, "fftExample: result="+res);
        //用于对快速傅里叶变换的结果进行分析
        Fourier.Analyzer fftAnalyzer  = new Fourier.Analyzer(fft);
        //输出最大幅值处的频率
        Log.d(TAG, "fftExample: FrequencyAtMaxAmplitude="+fftAnalyzer.getFrequencyAtMaxAmplitude(cna));
        textView.setText("FrequencyAtMaxAmplitude="+fftAnalyzer.getFrequencyAtMaxAmplitude(cna));

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values=sensorEvent.values;
        ContentValues contentValues=new ContentValues();
        contentValues.put("id",id++);
        contentValues.put("ax",values[0]);
        contentValues.put("ay",values[1]);
        contentValues.put("az",values[2]);
        database.insert("PhoneAccData",null,contentValues);
        textView.setText("x="+values[0]+"\ny="+values[1]+"\nz="+values[2]);

        /*
        if(index<100){
            data[index++]=values[0];
        }else {
            fftExample();
            index=0;
        }
        */

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}