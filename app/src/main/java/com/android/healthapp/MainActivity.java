package com.android.healthapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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

import com.android.healthapp.ui.healthData.HealthDataFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.android.healthapp.databinding.ActivityMainBinding;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,BLESPPUtils.OnBluetoothAction {

    private ActivityMainBinding binding;
    private BLESPPUtils mBLESPPUtils;
    // 保存搜索到的设备，避免重复
    private ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    private DeviceDialogCtrl mDeviceDialogCtrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPermissions();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESPPUtils.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

        }
    }

    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE"},
                    1
            );
        }
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
        if(mDeviceDialogCtrl!=null) mDeviceDialogCtrl.dismiss();
    }

    @Override
    public void onConnectFailed(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
        mDeviceDialogCtrl.dismiss();
    }

    @Override
    public void onReceiveBytes(final byte[] bytes) {
        String dataStr = new String(bytes);
        DataPacket dataPacket=new DataPacket(dataStr);
        Log.d("data", dataStr);
        HealthDataFragment.onDataUpdate(dataPacket);
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

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * 设备选择对话框控制
     */
    private class DeviceDialogCtrl {
        private LinearLayout mDialogRootView;
        private ProgressBar mProgressBar;
        private AlertDialog mConnectDeviceDialog;

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
    public class DataPacket{

        //数据字符串
        private String data;
        //体温
        public float temp;
        //血氧
        public int spo2;
        //心率
        public int heartRate;
        //收缩压
        public int Sp;
        //舒张压
        public int Dp;
        //微循环
        public int Mc;
        //脉搏波数据
        public short[] acdata=new short[64];

        DataPacket(String dataStr){
            data=dataStr;
            Unpack();
        }

        private void Unpack(){
            String[] str=data.split("#");
            String[] data1=str[0].split(" ");
            String[] data2=str[1].split(" ");
            temp=Float.valueOf(data1[0]).floatValue();
            spo2=Integer.valueOf(data1[1]).intValue();
            heartRate=Integer.valueOf(data1[2]).intValue();
            Sp=Integer.valueOf(data1[3]).intValue();
            Dp=Integer.valueOf(data1[4]).intValue();
            Mc=Integer.valueOf(data1[5]).intValue();
            for(int i=0;i<64;i++){
                acdata[i]=Short.valueOf(data2[i]).shortValue();
            }
        }

    }
}