package com.gephenom.smarthome.media;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import com.gephenom.smarthome.internet.SocketIo;
import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.main.MyApplicationClass;
import com.gephenom.smarthome.tools.Tools;

import java.io.IOException;

public class MyMediaPlayer implements MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener{
    private  String TAG = "freedom";
    public MediaPlayer mediaPlayer = null;
    public String mediaPlayerState = "stopped";

    private  MyApplicationClass myApplication;
    public MyMediaPlayer(MyApplicationClass myapplicationClass){
        myApplication=myapplicationClass;
        myApplication.addLog("音乐播放模块初始化...");
    }
    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {
        player.start();
    }
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onError");
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        lastOrNext("next");
    }

    public void musicPlay(){
        if (mediaPlayerState.equals("paused")) {
            mediaPlayerState = "playing";
            mediaPlayer.start();
            //speechSynthesis.myStartSpeaking(getString(R.string.music_start_voice));
            myApplication.socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStart','musicName':'" + myApplication.musicList.get(myApplication.musicPlayIndex) + "','direction':'up'}"));
        } else {
            try {
                setMusicUrl(myApplication.musicList.get(myApplication.musicPlayIndex));
                //speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[0].split("_")));
            } catch (IOException e) {
                myApplication.speechSynthesis.myStartSpeaking("online music error");
                e.printStackTrace();
            }
        }
    }

    public void musicChange(int musicPlayIndex){
        try {
            myApplication.musicPlayIndex = musicPlayIndex;
            setMusicUrl(myApplication.musicList.get(musicPlayIndex));
            //speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",musicList.get(musicPlayIndex).split("_-_")[0].split("_")));
        } catch (IOException e) {
            myApplication.speechSynthesis.myStartSpeaking("online music error");
            e.printStackTrace();
        }
    }

    public void lastOrNext(String option){
        if(myApplication.musicMode.equals("random")){
            try {
                myApplication.musicPlayIndex=(int)(Math.floor(Math.random()*myApplication.musicList.size()));
                setMusicUrl(myApplication.musicList.get(myApplication.musicPlayIndex));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void  setMusicUrl(String url) throws IOException {
        mediaPlayerState = "playing";
        if(!(mediaPlayer==null)){mediaPlayer.release();}
        mediaPlayer = null;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(myApplication.serverUrl+"/myMusic/"+url);
        //mediaPlayer.prepare(); // might take long! (for buffering, etc)
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.prepareAsync();
        myApplication.socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStart','musicIndex':"+Integer.toString(myApplication.musicPlayIndex)+",'direction':'up'}"));
        Log.d(TAG, "musicIndex:"+myApplication.musicPlayIndex);
        myApplication.speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",url.split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",url.split("_-_")[0].split("_")));
    }

    public void pause(){
        mediaPlayerState = "paused";
        mediaPlayer.pause();
    }

    public void cancel() {
        if(!(mediaPlayer==null)){mediaPlayer.release();}
        mediaPlayer = null;
        mediaPlayerState = "stopped";
        myApplication.socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStop','direction':'up'}"));
    }


}
