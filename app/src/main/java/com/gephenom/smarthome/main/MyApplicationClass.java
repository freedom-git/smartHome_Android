package com.gephenom.smarthome.main;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.gephenom.smarthome.R;
import com.gephenom.smarthome.devices.AirConditioner;
import com.gephenom.smarthome.internet.Bluetooth;
import com.gephenom.smarthome.internet.SocketIo;
import com.gephenom.smarthome.media.MyMediaPlayer;
import com.gephenom.smarthome.speech.SpeechSynthesis;
import com.gephenom.smarthome.speech.SpeechWake;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import org.json.JSONArray;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;


/**
 * Created by freedom on 2017/3/5 0005.
 */

public class MyApplicationClass extends Application {

    public String TAG = "freedom"; //用于log
    //语音合成实例
    public SpeechSynthesis speechSynthesis;
    //socketIo实例
    public SocketIo socketIo;
    public String serverUrl = "http://www.gephenom.com:3000";

    //音乐播放实例
    public MyMediaPlayer myMediaPlayer;
    //音乐播放相关参数
    public JSONArray musicFileDir;
    public ArrayList<String> musicList = new ArrayList<String>();
    public int musicPlayIndex = 0;
    public String musicMode = "random";

    //灯光控制
    public ArrayList<String> light = new ArrayList<String>();

    //温度控制相关参数
    public float temValue = 0;//当前温度值
    public boolean autoTemControl = false;//自动控温开关
    public int maxTem = 29;//自动控温温度上限
    public int minTem = 28;//自动控温温度上下限

    //湿度控制相关参数
    public float humValue = 0;//当前湿度值
    public boolean autoHumControl = false;//自动控湿开关
    public int maxHum = 80;//自动控湿湿度上限
    public int minHum = 60;//自动控湿湿度上下限

    //空调控制相关参数
    public String airConditioningStatus = "off";
    public String airConditioningMode = "coldMode";
    public int airConditioningWindGrade = 3;
    public boolean airConditioningWindAutoDirection = false;
    public byte settingTem = 24;

    //Bluetooth实例
    public Bluetooth bluetooth;

    //AirConditioner实例
    public AirConditioner airConditioner;
    //语音唤醒实例
    public SpeechWake speechWake;
    //log
    private String log="";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application");

        StringBuffer param = new StringBuffer();
        param.append("appid=" + getString(R.string.app_id));
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(this, param.toString());

        //语音合成对象实例
        speechSynthesis = new SpeechSynthesis(this, getString(R.string.voicer));
        speechSynthesis.myStartSpeaking(getString(R.string.welcome_voice));

        //socket-io对象实例
        socketIo = new SocketIo(serverUrl,this);

        //音乐播放对象实例
        myMediaPlayer = new MyMediaPlayer(this);

        //AirConditioner实例
        airConditioner = new AirConditioner(this);

        //语音唤醒实例
        speechWake = new SpeechWake(this);

    }

    public void initWithActivity (Activity activity) {
        //Bluetooth对象实例
        if(bluetooth==null){bluetooth = new Bluetooth(this,activity);}
    }

    // Listener defined earlier
    public interface LogChangeListener {
        public void onLogChange(String log);
    }

    // Member variable was defined earlier
    private LogChangeListener listener;

    public void setLogChangeListener(LogChangeListener listener) {
        this.listener = listener;
        listener.onLogChange(log);
    }

    public void cancelLogChangeListener() {
        this.listener = null;
    }

    public void addLog(String newLog){
        Date date = new Date();
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        String time = android.text.format.DateFormat.format("MM-dd hh:mm:ss", new java.util.Date()).toString();
        if (log.length() > 0) {
            log = log + "\n" + time + " :  " + newLog;
        } else {
            log = time + " :  " + newLog;
        }
        if (listener != null) listener.onLogChange(log);
    }




}
