package com.android.healthapp.ui.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.healthapp.MainActivity;
import com.android.healthapp.R;
import com.android.healthapp.databinding.FragmentRemoteBinding;
import com.android.healthapp.ui.healthData.HealthDataFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RemoteFragment extends Fragment {

    private static final String TAG = "RemoteFragment";

    public static String remoteUserName="";

    public static int remoteDetect=0;

    static EditText editText;
    static Button button;

    MQTTManager mqttManager;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_remote,container,false);

        editText=view.findViewById(R.id.user_name);
        button=view.findViewById(R.id.remote_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableRemoteDetect();
                mqttManager=new MQTTManager(getContext());
                mqttManager.buildClient();
                remoteUserName=editText.getText().toString();
                if(remoteUserName.length()<1) remoteUserName=Build.MODEL;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mqttManager.isOK==false);
                        while(true){
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            mqttManager.sendMQTT("info",remoteUserName);
                        }

                    }
                }).start();
            }
        });

        if(!remoteUserName.equals("")){
            if(remoteDetect==1){
                editText.setText("正在进行远程监控");
            }
            if(HealthDataFragment.btDetect==1){
                editText.setText(remoteUserName+"正在进行远程监控");
            }
            editText.setEnabled(false);
            button.setEnabled(false);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public static int isRemoteDetect(){
        return remoteDetect;
    }

    public static void enableRemoteDetect(){
        remoteDetect=1;
    }

    public static void disableRemoteDetect(){
        remoteDetect=0;
    }

    public static class MQTTManager {

        public  Context mContext;
        private MqttAndroidClient mqttAndroidClient;
        private String clientId="d6722eed3eb44c5dbdc4ab78aed5c826";//自定义

        private MqttConnectOptions mqttConnectOptions;

        private ScheduledExecutorService reconnectPool;//重连线程池

        public boolean isOK=false;

        public MQTTManager(Context context){
            mContext=context;
        }

        public void buildClient() {
            closeMQTT();//先关闭上一个连接

            buildMQTTClient();
        }

        private IMqttActionListener iMqttActionListener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                TVLog("connect-"+"onSuccess");
                closeReconnectTask();
                subscribeToTopic();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                //connect-onFailure-MqttException (0) - java.net.UnknownHostException
                TVLog("connect-"+ "onFailure-"+exception);
                Log.e(TAG, "onFailure: ",exception);
                startReconnectTask();
            }
        };

        private MqttCallback mqttCallback = new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                //close-connectionLost-等待来自服务器的响应时超时 (32000)
                //close-connectionLost-已断开连接 (32109)
                TVLog("close-"+"connectionLost-"+cause);
                if (cause != null) {//null表示被关闭
                    startReconnectTask();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String data = new String(message.getPayload());
                if(RemoteFragment.isRemoteDetect()==1 && topic.equals("data")){
                    MainActivity.DataPacket dataPacket=new MainActivity.DataPacket(data);
                    if(dataPacket.state==1) MainActivity.saveHealthData(mContext, dataPacket);
                    HealthDataFragment.onDataUpdate(dataPacket);
                    TVLog("messageArrived-"+message.getId()+"-"+data);
                }
                if(topic.equals("info")){
                    Log.d(TAG, "messageArrived: info"+data);
                    remoteUserName=new String(message.getPayload());
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    TVLog("deliveryComplete-"+token.getMessage().toString());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        };

        private void buildMQTTClient(){
            //mqttAndroidClient = new MqttAndroidClient(mContext, MQTTCons.Broker, clientId);
            mqttAndroidClient = new MqttAndroidClient(mContext, "tcp://bemfa.com:9501", clientId);
            mqttAndroidClient.setCallback(mqttCallback);

            mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setConnectionTimeout(10);
            mqttConnectOptions.setKeepAliveInterval(20);
            mqttConnectOptions.setCleanSession(true);

            doClientConnection();
        }

        private synchronized void startReconnectTask(){
            if (reconnectPool != null)return;
            reconnectPool = Executors.newScheduledThreadPool(1);
            reconnectPool.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            } , 0 , 5*1000 , TimeUnit.MILLISECONDS);
        }

        private synchronized void closeReconnectTask(){
            if (reconnectPool != null) {
                reconnectPool.shutdownNow();
                reconnectPool = null;
            }
        }

        /**
         * 连接MQTT服务器
         */
        private synchronized void doClientConnection() {
            if (!mqttAndroidClient.isConnected()) {
                try {
                    mqttAndroidClient.connect(mqttConnectOptions, null, iMqttActionListener);
                    TVLog("mqttAndroidClient-connecting-"+mqttAndroidClient.getClientId());
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }

        private void subscribeToTopic() {//订阅之前会取消订阅，避免重连导致重复订阅
            try {

                mqttAndroidClient.unsubscribe(new String[]{"data", "info"}, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        TVLog("unsubscribe-"+"success");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        TVLog("unsubscribe-"+"failed-"+exception);
                    }
                });
                mqttAndroidClient.subscribe(new String[]{"data", "info"}, new int[]{0, 0}, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {//订阅成功
                        TVLog("subscribe-"+"success");
                        isOK=true;
                        //sendMQTT("data","hello");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    startReconnectTask();
                        TVLog("subscribe-"+"failed-"+exception);
                    }
                });

            } catch (MqttException ex) {
            }
        }

        public void sendMQTT(String topic, String msg) {
            try {
                if (mqttAndroidClient == null)return;
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                mqttAndroidClient.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
//                    TVLog("sendMQTT-"+"success:" + msg);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    startReconnectTask();
                        TVLog("sendMQTT-"+"failed:" + msg);
                    }
                });
            } catch (MqttException e) {
            }
        }

        public void closeMQTT(){
            closeReconnectTask();
            if (mqttAndroidClient != null){
                try {
                    mqttAndroidClient.unregisterResources();
                    mqttAndroidClient.disconnect();
                    TVLog("closeMQTT-"+mqttAndroidClient.getClientId());
                    mqttAndroidClient = null;
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }

        private void TVLog(String msg){
            String TAG="MQTT";
            Log.d(TAG, "TVLog: "+msg);
        }
    }


}