package com.android.healthapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.healthapp.ui.healthAssess.HealthAssessFragment;
import com.android.healthapp.ui.healthData.HealthDataFragment;
import com.android.healthapp.ui.remote.RemoteFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.android.healthapp.databinding.ActivityMainBinding;
import com.liyu.sqlitetoexcel.SQLiteToExcel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction,SensorEventListener {

    private ActivityMainBinding binding;
    private BLESPPUtils mBLESPPUtils;
    // 保存搜索到的设备，避免重复
    private ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    private DeviceDialogCtrl mDeviceDialogCtrl;

    private SensorManager sensorManager;

    private final String TAG="MainActivity";

    RemoteFragment.MQTTManager mqttManager;

    private int mStepDetector = 0;  // 自应用运行以来STEP_DETECTOR检测到的步数
    private int mStepCounter = 0;   // 自系统开机以来STEP_COUNTER检测到的步数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPermissions();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        sensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager!=null){
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                    SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE ),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESPPUtils.onDestroy();
    }

    @Override
    //TODO：姿态识别（手机）
    public void onSensorChanged(SensorEvent event) {
        // 传感器返回的数据 三轴加速度
        switch(event.sensor.getType()){
            case Sensor.TYPE_GYROSCOPE:
                float x=event.values[0];
                float y=event.values[1];
                float z=event.values[2];
                StringBuffer buffer = new StringBuffer();
                buffer.append("X：").append(String.format("%.2f", x)).append("\n");
                buffer.append("Y：").append(String.format("%.2f", y)).append("\n");
                buffer.append("Z：").append(String.format("%.2f", z)).append("\n");
                //Log.d(TAG, "onSensorChanged: \n"+buffer);

                break;

            case Sensor.TYPE_STEP_DETECTOR:
                if(event.values[0]==1.0f){
                    Log.d(TAG, "onSensorChanged: Detector");
                    HealthDataFragment.postureImageView.setImageResource(R.drawable.walking);
                }else{
                    HealthDataFragment.postureImageView.setImageResource(R.drawable.sitting);
                }

                break;

            case Sensor.TYPE_STEP_COUNTER:
                mStepCounter=(int) event.values[0];
                Log.d(TAG, "onSensorChanged: Counter="+mStepCounter);

                break;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE",
                            "android.permission.ACTIVITY_RECOGNITION"},
                    1
            );
        }
    }

    //保存健康数据
    public static void saveHealthData(Context context,DataPacket dataPacket){
        HealthDatabaseHelper healthDatabaseHelper= new HealthDatabaseHelper(context,"HealthDatabase",null,1);
        SQLiteDatabase database=healthDatabaseHelper.getWritableDatabase();
        ContentValues contentValues=new ContentValues();
        contentValues.put("date",dataPacket.timeStamp);
        contentValues.put("temperature",dataPacket.temp);
        contentValues.put("SpO2",dataPacket.spo2);
        contentValues.put("heartRate",dataPacket.heartRate);
        contentValues.put("Sp",dataPacket.Sp);
        contentValues.put("Dp",dataPacket.Dp);
        contentValues.put("Mc",dataPacket.Mc);
        contentValues.put("acdata",dataPacket.acdata);
        database.insert("HealthData",null,contentValues);
        database.close();
    }

    //保存姿态数据
    public static void savePostureData(Context context,List<Float> yawList,List<Float> pitchList,List<Float> rollList){
        HealthDatabaseHelper healthDatabaseHelper=new HealthDatabaseHelper(context,"HealthDatabase",null,1);
        SQLiteDatabase database=healthDatabaseHelper.getWritableDatabase();
        ContentValues contentValues=new ContentValues();
        for(int i=0;i<yawList.size();i++){
            contentValues.put("ax",yawList.get(i));
            contentValues.put("ay",pitchList.get(i));
            contentValues.put("az",rollList.get(i));
            database.insert("PostureData",null,contentValues);
            contentValues.clear();
        }
        database.close();
    }

    public List<DataPacket> readHealthData() throws InterruptedException {
        List<DataPacket> list=new ArrayList<>();
        HealthDatabaseHelper healthDatabaseHelper=new HealthDatabaseHelper(this,"HealthDatabase",null,1);
        SQLiteDatabase database=healthDatabaseHelper.getReadableDatabase();
        Cursor cursor=database.query("HealthData",null,null,null,null,null,null);
        if(cursor.moveToFirst()){
            do{
                DataPacket dataPacket=new DataPacket();
                dataPacket.timeStamp=cursor.getLong(0);
                dataPacket.temp=cursor.getFloat(1);
                dataPacket.spo2=cursor.getInt(2);
                dataPacket.heartRate=cursor.getInt(3);
                dataPacket.Sp=cursor.getInt(4);
                dataPacket.Dp=cursor.getInt(5);
                dataPacket.Mc=cursor.getInt(6);
                dataPacket.acdata=cursor.getBlob(7);
                list.add(dataPacket);

                HealthDataFragment.onDataUpdate(dataPacket);

            }while(cursor.moveToNext());
        }
        cursor.close();
        database.close();
        return list;
    }

    /**   蓝牙串口工具类接口实现     **/

    @Override
    public void onFoundDevice(BluetoothDevice device) {
        // 判断是不是重复的
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // 添加，下次有就不显示了
        mDevicesList.add(device);
        // 添加条目到 UI 并设置点击事件
        mDeviceDialogCtrl.addDevice(device, new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
                mBLESPPUtils.connect(clickDevice);
            }
        });
    }

    @Override
    public void onConnectSuccess(final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,"连接到"+device.getName(),Toast.LENGTH_LONG).show();
            }
        });
        //保存已连接的蓝牙设备的MAC地址
        SharedPreferences.Editor editor=getSharedPreferences("data",MODE_PRIVATE).edit();
        editor.putString("mac",device.getAddress());
        editor.commit();
        HealthDataFragment.enableBtDetect();
        if(mDeviceDialogCtrl!=null) mDeviceDialogCtrl.dismiss();
        HealthDataFragment.setStateText("蓝牙监控");
        //启动MQTT
        mqttManager=new RemoteFragment.MQTTManager(getApplicationContext());
        mqttManager.buildClient();

    }

    @Override
    public void onConnectFailed(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
        if(mDeviceDialogCtrl!=null) mDeviceDialogCtrl.dismiss();
    }

    @Override
    public void onReceiveBytes(final byte[] bytes) {
        String dataStr = new String(bytes);
        if(!dataStr.equals("")){
            DataPacket dataPacket=new DataPacket(dataStr);
            if(dataPacket.state==1) saveHealthData(getApplicationContext(),dataPacket);
            if(HealthDataFragment.isBtDetect()==1 && mqttManager.isOK==true){
                mqttManager.sendMQTT("data",dataStr);
            }
            Log.d(TAG, "onReceiveBytes: "+dataStr);
            //姿态识别
            HealthDataFragment.poseIdentify(getApplicationContext(),dataStr.split("#")[3].split(" "),dataStr.split("#")[4].split(" "),dataStr.split("#")[5].split(" "));
            //更新健康数据
            HealthDataFragment.onDataUpdate(dataPacket);
            //健康评估
            HealthAssessFragment.healthAssess(dataPacket);
        }

    }

    @Override
    public void onSendBytes(final byte[] bytes) {

    }

    @Override
    public void onFinishFoundDevice() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.titke_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_bt:
                // 初始化
                mBLESPPUtils = new BLESPPUtils(this, this);
                // 启用日志输出
                mBLESPPUtils.enableBluetooth();
                // 设置接收停止标志位字符串
                mBLESPPUtils.setStopString("\r\n");
                // 用户没有开启蓝牙的话打开蓝牙
                if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
                // 启动工具类
                mBLESPPUtils.onCreate();
                //蓝牙连接对话框显示
                mDeviceDialogCtrl = new DeviceDialogCtrl(this);
                mDeviceDialogCtrl.show();

                break;

            case R.id.connected_bt:
                // 初始化
                mBLESPPUtils = new BLESPPUtils(this, this);
                // 启用日志输出
                mBLESPPUtils.enableBluetooth();
                // 设置接收停止标志位字符串
                mBLESPPUtils.setStopString("\r\n");
                // 用户没有开启蓝牙的话打开蓝牙
                if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
                // 启动工具类
                mBLESPPUtils.onCreate();
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                mBluetoothAdapter.cancelDiscovery();
                //获取已配对过的蓝牙设备的MAC地址
                SharedPreferences sharedPreferences=getSharedPreferences("data",MODE_PRIVATE);
                String mac=sharedPreferences.getString("mac"," ");
                if(mac==" ") Toast.makeText(this,"本地没有已配对的蓝牙设备",Toast.LENGTH_LONG).show();
                else mBLESPPUtils.connect(mac);

                break;

            case R.id.export_health_data:

                new SQLiteToExcel
                        .Builder(this)
                        .setDataBase(this.getDatabasePath("HealthDatabase").getAbsolutePath())
                        .setTables("HealthData")
                        .setOutputFileName("HealthData.xls")
                        .start(new SQLiteToExcel.ExportListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onCompleted(String s) {
                                Toast.makeText(MainActivity.this,"数据导出成功！保存路径："+s,Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(MainActivity.this,"数据导出失败！",Toast.LENGTH_LONG).show();
                            }
                        });
                break;

            case R.id.export_posture_data:

                new SQLiteToExcel
                        .Builder(this)
                        .setDataBase(this.getDatabasePath("HealthDatabase").getAbsolutePath())
                        .setTables("PostureData")
                        .setOutputFileName("PostureData.xls")
                        .start(new SQLiteToExcel.ExportListener() {
                            @Override
                            public void onStart() {

                            }

                            @Override
                            public void onCompleted(String s) {
                                Toast.makeText(MainActivity.this,"数据导出成功！保存路径："+s,Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(MainActivity.this,"数据导出失败！",Toast.LENGTH_LONG).show();
                            }
                        });
                break;

            case R.id.phone_accel:
                startActivity(new Intent(this,PhoneAccelActivity.class));
                break;

            case R.id.health_web:
                startActivity(new Intent(this,HealthWebActivity.class));
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * 蓝牙设备选择对话框控制
     */
    private class DeviceDialogCtrl {
        private LinearLayout mDialogRootView;
        private ProgressBar mProgressBar;
        private AlertDialog mConnectDeviceDialog;

        /**
         * 初始化，设置基本布局
         * @param context
         */
        DeviceDialogCtrl(Context context) {
            // 搜索进度条
            mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            mProgressBar.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            50
                    )
            );

            // 根布局
            mDialogRootView = new LinearLayout(context);
            mDialogRootView.setOrientation(LinearLayout.VERTICAL);
            mDialogRootView.addView(mProgressBar);
            mDialogRootView.setMinimumHeight(700);

            // 容器布局
            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(mDialogRootView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            700
                    )
            );

            // 构建对话框
            mConnectDeviceDialog = new AlertDialog
                    .Builder(context)
                    .setNegativeButton("刷新", null)
                    .setPositiveButton("取消", null)
                    .create();
            mConnectDeviceDialog.setTitle("选择连接的蓝牙设备");
            mConnectDeviceDialog.setView(scrollView);
            mConnectDeviceDialog.setCancelable(false);
        }

        /**
         * 显示并开始搜索设备
         */
        void show() {
            mBLESPPUtils.startDiscovery();
            mConnectDeviceDialog.show();
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    return false;
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    //finish();
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialogRootView.removeAllViews();
                    mDialogRootView.addView(mProgressBar);
                    mDevicesList.clear();
                    mBLESPPUtils.startDiscovery();
                }
            });
        }

        /**
         * 取消对话框
         */
        void dismiss() {
            mConnectDeviceDialog.dismiss();
        }

        /**
         * 添加一个设备到列表
         * @param device 设备
         * @param onClickListener 点击回调
         */
        private void addDevice(final BluetoothDevice device, final View.OnClickListener onClickListener) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView devTag = new TextView(MainActivity.this);
                    devTag.setClickable(true);
                    devTag.setPadding(20,20,20,20);
                    devTag.setBackgroundResource(R.drawable.rect_round_button_ripple);
                    devTag.setText(device.getName() + "\nMAC:" + device.getAddress());
                    devTag.setTextColor(Color.WHITE);
                    devTag.setOnClickListener(onClickListener);
                    devTag.setTag(device);
                    devTag.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    );
                    ((LinearLayout.LayoutParams) devTag.getLayoutParams()).setMargins(
                            20, 20, 20, 20);
                    mDialogRootView.addView(devTag);
                }
            });
        }
    }

    /**
     * 数据包类
     */
    public static class DataPacket {

        //数据字符串
        private String data;
        //时间戳
        public long timeStamp=0;
        //数据状态 1有效 0无效
        public int state=0;
        //体温
        public float temp=0;
        //血氧
        public int spo2=0;
        //心率
        public int heartRate=0;
        //收缩压
        public int Sp=0;
        //舒张压
        public int Dp=0;
        //微循环
        public int Mc=0;
        //脉搏波数据
        public byte[] acdata=new byte[64];

        public DataPacket(String dataStr){
            data=dataStr;
            timeStamp=System.currentTimeMillis();
            unpack();
        }

        public DataPacket(){
            data=null;
        }

        private void unpack(){
            String[] str=data.split("#");
            state=Integer.valueOf(str[0]).intValue();
            String[] data1=str[1].split(" ");
            String[] data2=str[2].split(" ");
            temp=Float.valueOf(data1[0]).floatValue();
            spo2=Integer.valueOf(data1[1]).intValue();
            heartRate=Integer.valueOf(data1[2]).intValue();
            Sp=Integer.valueOf(data1[3]).intValue();
            Dp=Integer.valueOf(data1[4]).intValue();
            Mc=Integer.valueOf(data1[5]).intValue();
            for(int i=0;i<64;i++){
                acdata[i]=Byte.valueOf(data2[i]).byteValue();
            }
        }
    }
}