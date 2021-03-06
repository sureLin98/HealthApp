package com.android.healthapp.ui.healthData;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.android.healthapp.MainActivity;
import com.android.healthapp.R;
import com.android.healthapp.ui.remote.RemoteFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class HealthDataFragment extends Fragment {

    private static LineChart acLineChart;
    private static LineChart tempLineChart,spo2LineChart,hrLineChart,spLineChart,dpLineChart,mcLineChart;
    private static TextView tempTextView,spo2TextView,hrTextView,spTextView,dpTextView,mcTextView,stateTextView;
    public static ImageView postureImageView;
    private static List<List<Entry>> dataList=new ArrayList<>();
    private static int count=0;
    private static Queue<byte[]> queue=new LinkedList<>();

    private static List<Float> axList = new LinkedList<>();
    private static List<Float> ayList = new LinkedList<>();
    private static List<Float> azList = new LinkedList<>();

    public static int btDetect=0;

    private static int postureIndex=0;

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
        postureImageView=view.findViewById(R.id.posture);

        if(HealthDataFragment.isBtDetect()==1) stateTextView.setText("????????????");
        else if(RemoteFragment.isRemoteDetect()==1) stateTextView.setText("????????????");

        for(int i=0;i<6;i++){
            dataList.add(new ArrayList<>());
        }

        postureImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (postureIndex){
                    case 0:
                        postureImageView.setImageResource(R.drawable.sitting);
                        break;

                    case 1:
                        postureImageView.setImageResource(R.drawable.walking);
                        break;

                    case 2:
                        postureImageView.setImageResource(R.drawable.running);
                        break;

                    default:
                        break;
                }
                postureIndex++;
                postureIndex%=3;
            }
        });
        postureImageView.setVisibility(View.GONE);

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
     * ????????????
     **/
    public static void onDataUpdate(MainActivity.DataPacket dataPacket){
        if(postureImageView.getVisibility()==View.GONE){
            postureImageView.post(new Runnable() {
                @Override
                public void run() {
                    postureImageView.setVisibility(View.VISIBLE);
                }
            });
        }

        drawLineChart(0,tempLineChart,dataPacket.temp,0,60,tempTextView,"??????","???");
        drawLineChart(1,spo2LineChart,dataPacket.spo2,0,100,spo2TextView,"??????","%");
        drawLineChart(2,hrLineChart,dataPacket.heartRate,0,150,hrTextView,"??????","bpm");
        drawLineChart(3,spLineChart,dataPacket.Sp,0,200,spTextView,"?????????","mmHg");
        drawLineChart(4,dpLineChart,dataPacket.Dp,0,100,dpTextView,"?????????","mmHg");
        drawLineChart(5,mcLineChart,dataPacket.Mc,0,100,mcTextView,"?????????","%");
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
        lineChart.setNoDataText("?????????");

        lineChart.invalidate();
    }

    /**
     * ????????????
     **/
    public static void poseIdentify(Context context, String[] yaw, String[] pitch, String[] roll) {
        //Log.d(TAG, "poseIdentify: ypr="+yaw.length+" "+pitch.length+" "+roll.length);
        //????????????5s??????????????????
        if(count<5){
            count++;
            for(int i=0;i<yaw.length;i++){
                axList.add(Float.valueOf(yaw[i]));
            }
            for(int i=0;i<pitch.length;i++){
                ayList.add(Float.valueOf(pitch[i]));
            }
            for(int i=0;i<roll.length-1;i++){
                azList.add(Float.valueOf(roll[i]));
            }
            //Log.d(TAG, "poseIdentify: yprList="+yawList.size()+" "+pitchList.size()+" "+rollList.size());
        }else{
            count=0;
            //TODO ??????????????????

            MainActivity.savePostureData(context, axList, ayList, azList);

            //postureImageView.setImageResource(R.drawable.walking);

            axList.clear();
            ayList.clear();
            azList.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}