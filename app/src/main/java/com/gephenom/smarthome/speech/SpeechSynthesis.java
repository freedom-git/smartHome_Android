package com.gephenom.smarthome.speech;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.gephenom.smarthome.R;
import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.main.MyApplicationClass;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

public class SpeechSynthesis {
    private  String TAG = "freedom";
    private MyActivity myActivity;
    private  MyApplicationClass myApplication;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "vixk";
    // 缓冲进度
    private int mPercentForBuffering = 0;
    // 播放进度
    private int mPercentForPlaying = 0;

    public SpeechSynthesis(MyApplicationClass myapplication, String voiceName){
        this.voicer=voiceName;
        myApplication=myapplication;
        myApplication.addLog("语音合成模块初始化...");
        mTts = SpeechSynthesizer.createSynthesizer(myApplication, mTtsInitListener);
        setParam();
    }

    public void myStartSpeaking(String text) {
        myApplication.addLog("语音合成输出："+text);
        int code = mTts.startSpeaking(text, mTtsListener);
//			/**
//			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
//			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
//			*/
//			String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
//			int code = mTts.synthesizeToUri(text, path, mTtsListener);

        if (code != ErrorCode.SUCCESS) {
            if(code == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED){
                //未安装则跳转到提示安装页面
                Log.d(TAG, "语音合成失败,: " + code);
                //mInstaller.install();
            }else {
                Log.d(TAG, "语音合成失败,: " + code);
            }
        }
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.d(TAG, "初始化失败,错误码："+code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 参数设置
     * @return
     */
    private void setParam(){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, "80");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH,  "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "100");
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            Log.d(TAG, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
             Log.d(TAG, "继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
//            showTip(String.format(getString(R.string.tts_toast_format),
//                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                // showTip("播放完成");
            } else if (error != null) {
                Log.d(TAG, error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };


}
