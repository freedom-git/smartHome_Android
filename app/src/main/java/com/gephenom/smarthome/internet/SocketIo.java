package com.gephenom.smarthome.internet;


import android.util.Log;


import com.gephenom.smarthome.main.MyApplicationClass;
import com.gephenom.smarthome.tools.Tools;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;



public class SocketIo {
    public Socket mSocket;
    private  MyApplicationClass myApplication;
    public SocketIo(String url,MyApplicationClass myapplicationClass){
        myApplication=myapplicationClass;
        try {
            mSocket = IO.socket(url);
            mSocket.on("musicCtrl", musicCtrlListener);
            mSocket.connect();
            myApplication.addLog("服务器连接成功");
        } catch (URISyntaxException e) {}
    }

    private Emitter.Listener musicCtrlListener = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
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
                        myApplication.musicPlayIndex = musicIndex;
                        break;
                    case "MusicContinue":
                        musicIndex = data.getInt("musicIndex");
                        if (musicIndex >= 0) {
                            myApplication.musicPlayIndex = musicIndex;
                        }
                        break;
                    case "musicFileDir":
                        if(myApplication.musicList.size()>0){return;}
                        musicFileDir = data.getJSONArray("musicFileDir");
                        Log.d(myApplication.TAG, "musicCtrlListener=" + musicFileDir.length());
                        myApplication.musicFileDir=musicFileDir;
                        if (musicFileDir.length() > 0) {
                            for (int i = 0; i < musicFileDir.length(); i++) {
                                myApplication.musicList.add(musicFileDir.getString(i));
                                //Log.d(TAG, "musicList add " + musicList.get(i));
                            }
                        }
                        break;
                }

            } catch (JSONException e) {
                return;
            }
            switch (event) {
                case "musicFileDir":
                    break;
                case "MusicPause":
                    if (myApplication.myMediaPlayer.mediaPlayerState.equals("playing")) {
                        myApplication.myMediaPlayer.pause();
                        //speechSynthesis.myStartSpeaking(getString(R.string.music_pause_voice));
                        mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicPaused','direction':'up'}"));
                    }

                    break;
                case "MusicContinue":
                    myApplication.myMediaPlayer.musicPlay();
                    break;
                case "changeMusic":
                    myApplication.myMediaPlayer.musicChange(myApplication.musicPlayIndex);
                    break;
                case "lastMusic":
                    myApplication.myMediaPlayer.lastOrNext("last");
                    break;
                case "nextMusic":
                    myApplication.myMediaPlayer.lastOrNext("next");
                    break;
                case "stop":
                    myApplication.myMediaPlayer.cancel();
                    break;
                case "back":
                    myApplication.bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff});
                    break;
            }

            myApplication.addLog("socket:"+event);
        }








    };


}
