package com.android.healthapp.ui.healthData;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.healthapp.MainActivity;
import com.android.healthapp.R;
import com.android.healthapp.databinding.FragmentDataBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class HealthDataFragment extends Fragment {

    private static LineChart acLineChart;
    private static LineChart tempLineChart,spo2LineChart,hrLineChart,spLineChart,dpLineChart,mcLineChart;
    private static TextView tempTextView,spo2TextView,hrTextView,spTextView,dpTextView,mcTextView;
    private static List<List<Entry>> dataList=new ArrayList<>();
    private static int count=0;
    private static Queue<byte[]> queue=new LinkedList<>();

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

        for(int i=0;i<6;i++){
            dataList.add(new ArrayList<>());
        }

        return view;
    }

    /**
    * 数据更新
    * */
    public static void onDataUpdate(MainActivity.DataPacket dataPacket){
        drawLineChart(0,tempLineChart,dataPacket.temp,0,60,tempTextView,"体温","℃");
        drawLineChart(1,spo2LineChart,dataPacket.spo2,0,100,spo2TextView,"血氧","%");
        drawLineChart(2,hrLineChart,dataPacket.heartRate,0,150,hrTextView,"心率","bpm");
        drawLineChart(3,spLineChart,dataPacket.Sp,0,200,spTextView,"收缩压","nmHg");
        drawLineChart(4,dpLineChart,dataPacket.Dp,0,100,dpTextView,"舒张压","nmHg");
        drawLineChart(5,mcLineChart,dataPacket.Mc,0,100,mcTextView,"微循环","%");
        drawLineChart(acLineChart,dataPacket.acdata);
    }

    private static void drawLineChart(int index, LineChart lineChart, float data,float min,float max,TextView textView,String name,String dw){
        textView.setText(name+": "+Float.toString(data)+dw);

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

        lineChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}