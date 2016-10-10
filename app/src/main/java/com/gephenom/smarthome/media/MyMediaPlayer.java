package com.gephenom.smarthome.media;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import com.gephenom.smarthome.internet.SocketIo;
import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.tools.Tools;

import java.io.IOException;

public class MyMediaPlayer implements MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener{
    private  String TAG = "freedom";
    public MediaPlayer mediaPlayer = null;
    public String mediaPlayerState = "release";

    public MyActivity myActivity;
    public MyMediaPlayer(MyActivity activity){
        myActivity=activity;
    }
    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {
        mediaPlayerState = "prepared";
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

    public void lastOrNext(String option){
        if(myActivity.musicMode.equals("random")){
            try {
                myActivity.musicPlayIndex=(int)(Math.floor(Math.random()*myActivity.musicList.size()));
                setMusicUrl(myActivity.musicList.get(myActivity.musicPlayIndex));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void  setMusicUrl(String url) throws IOException {
        if(!(mediaPlayer==null)){mediaPlayer.release();}
        mediaPlayer = null;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDataSource(myActivity.serverUrl+"/myMusic/"+url);
        //mediaPlayer.prepare(); // might take long! (for buffering, etc)
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.prepareAsync();
        mediaPlayerState = "initialized";
        myActivity.socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStart','musicIndex':"+Integer.toString(myActivity.musicPlayIndex)+",'direction':'up'}"));
        Log.d(TAG, "musicIndex:"+myActivity.musicPlayIndex);
        myActivity.speechSynthesis.myStartSpeaking("music:"+ TextUtils.join(" ",url.split("_-_")[1].split(".mp3")[0].split("_"))+",by "+TextUtils.join(" ",url.split("_-_")[0].split("_")));
    }

    public String getMediaPlayerState(){
        if(mediaPlayerState.equals("prepared")){
            if(mediaPlayer.isPlaying()){
                return "playing";
            }else{
                return  "paused";
            }
        }else{
            return "stop";
        }
    }

    public void cancel() {
        if(!(mediaPlayer==null)){mediaPlayer.release();}
        mediaPlayer = null;
        mediaPlayerState = "release";
        myActivity.socketIo.mSocket.emit("musicCtrl", Tools.getJsonFromString("{'event':'musicStop','direction':'up'}"));
    }


}
