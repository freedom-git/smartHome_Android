package com.gephenom.smarthome.main;


import android.os.Build;
import android.os.Bundle;


import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.view.KeyEvent;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.gephenom.smarthome.R;
import com.gephenom.smarthome.tools.Tools;

import org.json.JSONException;
import org.json.JSONObject;

public class WebViewActivity extends AppCompatActivity {
    private WebView mWebView;
    public  MyApplicationClass myApplication;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);

        myApplication=(MyApplicationClass)getApplication();

        mWebView = (WebView) findViewById(R.id.wvPortal);

        mWebView.loadUrl("file:///android_asset/www/index.html");

        WebSettings mWebSettings = mWebView.getSettings();

        mWebSettings.setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebViewActivity.BridgeWCClient());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.setWebContentsDebuggingEnabled(true);
        }
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN){
            mWebSettings.setAllowUniversalAccessFromFileURLs(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(myApplication.TAG, "KeyEvent.ACTION_DOWN");
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    Log.d(myApplication.TAG, String.valueOf(mWebView.canGoBack()));
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }



    private class BridgeWCClient extends WebChromeClient {
        String BRIDGE_KEY="hello freedom";
        @Override

        public boolean onJsPrompt(WebView view, String url, String title, String message, JsPromptResult result) {

            if (title.equals(BRIDGE_KEY)) {

                JSONObject commandJSON = null;

                try {

                    commandJSON = new JSONObject(message);

                    processCommand(commandJSON);

                } catch (JSONException ex) {

                    //Received an invalid json

                    Log.e(myApplication.TAG, "Invalid JSON: " + ex.getMessage());

                    result.confirm();

                    return true;

                }

                result.confirm();

                return true;

            } else {

                return false;

            }

        }



        private void processCommand(JSONObject commandJSON) throws JSONException{

            String command = commandJSON.getString("method");

            switch (command){
                case "musicPause":
                    myApplication.myMediaPlayer.pause();
                    break;
                case "musicPlay":
                    myApplication.myMediaPlayer.musicPlay();
                    break;
                case "musicNext":
                    myApplication.myMediaPlayer.lastOrNext("next");
                    break;
                case "musicStop":
                    myApplication.myMediaPlayer.cancel();
                    break;
                case "musicChange":
                    myApplication.myMediaPlayer.musicChange(commandJSON.getInt("index"));
                    break;
                case "getMusicList":
                    sendMusicList();
                    break;
            }
            mWebView.loadUrl("javascript:Bridge.homeStatusCallback("+

                    Tools.getJsonFromString("{" +
                            "'event':'sendHomeStatus'," +
                            "'direction':'up'," +
                            "'musicStatus':{'musicIndex':" + Integer.toString(myApplication.musicPlayIndex) + ",'status':" + myApplication.myMediaPlayer.mediaPlayerState + ",'mode':" + myApplication.musicMode + "}," +
                            "'temperature':{'value':" + myApplication.temValue + ",'lowerLimit':" + myApplication.minTem + ",'upperLimit':" + myApplication.maxTem + ",'autoCtrlStatus':" + myApplication.autoTemControl + ",'airConditioningStatus':" + myApplication.airConditioningStatus + "}," +
                            "'humidity':{'value':" + myApplication.humValue + ",'lowerLimit':" + myApplication.minHum + ",'upperLimit':" + myApplication.maxHum + ",'autoCtrlStatus':" + myApplication.autoHumControl + "}," +
                            "'airConditioner':{'mode':" + myApplication.airConditioningMode + ",'windGrade':" + myApplication.airConditioningWindGrade + ",'windAutoDirection':" + myApplication.airConditioningWindAutoDirection + ",'settingTem':" + myApplication.settingTem + "}" +
                            "}")

                    +")");

        }

        private void sendMusicList(){
            mWebView.loadUrl("javascript:Bridge.musicListCallback("+

                    myApplication.musicFileDir

                    +")");
        }





    }

}
