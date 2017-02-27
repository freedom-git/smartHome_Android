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

    private byte[] coldMode=new byte[]{(byte)0x05,(byte)0x01,(byte)0x08,(byte)0x08,(byte)0x04};
    private byte[] windMode=new byte[]{(byte)0x05,(byte)0x03,(byte)0x08,(byte)0x08,(byte)0x06};
    private byte[] reduceHumidityMode=new byte[]{(byte)0x05,(byte)0x02,(byte)0x08,(byte)0x08,(byte)0x07};
    private byte[] heatMode=new byte[]{(byte)0x05,(byte)0x04,(byte)0x08,(byte)0x08,(byte)0x01};

    private byte[] windDirection=new byte[]{(byte)0x08,(byte)0x01,(byte)0x08,(byte)0x08,(byte)0x09};
    private byte[] windDirectionAuto=new byte[]{(byte)0x08,(byte)0x00,(byte)0x08,(byte)0x08,(byte)0x08};

    private byte[] wind1=new byte[]{(byte)0x07,(byte)0x01,(byte)0x08,(byte)0x08,(byte)0x06};
    private byte[] wind2=new byte[]{(byte)0x07,(byte)0x02,(byte)0x08,(byte)0x08,(byte)0x05};
    private byte[] wind3=new byte[]{(byte)0x07,(byte)0x03,(byte)0x08,(byte)0x08,(byte)0x04};



    public AirConditioner(MyActivity activity){myActivity=activity;}

    public void start(){
        myActivity.bluetooth.mBluetoothLeService.writeByte(directiveSart);
        myActivity.airConditioningStatus="on";
        myActivity.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myActivity.airConditioningStatus+",'direction':'up'}"));
    }

    public void stop(){
        myActivity.bluetooth.mBluetoothLeService.writeByte(directiveStop);
        myActivity.airConditioningStatus="off";
        myActivity.airConditioningWindAutoDirection=false;
        myActivity.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myActivity.airConditioningStatus+",'direction':'up'}"));
    }

    public void addTemperature(){
        myActivity.settingTem++;
        myActivity.bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte)0x06,myActivity.settingTem,(byte)0x08,(byte)0x08,(byte)((byte)0x06 ^ myActivity.settingTem)});
    }

    public void reduceTemperature(){
        myActivity.settingTem--;
        myActivity.bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte)0x06,myActivity.settingTem,(byte)0x08,(byte)0x08,(byte)((byte)0x06 ^ myActivity.settingTem)});
    }

    public void changeMode(String modeName){
        switch (modeName){
            case "coldMode":
                myActivity.bluetooth.mBluetoothLeService.writeByte(coldMode);
                break;
            case "windMode":
                myActivity.bluetooth.mBluetoothLeService.writeByte(windMode);
                break;
            case "reduceHumidityMode":
                myActivity.bluetooth.mBluetoothLeService.writeByte(reduceHumidityMode);
                break;
            case "heatMode":
                myActivity.bluetooth.mBluetoothLeService.writeByte(heatMode);
                break;
        }
        myActivity.airConditioningMode=modeName;
    }

    public void windDirection(){
        myActivity.bluetooth.mBluetoothLeService.writeByte(windDirection);
        myActivity.airConditioningWindAutoDirection=false;
    }

    public void windDirectionAuto(){
        myActivity.bluetooth.mBluetoothLeService.writeByte(windDirectionAuto);
        myActivity.airConditioningWindAutoDirection=!myActivity.airConditioningWindAutoDirection;
    }

    public void changeWindGrade(int windGrade){
        switch (windGrade){
            case 1:
                myActivity.bluetooth.mBluetoothLeService.writeByte(wind1);
                break;
            case 2:
                myActivity.bluetooth.mBluetoothLeService.writeByte(wind2);
                break;
            case 3:
                myActivity.bluetooth.mBluetoothLeService.writeByte(wind3);
                break;
        }
        myActivity.airConditioningWindGrade=windGrade;
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
