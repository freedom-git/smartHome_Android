package com.gephenom.smarthome.main;


import android.app.ActionBar;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.gephenom.smarthome.R;
import com.gephenom.smarthome.devices.AirConditioner;
import com.gephenom.smarthome.internet.Bluetooth;
import com.gephenom.smarthome.internet.SocketIo;
import com.gephenom.smarthome.media.MyMediaPlayer;
import com.gephenom.smarthome.speech.SpeechRecognize;
import com.gephenom.smarthome.speech.SpeechSynthesis;

import com.gephenom.smarthome.speech.SpeechWake;
import com.gephenom.smarthome.tools.Tools;
import com.github.nkzawa.emitter.Emitter;




import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MyActivity extends AppCompatActivity {

    public String TAG = "freedom"; //用于log

    //语音合成实例
    public SpeechSynthesis speechSynthesis;
    //语音识别实例
    public SpeechRecognize speechRecognize;
    //语音唤醒实例
    public SpeechWake speechWake;
    //音乐播放实例
    private MyMediaPlayer myMediaPlayer;
    //socketIo实例
    public SocketIo socketIo;
    //Bluetooth实例
    public Bluetooth bluetooth;
    //AirConditioner实例
    public AirConditioner airConditioner;

    private Toast mToast;

    //音乐播放相关参数
    public ArrayList<String> musicList  ;
    public int musicPlayIndex ;
    public String musicMode ;

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
    public int settingTem = 24;

    private EditText editText;

    public  MyApplicationClass myApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);


        myApplication=(MyApplicationClass)getApplication();
        myApplication.initWithActivity(this);

        editText = (EditText) findViewById(R.id.edit_message);
        editText.setKeyListener(null);


        musicList=myApplication.musicList  ;
        musicPlayIndex=myApplication.musicPlayIndex ;
        musicMode= myApplication.musicMode;
        //灯光控制
        light = myApplication.light;

        //温度控制相关参数
        temValue = myApplication.temValue;//当前温度值
        autoTemControl = myApplication.autoTemControl;//自动控温开关
        maxTem = myApplication.maxTem;//自动控温温度上限
        minTem = myApplication.minTem;//自动控温温度上下限

        //湿度控制相关参数
        humValue = myApplication.humValue;//当前湿度值
        autoHumControl = myApplication.autoHumControl;//自动控湿开关
        maxHum = myApplication.maxHum;//自动控湿湿度上限
        minHum = myApplication.minHum;//自动控湿湿度上下限

        //空调控制相关参数
        airConditioningStatus = myApplication.airConditioningStatus;
        airConditioningMode = myApplication.airConditioningMode;
        airConditioningWindGrade = myApplication.airConditioningWindGrade;
        airConditioningWindAutoDirection = myApplication.airConditioningWindAutoDirection;
        settingTem = myApplication.settingTem;






        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyActivity.this, WebViewActivity.class);
                startActivity(intent);
            }
        });

        //socket-io对象实例
        socketIo =myApplication.socketIo;

        socketIo.mSocket.on("airCtrl", airCtrlListener);
        socketIo.mSocket.on("memberManage", memberManageListener);
        socketIo.mSocket.on("lightCtrl", lightListener);



        //语音合成对象实例
        speechSynthesis=myApplication.speechSynthesis;

        //语音识别对象实例
        //speechRecognize = new SpeechRecognize(MyActivity.this,this);
        //语音唤醒对象实例
        speechWake = myApplication.speechWake;
        //音乐播放对象实例
        myMediaPlayer = myApplication.myMediaPlayer;
        //Bluetooth对象实例
        bluetooth = myApplication.bluetooth;
        //AirConditioner实例
        airConditioner = myApplication.airConditioner;

        //初始化灯光数组
        light.add(0, "off");


        loop();//开始定时任务

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void nextMusic() {
        myMediaPlayer.lastOrNext("next");
    }

    private Emitter.Listener airCtrlListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MyActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String event;
                    String direction;
                    int musicIndex;
                    JSONArray musicFileDir;
                    try {
                        event = data.getString("event");
                        direction = data.getString("direction");
                        if (!direction.equals("down") && !direction.equals("both")) {
                            return;
                        }

                        switch (event) {
                            case "changeMusic":
                                musicIndex = data.getInt("musicIndex");
                                musicPlayIndex = musicIndex;
                                break;
                            case "MusicContinue":
                                musicIndex = data.getInt("musicIndex");
                                if (musicIndex >= 0) {
                                    musicPlayIndex = musicIndex;
                                }
                                break;
                            case "musicFileDir":
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch (event) {
                        case "startTemAutoCtrl":
                            autoTemControl = true;
                            airConditioner.airConditionerAutoCtrl(temValue);
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':" + autoTemControl + ",'direction':'up'}"));
                            break;
                        case "stopTemAutoCtrl":
                            autoTemControl = false;
                            airConditioner.stop();
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':" + autoTemControl + ",'direction':'up'}"));
                            break;
                        case "startWork":
                            airConditioner.start();
                            break;
                        case "stopWork":
                            airConditioner.stop();
                            break;
                        case "addTemperature":
                            airConditioner.addTemperature();
                            break;
                        case "reduceTemperature":
                            airConditioner.reduceTemperature();
                            break;
                        case "coldMode":
                            airConditioner.changeMode(event);
                            break;
                        case "windMode":
                            airConditioner.changeMode(event);
                            break;
                        case "reduceHumidityMode":
                            airConditioner.changeMode(event);
                            break;
                        case "heatMode":
                            airConditioner.changeMode(event);
                            break;
                        case "windDirection":
                            airConditioner.windDirection();
                            break;
                        case "windDirectionAuto":
                            airConditioner.windDirectionAuto();
                            break;
                        case "wind1":
                            airConditioner.changeWindGrade(1);
                            break;
                        case "wind2":
                            airConditioner.changeWindGrade(2);
                            break;
                        case "wind3":
                            airConditioner.changeWindGrade(3);
                            break;
                    }
                    sendHomeStatus();
                    myApplication.addLog(event);
                }
            });
        }
    };

    private Emitter.Listener memberManageListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            MyActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String event;
                    String direction;
                    int musicIndex;
                    JSONArray musicFileDir;
                    try {
                        event = data.getString("event");
                        direction = data.getString("direction");
                        if (!direction.equals("down") && !direction.equals("both")) {
                            return;
                        }

                        switch (event) {
                            case "changeMusic":
                                musicIndex = data.getInt("musicIndex");
                                musicPlayIndex = musicIndex;
                                break;
                            case "MusicContinue":
                                musicIndex = data.getInt("musicIndex");
                                if (musicIndex >= 0) {
                                    musicPlayIndex = musicIndex;
                                }
                                break;
                            case "musicFileDir":
                                musicFileDir = data.getJSONArray("musicFileDir");
                                Log.d(TAG, "musicCtrlListener=" + musicFileDir.length());
                                if (musicFileDir.length() > 0) {
                                    for (int i = 0; i < musicFileDir.length(); i++) {
                                        musicList.add(musicFileDir.getString(i));
                                        //Log.d(TAG, "musicList add " + musicList.get(i));
                                    }
                                }
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch (event) {
                        case "newMemberConnected":
                            sendHomeStatus();
                            break;
                        case "stopTemAutoCtrl":
                            autoTemControl = false;
                            bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte) 0x04, (byte) 0x00, (byte) 0x08, (byte) 0x08, (byte) 0x04});
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':" + autoTemControl + ",'direction':'up'}"));
                            break;
                        case "getIndoorData":
                            break;
                        case "setTemRange":

                            break;
                        case "changeMusic":
                            try {
                                myMediaPlayer.setMusicUrl(musicList.get(musicPlayIndex));
                                //speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[0].split("_")));
                            } catch (IOException e) {
                                speechSynthesis.myStartSpeaking("online music error");
                                e.printStackTrace();
                            }
                            break;
                        case "lastMusic":
                            myMediaPlayer.lastOrNext("last");
                            break;
                        case "nextMusic":
                            myMediaPlayer.lastOrNext("next");
                            break;
                        case "stop":
                            myMediaPlayer.cancel();
                            break;
                    }

                    myApplication.addLog(event);
                }
            });
        }
    };

    private Emitter.Listener lightListener = new Emitter.Listener() {
        int lightId;
        byte lightIdByte;
        byte lightStatusByte;

        @Override
        public void call(final Object... args) {
            MyActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String event;
                    String direction;

                    String lightStatus;
                    try {
                        event = data.getString("event");
                        direction = data.getString("direction");
                        if (!direction.equals("down") && !direction.equals("both")) {
                            return;
                        }

                        switch (event) {
                            case "lightStatusChange":
                                lightId = data.getInt("lightId");
                                lightStatus = data.getString("lightStatus");
                                light.add(lightId, lightStatus);
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch (event) {
                        case "lightStatusChange":
                            if (lightId == 0) {
                                lightIdByte = (byte) lightId;
                            }
                            if (light.get(lightId).equals("on")) {
                                lightStatusByte = (byte) 0x01;
                            } else {
                                lightStatusByte = (byte) 0x00;
                            }
                            bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte) 0xff, (byte) 0x02, lightIdByte, lightStatusByte, (byte) 0xff});
                            break;
                    }
                    myApplication.addLog(event);
                }
            });
        }
    };


    public void sendHomeStatus() {
        socketIo.mSocket.emit("homeStatus", Tools.getJsonFromString("{" +
                "'event':'sendHomeStatus'," +
                "'direction':'up'," +
                "'musicStatus':{'musicIndex':" + Integer.toString(musicPlayIndex) + ",'status':" + myMediaPlayer.mediaPlayerState + ",'mode':" + musicMode + "}," +
                "'temperature':{'value':" + temValue + ",'lowerLimit':" + minTem + ",'upperLimit':" + maxTem + ",'autoCtrlStatus':" + autoTemControl + ",'airConditioningStatus':" + airConditioningStatus + "}," +
                "'humidity':{'value':" + humValue + ",'lowerLimit':" + minHum + ",'upperLimit':" + maxHum + ",'autoCtrlStatus':" + autoHumControl + "}," +
                "'airConditioner':{'mode':" + airConditioningMode + ",'windGrade':" + airConditioningWindGrade + ",'windAutoDirection':" + airConditioningWindAutoDirection + ",'settingTem':" + settingTem + "}" +
                "}"));
    }

//    public void addLogToScreen(String logMessage) {
//        String logContent = editText.getText().toString();
//        Date date = new Date();
//        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
//        String time = android.text.format.DateFormat.format("MM-dd hh:mm:ss", new java.util.Date()).toString();
//        if (logContent.length() > 0) {
//            logContent = logContent + "\n" + time + " :  " + logMessage;
//        } else {
//            logContent = time + " :  " + logMessage;
//        }
//
//        editText.setText(logContent);
//    }


    private void loop() {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private long time = 0;

            @Override
            public void run() {
                // do stuff then
                // can call h again after work!
                if (bluetooth.mBluetoothLeService != null) {
                    bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff});
                }
                Log.d(TAG, "Loop");
                h.postDelayed(this, 60000);
            }
        }, 60000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myApplication.setLogChangeListener(new MyApplicationClass.LogChangeListener() {
            @Override
            public void onLogChange(String log){
                try {
                    editText.setText(log);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        myApplication.cancelLogChangeListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

       // myMediaPlayer.cancel();

        //socketIo.mSocket.disconnect();
        //socketIo.mSocket.off("musicCtrl", musicCtrlListener);
        //socketIo.mSocket.off("airCtrl", musicCtrlListener);


//        bluetooth.onDestroy();
    }





}




