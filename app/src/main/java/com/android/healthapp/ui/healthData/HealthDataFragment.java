package com.android.healthapp.ui.healthData;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.android.healthapp.MainActivity;
import com.android.healthapp.R;
import com.android.healthapp.ui.healthAssess.HealthAssessFragment;
import com.android.healthapp.ui.remote.RemoteFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HealthDataFragment extends Fragment {

    private static LineChart acLineChart;
    private static LineChart tempLineChart,spo2LineChart,hrLineChart,spLineChart,dpLineChart,mcLineChart;
    private static TextView tempTextView,spo2TextView,hrTextView,spTextView,dpTextView,mcTextView,stateTextView;
    private static List<List<Entry>> dataList=new ArrayList<>();
    private static int count=0;
    private static Queue<byte[]> queue=new LinkedList<>();

    private static List<Float> yawList = new LinkedList<>();
    private static List<Float> pitchList = new LinkedList<>();
    private static List<Float> rollList = new LinkedList<>();

    public static int btDetect=0;

    static String TAG="HealthDataFragment";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_data,container,false);

        acLineChart=view.findViewById(R.id.ac_chart);
        tempLineChart=view.findViewById(R.id.temp_chart);
        spo2LineChart=view.findViewById(R.id.spo2_chart);
        hrLineChart=view.findViewById(R.id.heart_rate_chart);
        spLineChart=view.findViewById(R.id.sp_chart);
        dpLineChart=view.findViewById(R.id.dp_chart);
        mcLineChart=view.findViewById(R.id.mc_chart);

        tempTextView=view.findViewById(R.id.temp);
        spo2TextView=view.findViewById(R.id.spo2);
        hrTextView=view.findViewById(R.id.heart_rate);
        spTextView=view.findViewById(R.id.sp);
        dpTextView=view.findViewById(R.id.dp);
        mcTextView=view.findViewById(R.id.mc);

        stateTextView=view.findViewById(R.id.state);

        if(HealthDataFragment.isBtDetect()==1) stateTextView.setText("蓝牙监控");
        else if(RemoteFragment.isRemoteDetect()==1) stateTextView.setText("远程监控");

        for(int i=0;i<6;i++){
            dataList.add(new ArrayList<>());
        }

        return view;
    }

    public static int isBtDetect(){
        return btDetect;
    }

    public static void enableBtDetect(){
        btDetect=1;
    }

    public static void disableBtDetect(){
        btDetect=0;
    }

    public static void setStateText(String inf){
        stateTextView.post(new Runnable() {
            @Override
            public void run() {
                stateTextView.setText(inf);
            }
        });
    }

    /**
     * 数据更新
     **/
    public static void onDataUpdate(MainActivity.DataPacket dataPacket){

        drawLineChart(0,tempLineChart,dataPacket.temp,0,60,tempTextView,"体温","℃");
        drawLineChart(1,spo2LineChart,dataPacket.spo2,0,100,spo2TextView,"血氧","%");
        drawLineChart(2,hrLineChart,dataPacket.heartRate,0,150,hrTextView,"心率","bpm");
        drawLineChart(3,spLineChart,dataPacket.Sp,0,200,spTextView,"收缩压","mmHg");
        drawLineChart(4,dpLineChart,dataPacket.Dp,0,100,dpTextView,"舒张压","mmHg");
        drawLineChart(5,mcLineChart,dataPacket.Mc,0,100,mcTextView,"微循环","%");
        drawLineChart(acLineChart,dataPacket.acdata);
    }

    private static void drawLineChart(int index, LineChart lineChart, float data,float min,float max,TextView textView,String name,String dw){

        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(name+": "+ new String(String.valueOf(data)) +dw);
            }
        });

        List<Entry> list=dataList.get(index);
        list.add(new Entry(list.size()+1,data));

        LineDataSet lineDataSet;

        if(list.size()>15)
            lineDataSet=new LineDataSet(list.subList(list.size()-15,list.size()-1),"temp");
        else
            lineDataSet=new LineDataSet(list,"baseData");

        lineDataSet.setDrawCircles(false);
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setCircleColor(Color.RED);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawFilled(true);

        LineData lineData=new LineData(lineDataSet);

        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setData(lineData);
        lineChart.getXAxis().setEnabled(false);
        lineChart.getAxisLeft().setAxisMaximum(max);
        lineChart.getAxisLeft().setAxisMinimum(min);
        lineChart.getAxisRight().setAxisMaximum(max);
        lineChart.getAxisRight().setAxisMinimum(min);

        lineChart.invalidate();
    }

    private static void drawLineChart(LineChart lineChart,byte[] acdata){
        List<Entry> list=new ArrayList<>();
        int j=0;

        queue.offer(acdata);
        if(queue.size()>6) queue.poll();

        for(byte[] x : queue){
            for(int i=0;i<64;i++,j++){
                list.add(new Entry(j,62-x[i]));
            }
        }

        LineDataSet lineDataSet;

        lineDataSet=new LineDataSet(list,"acData");

        lineDataSet.setDrawCircles(false);
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setCircleColor(Color.RED);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawFilled(true);

        LineData lineData=new LineData(lineDataSet);

        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setData(lineData);
        lineChart.getXAxis().setEnabled(false);
        lineChart.getAxisLeft().setAxisMaximum(80);
        lineChart.getAxisLeft().setAxisMinimum(-80);
        lineChart.getAxisRight().setAxisMaximum(80);
        lineChart.getAxisRight().setAxisMinimum(-80);
        lineChart.setNoDataText("无数据");

        lineChart.invalidate();
    }

    /**
     * 姿态识别
     **/
    public static void poseIdentify(String[] yaw,String[] pitch,String[] roll) {
        //Log.d(TAG, "poseIdentify: ypr="+yaw.length+" "+pitch.length+" "+roll.length);
        //收集分析5s内的姿态数据（50个数据点）
        if(count<5){
            count++;
            for(int i=0;i<yaw.length;i++){
                yawList.add(Float.valueOf(yaw[i]));
            }
            for(int i=0;i<pitch.length;i++){
                pitchList.add(Float.valueOf(pitch[i]));
            }
            for(int i=0;i<roll.length-1;i++){
                rollList.add(Float.valueOf(roll[i]));
            }
            //Log.d(TAG, "poseIdentify: yprList="+yawList.size()+" "+pitchList.size()+" "+rollList.size());
        }else{
            count=0;
            //TODO 姿态数据处理



            yawList.clear();
            pitchList.clear();
            rollList.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}