package com.gephenom.smarthome.speech;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.tools.Tools;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * Created by freedom on 2016/10/2 0002.
 */
public class SpeechRecognize {
    private static String TAG = "freedom";
    private MyActivity myActivity;
    // 语音识别对象
    private SpeechRecognizer mAsr;
    // 云端语法文件
    private String mCloudGrammar = null;
    // 语法、词典临时变量
    String mContent;
    // 函数调用返回值
    int ret = 0;

    private String grammarAbnfId = null;
    private Toast mToast;

    private static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
    private static final String GRAMMAR_TYPE_ABNF = "abnf";
    private String mEngineType = "cloud";


    public SpeechRecognize(Context context, MyActivity activity){
        myActivity=activity;
        mAsr = SpeechRecognizer.createRecognizer(context, mInitListener);
        mCloudGrammar = Tools.readFile(context,"grammar_sample.abnf","utf-8");
        mContent = new String(mCloudGrammar);
        //指定引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING,"utf-8");
        ret = mAsr.buildGrammar(GRAMMAR_TYPE_ABNF, mContent, mCloudGrammarListener);
        if(ret != ErrorCode.SUCCESS)
            showTip("语法构建失败,错误码：" + ret);

        mToast = Toast.makeText(context,"",Toast.LENGTH_SHORT);

    }

    public void startRecognize() {
        if (!setParam()) {
            showTip("请先构建语法。");
            return;
        };
        ret = mAsr.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("识别失败,错误码: " + ret);
        }


//        // 停止识别
//        case R.id.isr_stop:
//        mAsr.stopListening();
//        showTip("停止识别");
//        break;
//        // 取消识别
//        case R.id.isr_cancel:
//        mAsr.cancel();
//        showTip("取消识别");
//        break;


    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognize init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code);
            }
        }
    };



    /**
     * 云端构建语法监听器。
     */
    private GrammarListener mCloudGrammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if(error == null){
                String grammarID = new String(grammarId);
                if(!TextUtils.isEmpty(grammarId))
                    grammarAbnfId=grammarID;
                showTip("语法构建成功：" + grammarId);
            }else{
                showTip("语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };
    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            //Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                String text = parseGrammarResult(result.getResultString());
                if(!text.equals("没有匹配结果.")){myActivity.nextMusic();}
                Log.d(TAG, "text : "+text);
            } else {
                Log.d(TAG, "recognizer result : null");
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            showTip("onError Code："	+ error.getErrorCode());
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

    public static String parseGrammarResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                for(int j = 0; j < items.length(); j++)
                {
                    JSONObject obj = items.getJSONObject(j);
                    if(obj.getString("w").contains("nomatch"))
                    {
                        ret.append("没有匹配结果.");
                        return ret.toString();
                    }
                    ret.append("【结果】" + obj.getString("w"));
                    ret.append("【置信度】" + obj.getInt("sc"));
                    ret.append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.append("没有匹配结果.");
        }
        return ret.toString();
    }

    private void showTip(final String str) {
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    /**
     * 参数设置
     * @param "param"
     * @return
     */
    public boolean setParam(){
        boolean result = false;
        //设置识别引擎
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        //设置返回结果为json格式
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");

        if("cloud".equalsIgnoreCase(mEngineType))
        {
            String grammarId = grammarAbnfId;
            if(TextUtils.isEmpty(grammarId))
            {
                result =  false;
            }else {
                //设置云端识别使用的语法id
                mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
                result =  true;
            }
        }

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/asr.wav");
        return result;
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // 退出时释放连接
//        mAsr.cancel();
//        mAsr.destroy();
//    }
//
//    @Override
//    protected void onResume() {
//        //移动数据统计分析
//        FlowerCollector.onResume(AsrDemo.this);
//        FlowerCollector.onPageStart(TAG);
//        super.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        //移动数据统计分析
//        FlowerCollector.onPageEnd(TAG);
//        FlowerCollector.onPause(AsrDemo.this);
//        super.onPause();
//    }


}
