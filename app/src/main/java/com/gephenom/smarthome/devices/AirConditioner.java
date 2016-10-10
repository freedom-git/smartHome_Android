package com.gephenom.smarthome.devices;

import android.util.Log;

import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.tools.Tools;

/**
 * Created by freedom on 2016/9/19 0019.
 */
public class AirConditioner {

    //Bluetooth实例
    private MyActivity myActivity;

    private byte[] directiveSart=new byte[]{(byte)0x04,(byte)0xFF,(byte)0x08,(byte)0x08,(byte)0xFB};
    private byte[] directiveStop=new byte[]{(byte)0x04,(byte)0x00,(byte)0x08,(byte)0x08,(byte)0x04};

    public AirConditioner(MyActivity activity){myActivity=activity;}

    public void start(){
        myActivity.bluetooth.connectBLE.writeByte(directiveSart);
        myActivity.airConditioningStatus="on";
        myActivity.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myActivity.airConditioningStatus+",'direction':'up'}"));
    }

    public void stop(){
        myActivity.bluetooth.connectBLE.writeByte(directiveStop);
        myActivity.airConditioningStatus="off";
        myActivity.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myActivity.airConditioningStatus+",'direction':'up'}"));
    }

    public void airConditionerAutoCtrl(float temperature){
        if(myActivity.autoTemControl&&((temperature>=myActivity.maxTem&&myActivity.airConditioningStatus.equals("off"))||(temperature>=myActivity.temValue&&temperature>=(myActivity.maxTem+0.5)))){
            myActivity.airConditioner.start();
            Log.d(myActivity.TAG, "auto start");
        }
        if(myActivity.autoTemControl&&((temperature<=myActivity.minTem&&myActivity.airConditioningStatus.equals("on"))||(temperature<myActivity.temValue&&temperature<=(myActivity.minTem-0.5)))){
            myActivity.airConditioner.stop();
            Log.d(myActivity.TAG, "auto stop");
        }
    }
}
