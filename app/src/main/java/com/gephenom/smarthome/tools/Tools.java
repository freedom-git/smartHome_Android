package com.gephenom.smarthome.tools;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class Tools {
    /**
     * 字符串转换成Json
     * @param String str 待转换的ASCII字符串
     * @return JSONObject
     */
    public static JSONObject getJsonFromString(String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    /**
     * 读取asset目录下文件。
     * @return content
     */
    public static String readFile(Context mContext, String file, String code)
    {
        int len = 0;
        byte []buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len  = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            result = new String(buf,code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
