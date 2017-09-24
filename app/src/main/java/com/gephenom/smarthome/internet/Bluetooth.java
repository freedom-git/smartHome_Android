package com.gephenom.smarthome.internet;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.gephenom.smarthome.R;
import com.gephenom.smarthome.main.MyApplicationClass;
import com.gephenom.smarthome.tools.StringConverter;
import com.gephenom.smarthome.tools.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.Context.BIND_AUTO_CREATE;

public class Bluetooth {

    private  String TAG = "freedom";
    private MyApplicationClass myApplication;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    //public ConnectedThread connectBLE = null;
//    public connectBLE connectBLE = null;

    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress = "5C:F8:21:87:C6:1A";
    private boolean mConnected = false;

    private int rec_flag=0,rec_num=0; //接受状态位,接收长度位
    private String[] RecvData = new String [3]; //接收三字节缓存

    public boolean 	Is_connect = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public Bluetooth(MyApplicationClass myapplicationClass,Activity activity){
        myApplication=myapplicationClass;
        startBLE(activity);
    }

    //开启蓝牙4.0
    public void startBLE(Activity activity){
        checkBleAvailability(activity);
        Intent gattServiceIntent = new Intent(myApplication, BluetoothLeService.class);
        myApplication.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }


    private void checkBleAvailability(Activity activity){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!myApplication.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(myApplication, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//            myApplication.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) myApplication.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(myApplication, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
//            myApplication.finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                   myApplication.addLog("蓝牙已连接");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                myApplication.addLog("蓝牙已断开");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, mBluetoothLeService.getSupportedGattServices().toString());
                mBluetoothLeService.setCharacteristicNotification(mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0), true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "dataToString:"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                getOrder(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void getOrder(String data){
            String SBUF;
            for(int i=0;i<data.length()/2;i++)	{
                SBUF= data.substring(2*i, 2*(i+1));
                if(rec_flag==0)		{
                    if(SBUF.equalsIgnoreCase("FF"))	{
                        //Log.d(TAG, "data:start");
                        rec_flag=1;
                        rec_num=0;
                    }
                }
                else{
                    if(SBUF.equalsIgnoreCase("FF")){
                        if(rec_num==3){
                            Log.d(TAG, "received data: "+ RecvData[0]+' '+RecvData[1]+' '+RecvData[2]);
                            checkOrder(RecvData);
                            rec_flag=0;
                        }
                        rec_num=0;
                    }
                    else{
                        //Log.d(TAG, "data: "+rec_num+' '+ buffer[i]);
                        if(rec_num>2){
                            rec_num=0;
                            return;
                        }

                        RecvData[rec_num]=SBUF;
                        rec_num++;
                    }
                }
            }
    }

    private void checkOrder(String[] RecvData){
        if (RecvData[0].equals("00")) {
            float humidity=(float) (Integer.parseInt(RecvData[1],16)*256+Integer.parseInt(RecvData[2],16))/10;
            myApplication.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'humidityUpdate','humidity':"+humidity+",'direction':'up'}"));
            myApplication.humValue=humidity;
            Log.d(TAG, "humidity is: "+ humidity);
        }
        if (RecvData[0].equals("01")) {
            float temperature=(float)(Integer.parseInt(RecvData[1],16)*256+Integer.parseInt(RecvData[2],16))/10;
            myApplication.socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureUpdate','temperature':"+temperature+",'direction':'up'}"));
            Log.d(TAG, "temperature is: "+ temperature);
            myApplication.airConditioner.airConditionerAutoCtrl(temperature);
            myApplication.temValue=temperature;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void onResume(){
        myApplication.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void onPause(){

    }

    public void onDestroy(){
        myApplication.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


}
