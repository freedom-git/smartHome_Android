package com.gephenom.smarthome.main;


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
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;


import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.ArrayList;


public class MyActivity extends AppCompatActivity{

    public   String TAG = "freedom"; //用于log
    public String serverUrl = "http://www.gephenom.com:3000";
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
    public ArrayList<String> musicList = new ArrayList<String>();
    public int musicPlayIndex=0;
    public String musicMode="random";

    //温度控制相关参数
    public float temValue = 0;//当前温度值
    public boolean autoTemControl = false;//自动控温开关
    public int maxTem = 29;//自动控温温度上限
    public int minTem = 28;//自动控温温度上下限
    public String airConditioningStatus = "off";

    //湿度控制相关参数
    public float humValue = 0;//当前湿度值
    public boolean autoHumControl = false;//自动控湿开关
    public int maxHum = 80;//自动控湿湿度上限
    public int minHum = 60;//自动控湿湿度上下限


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//                speechRecognize.startRecognize();
                speechWake.startWake();
            }
        });

        //socket-io对象实例
        socketIo = new SocketIo(serverUrl);
        socketIo.mSocket.on("musicCtrl", musicCtrlListener);
        socketIo.mSocket.on("airCtrl", airCtrlListener);
        socketIo.mSocket.on("memberManage", memberManageListener);
        socketIo.mSocket.connect();
        SpeechUtility.createUtility(MyActivity.this, SpeechConstant.APPID+"=57f314f0");
        //语音合成对象实例
        speechSynthesis = new SpeechSynthesis(MyActivity.this,getString(R.string.voicer));
        //语音识别对象实例
        //speechRecognize = new SpeechRecognize(MyActivity.this,this);
        //语音唤醒对象实例
        speechWake = new SpeechWake(this);
        //音乐播放对象实例
        myMediaPlayer = new MyMediaPlayer(this);


        //Bluetooth对象实例
        bluetooth = new Bluetooth(this,socketIo);
        //AirConditioner实例
        airConditioner = new AirConditioner(this);


        speechSynthesis.myStartSpeaking(getString(R.string.welcome_voice));

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

    private Emitter.Listener musicCtrlListener = new Emitter.Listener() {
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
                        if(!direction.equals("down")&&!direction.equals("both")){return;}

                        switch (event){
                            case "changeMusic":
                                musicIndex = data.getInt("musicIndex");
                                musicPlayIndex=musicIndex;
                                break;
                            case "MusicContinue":
                                musicIndex = data.getInt("musicIndex");
                                if(musicIndex>=0){musicPlayIndex=musicIndex;}
                                break;
                            case "musicFileDir":
                                musicFileDir = data.getJSONArray("musicFileDir");
                                Log.d(TAG, "musicCtrlListener="+musicFileDir.length());
                                if(musicFileDir.length()>0){
                                    musicList.clear();
                                    Log.d(TAG, "musicList.clear() " + musicList.size());
                                    for(int i = 0 ; i < musicFileDir.length() ; i++){
                                        musicList.add(musicFileDir.getString(i));
                                        //Log.d(TAG, "musicList add " + musicList.get(i));
                                    }
                                }
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch(event) {
                        case "musicFileDir":

                            break;
                        case "MusicPause":
                            if(myMediaPlayer.getMediaPlayerState().equals("playing")){
                                myMediaPlayer.mediaPlayer.pause();
                                //speechSynthesis.myStartSpeaking(getString(R.string.music_pause_voice));
                                socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicPaused','direction':'up'}"));
                            }

                            break;
                        case "MusicContinue":
                            if(myMediaPlayer.getMediaPlayerState().equals("paused")){
                                myMediaPlayer.mediaPlayer.start();
                                //speechSynthesis.myStartSpeaking(getString(R.string.music_start_voice));
                                socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStart','musicName':'" + musicList.get(musicPlayIndex) + "','direction':'up'}"));
                            }else{
                                try {
                                    myMediaPlayer.setMusicUrl(musicList.get(musicPlayIndex));
                                    //speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[0].split("_")));
                                } catch (IOException e) {
                                    speechSynthesis.myStartSpeaking("online music error");
                                    e.printStackTrace();
                                }
                            }
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
                            nextMusic();
                            break;
                        case "stop":
                            myMediaPlayer.cancel();
                            break;
                        case "back":
                            bluetooth.connectBLE.writeByte(new byte[]{(byte)0xff,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xff});
                            break;
                    }

                    EditText editText = (EditText) findViewById(R.id.edit_message);
                    editText.setText(event);
                }
            });
        }
    };

    public void nextMusic(){
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
                        if(!direction.equals("down")&&!direction.equals("both")){return;}

                        switch (event){
                            case "changeMusic":
                                musicIndex = data.getInt("musicIndex");
                                musicPlayIndex=musicIndex;
                                break;
                            case "MusicContinue":
                                musicIndex = data.getInt("musicIndex");
                                if(musicIndex>=0){musicPlayIndex=musicIndex;}
                                break;
                            case "musicFileDir":
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch(event) {
                        case "startTemAutoCtrl":
                            autoTemControl=true;
                            airConditioner.airConditionerAutoCtrl(temValue);
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':"+autoTemControl+",'direction':'up'}"));
                            break;
                        case "stopTemAutoCtrl":
                            autoTemControl=false;
                            airConditioner.stop();
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':"+autoTemControl+",'direction':'up'}"));
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
                        case "back":
                            bluetooth.connectBLE.writeByte(new byte[]{(byte)0xff,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xff});
                            break;
                    }

                    EditText editText = (EditText) findViewById(R.id.edit_message);
                    editText.setText(event);
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
                        if(!direction.equals("down")&&!direction.equals("both")){return;}

                        switch (event){
                            case "changeMusic":
                                musicIndex = data.getInt("musicIndex");
                                musicPlayIndex=musicIndex;
                                break;
                            case "MusicContinue":
                                musicIndex = data.getInt("musicIndex");
                                if(musicIndex>=0){musicPlayIndex=musicIndex;}
                                break;
                            case "musicFileDir":
                                musicFileDir = data.getJSONArray("musicFileDir");
                                Log.d(TAG, "musicCtrlListener="+musicFileDir.length());
                                if(musicFileDir.length()>0){
                                    for(int i = 0 ; i < musicFileDir.length() ; i++){
                                        musicList.add(musicFileDir.getString(i));
                                        //Log.d(TAG, "musicList add " + musicList.get(i));
                                    }
                                }
                                break;
                        }

                    } catch (JSONException e) {
                        return;
                    }
                    switch(event) {
                        case "newMemberConnected":
                            sendHomeStatus();
                            break;
                        case "stopTemAutoCtrl":
                            autoTemControl=false;
                            bluetooth.connectBLE.writeByte(new byte[]{(byte)0x04,(byte)0x00,(byte)0x08,(byte)0x08,(byte)0x04});
                            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureAutoCtrlStatusUpdate','temperatureAutoCtrlStatus':"+autoTemControl+",'direction':'up'}"));
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
                        case "back":
                            bluetooth.connectBLE.writeByte(new byte[]{(byte)0xff,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xff});
                            break;
                    }

                    EditText editText = (EditText) findViewById(R.id.edit_message);
                    editText.setText(event);
                }
            });
        }
    };


    public void sendHomeStatus() {
        socketIo.mSocket.emit("homeStatus", Tools.getJsonFromString("{" +
                "'event':'sendHomeStatus'," +
                "'direction':'up'," +
                "'musicStatus':{'musicIndex':"+Integer.toString(musicPlayIndex)+",'status':"+myMediaPlayer.getMediaPlayerState()+",'mode':"+musicMode+"}," +
                "'temperature':{'value':"+temValue+",'lowerLimit':"+minTem+",'upperLimit':"+maxTem+",'autoCtrlStatus':"+autoTemControl+",'airConditioningStatus':"+airConditioningStatus+"}," +
                "'humidity':{'value':"+humValue+",'lowerLimit':"+minHum+",'upperLimit':"+maxHum+",'autoCtrlStatus':"+autoHumControl+"}" +
                "}"));
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void loop(){
        final Handler h = new Handler();
        h.postDelayed(new Runnable()
        {
            private long time = 0;

            @Override
            public void run()
            {
                // do stuff then
                // can call h again after work!
                bluetooth.connectBLE.writeByte(new byte[]{(byte)0xff,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xff});
                Log.d(TAG, "Loop" );
                h.postDelayed(this, 60000);
            }
        }, 60000);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        myMediaPlayer.cancel();
        myMediaPlayer = null;

        socketIo.mSocket.disconnect();
        socketIo.mSocket.off("musicCtrl", musicCtrlListener);
        socketIo.mSocket.off("airCtrl", musicCtrlListener);



        socketIo=null;

        bluetooth.connectBLE.cancel();
        bluetooth.connectBLE=null;
        bluetooth=null;
    }


}




