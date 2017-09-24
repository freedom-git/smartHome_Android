package com.gephenom.smarthome.devices;

import android.util.Log;

import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.main.MyApplicationClass;
import com.gephenom.smarthome.tools.Tools;

/**
 * Created by freedom on 2016/9/19 0019.
 */
public class AirConditioner {


    private  MyApplicationClass myApplication;

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



    public AirConditioner(MyApplicationClass myapplicationClass){myApplication=myapplicationClass;}

    public void start(){
        myApplication.bluetooth.mBluetoothLeService.writeByte(directiveSart);
        myApplication.airConditioningStatus="on";
        myApplication.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myApplication.airConditioningStatus+",'direction':'up'}"));
    }

    public void stop(){
        myApplication.bluetooth.mBluetoothLeService.writeByte(directiveStop);
        myApplication.airConditioningStatus="off";
        myApplication.airConditioningWindAutoDirection=false;
        myApplication.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'airConditioningStatus','airConditioningStatus':"+myApplication.airConditioningStatus+",'direction':'up'}"));
    }

    public void addTemperature(){
        myApplication.settingTem++;
        myApplication.bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte)0x06,myApplication.settingTem,(byte)0x08,(byte)0x08,(byte)((byte)0x06 ^ myApplication.settingTem)});
    }

    public void reduceTemperature(){
        myApplication.settingTem--;
        myApplication.bluetooth.mBluetoothLeService.writeByte(new byte[]{(byte)0x06,myApplication.settingTem,(byte)0x08,(byte)0x08,(byte)((byte)0x06 ^ myApplication.settingTem)});
    }

    public void changeMode(String modeName){
        switch (modeName){
            case "coldMode":
                myApplication.bluetooth.mBluetoothLeService.writeByte(coldMode);
                break;
            case "windMode":
                myApplication.bluetooth.mBluetoothLeService.writeByte(windMode);
                break;
            case "reduceHumidityMode":
                myApplication.bluetooth.mBluetoothLeService.writeByte(reduceHumidityMode);
                break;
            case "heatMode":
                myApplication.bluetooth.mBluetoothLeService.writeByte(heatMode);
                break;
        }
        myApplication.airConditioningMode=modeName;
    }

    public void windDirection(){
        myApplication.bluetooth.mBluetoothLeService.writeByte(windDirection);
        myApplication.airConditioningWindAutoDirection=false;
    }

    public void windDirectionAuto(){
        myApplication.bluetooth.mBluetoothLeService.writeByte(windDirectionAuto);
        myApplication.airConditioningWindAutoDirection=!myApplication.airConditioningWindAutoDirection;
    }

    public void changeWindGrade(int windGrade){
        switch (windGrade){
            case 1:
                myApplication.bluetooth.mBluetoothLeService.writeByte(wind1);
                break;
            case 2:
                myApplication.bluetooth.mBluetoothLeService.writeByte(wind2);
                break;
            case 3:
                myApplication.bluetooth.mBluetoothLeService.writeByte(wind3);
                break;
        }
        myApplication.airConditioningWindGrade=windGrade;
    }

    public void airConditionerAutoCtrl(float temperature){
        if(myApplication.autoTemControl&&((temperature>=myApplication.maxTem&&myApplication.airConditioningStatus.equals("off"))||(temperature>=myApplication.temValue&&temperature>=(myApplication.maxTem+0.5)))){
            myApplication.airConditioner.start();
            Log.d(myApplication.TAG, "auto start");
        }
        if(myApplication.autoTemControl&&((temperature<=myApplication.minTem&&myApplication.airConditioningStatus.equals("on"))||(temperature<myApplication.temValue&&temperature<=(myApplication.minTem-0.5)))){
            myApplication.airConditioner.stop();
            Log.d(myApplication.TAG, "auto stop");
        }
    }
}
