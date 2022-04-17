package com.android.healthapp.ui.healthAssess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.healthapp.MainActivity;
import com.android.healthapp.R;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HealthAssessFragment extends Fragment {

    public static final String TAG="HealthAssess";

    EditText heightEditText;
    EditText weightEditText;
    EditText ageEditText;
    Button startButton;
    LinearLayout linearLayout1,linearLayout2;
    RadioGroup radioGroup;

    private static HorizontalBarChart assessChart=null;
    private static TextView assessText=null;
    private static int count=0;
    private static String info="";
    private static int[] level=new int[5];
    private static float[] ave=new float[6];
    private static float[] sum=new float[6];

    private static Map<String,Number> range=new HashMap<String,Number>(){{
        put("temp_upper_limit",37.5);
        put("temp_lower_limit",36.0);

        put("SpO2_upper_limit",100);
        put("SpO2_lower_limit",95);

        put("HR_upper_limit",100);
        put("HR_lower_limit",60);

        put("Sp_upper_limit",140);
        put("Sp_lower_limit",90);

        put("Dp_upper_limit",90);
        put("Dp_lower_limit",60);

        put("Mc_upper_limit",100);
        put("Mc_lower_limit",79);
    }};

    private int height,weight,gender,age;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_assess_1,container,false);;

        SharedPreferences sharedPreferences=getActivity().getSharedPreferences("data",Context.MODE_PRIVATE);
        height=sharedPreferences.getInt("height",0);
        weight=sharedPreferences.getInt("weight",0);
        age=sharedPreferences.getInt("age",0);
        gender=sharedPreferences.getInt("gender",-1);

        linearLayout1=view.findViewById(R.id.layout_1);
        linearLayout2=view.findViewById(R.id.layout_2);

        assessChart=view.findViewById(R.id.horizontal_bar_chart);
        assessText=view.findViewById(R.id.assess_text);

        /*获取基本信息*/
        if(height==0 && weight==0 && age==0 && gender==-1){
            heightEditText=view.findViewById(R.id.height);
            weightEditText=view.findViewById(R.id.weight);
            ageEditText=view.findViewById(R.id.age);
            startButton=view.findViewById(R.id.start);
            radioGroup=view.findViewById(R.id.gender_radio_group);

            SharedPreferences.Editor editor=getActivity().getSharedPreferences("data",Context.MODE_PRIVATE).edit();

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    RadioButton rb=radioGroup.findViewById(i);
                    if(rb.getText().equals("男")) gender=1;
                    else gender=0;
                    editor.putInt("gender",gender);
                }
            });

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(heightEditText.getText().toString().equals("") || weightEditText.getText().toString().equals("") || ageEditText.getText().toString().equals("")){
                        Toast.makeText(getContext(),"信息不完整！",Toast.LENGTH_SHORT).show();
                    }else{
                        editor.putInt("height",Integer.valueOf(heightEditText.getText().toString()));
                        editor.putInt("weight",Integer.valueOf(weightEditText.getText().toString()));
                        editor.putInt("age",Integer.valueOf(ageEditText.getText().toString()));
                        editor.commit();
                        linearLayout1.setVisibility(View.GONE);
                        linearLayout2.setVisibility(View.VISIBLE);
                    }
                }
            });
        }else{
            linearLayout1.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.VISIBLE);
        }

        initChart();

        return view;
    }

    private void initChart(){
        assessChart.getDescription().setEnabled(false);
        assessChart.getLegend().setEnabled(false);
        setAxis();  // 设置坐标轴
    }

    public static void setData(int[] assess) {
        List<BarEntry> entryList = new ArrayList<>();
        entryList.add(new BarEntry(0, 0));
        entryList.add(new BarEntry(1, assess[0]));
        entryList.add(new BarEntry(2, assess[1]));
        entryList.add(new BarEntry(3, assess[2]));
        entryList.add(new BarEntry(4, assess[3]));
        entryList.add(new BarEntry(5, assess[4]));

        Log.d(TAG, "setData: "+assess[4]);

        BarDataSet barDataSet = new BarDataSet(entryList, "data");
        barDataSet.setDrawValues(false);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.6f); // 设置柱子的宽度
        assessChart.setData(barData);
        assessChart.invalidate();
    }

    private void setAxis() {
        final String label[] = {"","微循环", "血压", "心率","血氧","体温"};
        final String level[] = {"","较差","一般","良好"};
        int levelNum=level.length-1;
        XAxis xAxis = assessChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(15f);
        xAxis.setLabelCount(3);
        xAxis.setGranularity(1f); // 防止放大图后，标签错乱
        xAxis.setAxisMaximum(6);
        xAxis.setAxisMinimum(0);
        xAxis.setLabelCount(6);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                try {
                    return label[(int) value];
                } catch (Exception e) {
                    return "";
                }
            }
        });

        YAxis yAxis_right = assessChart.getAxisRight();
        yAxis_right.setAxisMinimum(0);
        yAxis_right.setAxisMaximum(levelNum);
        yAxis_right.setLabelCount(levelNum);
        yAxis_right.setTextSize(14f);
        yAxis_right.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return level[(int)value];
            }
        });

        // 不显示最顶部的轴
        YAxis yAxis_left = assessChart.getAxisLeft();
        yAxis_left.setAxisMinimum(0);
        yAxis_left.setAxisMaximum(levelNum);
        yAxis_left.setLabelCount(levelNum);
        yAxis_left.setEnabled(false);
    }

    public static void healthAssess(MainActivity.DataPacket dataPacket){

        if(count==60){
            ave[0]=sum[0]/count;
            ave[1]=sum[1]/count;
            ave[2]=sum[2]/count;
            ave[3]=sum[3]/count;
            ave[4]=sum[4]/count;
            ave[5]=sum[5]/count;
            //Log.d(TAG, "healthAssess: ave="+ave[5]);
            setLevelAndInfo(ave);
            count=0;
            sum=new float[6];
            ave=new float[6];
        }else{
            count++;
            sum[0]+=dataPacket.temp;
            sum[1]+=dataPacket.spo2;
            sum[2]+=dataPacket.heartRate;
            sum[3]+=dataPacket.Sp;
            sum[4]+=dataPacket.Dp;
            sum[5]+=dataPacket.Mc;
            //Log.d(TAG, "healthAssess: sum="+sum[5]);
        }

        if(assessText!=null && assessChart!=null){
            assessText.setTextColor(Color.BLACK);
            if(!info.equals("")) assessText.setText(info);
            else assessText.setText("正在评估健康状况. . .");
            setData(level);
        }
    }

    private static void setLevelAndInfo(float[] ave) {

        info="";
        level=new int[5];
        DecimalFormat df1 = new DecimalFormat("#.00");

        info+="1.体温："+df1.format(ave[0])+"℃，";
        if(ave[0] > (Double)range.get("temp_upper_limit")){
            info = info + "偏高\n";
            level[4]=2;
        }else if(ave[0] < (Double)range.get("temp_lower_limit")){
            info = info + "偏低\n";
            level[4]=2;
        }else{
            info = info + "正常\n";
            level[4]=3;
        }

        info+="\n2.血氧："+df1.format(ave[1])+"%，";
        if(ave[1] > (int)range.get("SpO2_upper_limit")){
            info = info +"异常\n";
            level[3]=2;
        }else if(ave[1] < (int)range.get("SpO2_lower_limit")){
            info = info +"血氧饱和度较低，轻度缺氧,呼吸系统或心血管系统可能存在异常，长时间低氧会对神经系统及身体器官造成损伤。" +
                    "建议增加有氧运动，例如慢跑、打太极等；戒烟戒酒；吃富含维生素和铁的菜花、菠菜等绿叶蔬菜的健康饮食，避免由于贫血造成血氧不足。\n";
            level[3]=2;
        }else{
            info = info +"正常\n";
            level[3]=3;
        }

        info+="\n3.心率："+(int)ave[2]+"bpm，";
        if(ave[2] > (int)range.get("HR_upper_limit")){
            info = info +"心动过速，如果心跳过快以至于不能维持有效的血液循环时，可能出现心慌、气短、乏力等症状。\n";
            level[2]=2;
        }else if(ave[2] < (int)range.get("HR_lower_limit")){
            info = info +"心动过缓，可能有头晕、乏力、倦怠、精神差的症状，运动员或家族遗传一般无异常。\n";
            level[2]=2;
        }else{
            info = info +"正常\n";
            level[2]=3;
        }

        info+="\n4.血压："+(int)ave[3]+"/"+(int)ave[4]+"mmHg，";
        if(ave[3] > (int)range.get("Sp_upper_limit")){
            info = info +"血压较高，轻度时没有明显的症状，随着血压持续升高，可能出现头晕、头痛、颈项板紧、疲劳、心悸等症状。" +
                    "建议调节饮食，补钾排钠，增加粗粮摄入；定期测量血压，注意别在运动后测量血压，运动后血压升高是正常现象。\n";
            level[1]=2;
        }else if(ave[3] < (int)range.get("Sp_lower_limit")){
            info = info +"血压较低，生理性低血压无明显症状；病理性低血压容易出现头晕乏力，眼黑肢软，严重时会出现昏厥的症状。" +
                    "建议少熬夜、避免过度劳累；注意补充营养；增加有氧运动。\n";
            level[1]=2;
        }else{
            info = info +"血压正常。\n";
            level[1]=3;
        }

        if(ave[4] > (int)range.get("Dp_upper_limit")){
            //info = info +"舒张压较高\n";
        }else if(ave[4] < (int)range.get("Dp_lower_limit")){
            //info = info +"舒张压较低\n";
        }else{
            //info = info +"舒张压正常\n";
        }

        info+="\n5.微循环："+df1.format(ave[5])+"%，";
        if(ave[5] > (int)range.get("Mc_upper_limit")){
            info = info +"异常\n";
            level[0]=2;
        }else if(ave[5] < (int)range.get("Mc_lower_limit")){
            info = info +"血管指数过低，处于亚健康状态，容易出现疲乏无力、情绪低落、睡眠质量差等症状。" +
                    "建议少熬夜、避免过度劳累；低盐低脂饮食；戒烟戒酒；增加有氧运动。\n";
            level[0]=2;
        }else{
            info = info +"正常\n";
            level[0]=3;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}