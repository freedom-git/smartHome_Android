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
import com.gephenom.smarthome.main.MyActivity;
import com.gephenom.smarthome.tools.StringConverter;
import com.gephenom.smarthome.tools.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.Context.BIND_AUTO_CREATE;

public class Bluetooth {

    private  String TAG = "freedom";
    //socketIo实例
    private SocketIo socketIo;
    private MyActivity myActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    //public ConnectedThread connectBLE = null;
    public connectBLE connectBLE = null;

    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress = "5C:F8:21:87:C6:1A";
    private boolean mConnected = false;

    private int rec_flag=0,rec_num=0; //接受状态位,接收长度位
    private String[] RecvData = new String [3]; //接收三字节缓存

    public boolean 	Is_connect = false;
//    private byte[] buffer = new byte[10];  // buffer store for the stream
//    private int temp_connect; // bytes returned from read()
//    private String str_recive="";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                myActivity.finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public Bluetooth(MyActivity activity, SocketIo socket){
        socketIo=socket;
        myActivity=activity;
        //startBluetooth();
        startBLE();
    }

    //开启蓝牙4.0
    public void startBLE(){
        checkBleAvailability();


        Intent gattServiceIntent = new Intent(myActivity, BluetoothLeService.class);
        myActivity.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);





    }

    private void checkBleAvailability(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!myActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(myActivity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            myActivity.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) myActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(myActivity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            myActivity.finish();
            return;
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                myActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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
                myActivity.addLogToScreen("蓝牙已连接");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                myActivity.addLogToScreen("蓝牙已断开");
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
            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'humidityUpdate','humidity':"+humidity+",'direction':'up'}"));
            myActivity.humValue=humidity;
            Log.d(TAG, "humidity is: "+ humidity);
        }
        if (RecvData[0].equals("01")) {
            float temperature=(float)(Integer.parseInt(RecvData[1],16)*256+Integer.parseInt(RecvData[2],16))/10;
            socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureUpdate','temperature':"+temperature+",'direction':'up'}"));
            Log.d(TAG, "temperature is: "+ temperature);
            myActivity.airConditioner.airConditionerAutoCtrl(temperature);
            myActivity.temValue=temperature;
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
        myActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void onPause(){
        //myActivity.unregisterReceiver(mGattUpdateReceiver);
    }

    public void onDestroy(){
        myActivity.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    public class connectBLE {
        public void writeByte(byte[] bytes){
            Log.d(TAG, bytes.toString());
        }
    }












//    public void startBluetooth() {
//
//        /*1:获取本地BlueToothAdapter*/
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if(mBluetoothAdapter == null)
//        {
//            Log.i(TAG, "BluetoothAdapter == null");
//            return;
//        }
//        if(!mBluetoothAdapter.isEnabled())
//        {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            myActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//
//        connectBLE = new ConnectedThread(mBluetoothAdapter);
//        connectBLE.start();
//
//
//    }
//
//    public class ConnectedThread extends Thread {
//
//
//
//        private BluetoothSocket btSocket = null;
//
//        private OutputStream outStream = null;
//
//        private InputStream inStream = null;
//
//        private  String TAG = "freedom";
//
//        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  //这条是蓝牙串口通用的UUID，不要更改
//
//        private String address = "20:16:04:11:98:97"; // <==要连接的蓝牙设备MAC地址
//
//        private BluetoothAdapter mBluetoothAdapter = null;
//
//        private int rec_flag=0,rec_num=0; //接受状态位,接收长度位
//
//        private String[] RecvData = new String [3]; //接收三字节缓存
//
//
//        public ConnectedThread(BluetoothAdapter BluetoothAdapterParam) {
//        /*1:获取本地BlueToothAdapter*/
//            mBluetoothAdapter = BluetoothAdapterParam;
//        }
//
//        private void connect(){
//            /*2:获取远程BlueToothDevice*/
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//            Log.i(TAG, device.toString());
//            if(mBluetoothAdapter == null)
//            {
//                Log.i(TAG, "Can't get remote device.");
//                return;
//            }
//
///*3:获得Socket*/
//            try {
//                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
//                Log.i(TAG, "btSocket");
//            } catch (IOException e) {
//
//                Log.e(TAG, "ON RESUME: Socket creation failed.", e);
//
//            }
//
///*4:取消discovered节省资源*/
//            mBluetoothAdapter.cancelDiscovery();
//
//
///*5:连接*/
//
//            try {
//                Log.i(TAG, "connecting");
//                btSocket.connect();
//                Log.i(TAG, "connect");
//                Log.i(TAG, "ON RESUME: BT connection established, data transfer link open.");
//                Is_connect=true;
//            } catch (IOException e) {
//                Log.i(TAG, "IOException",e);
//                try {
//                    btSocket.close();
//                    Log.i(TAG, "close");
//                } catch (IOException e2) {
//                    Log .e(TAG,"ON RESUME: Unable to close socket during connection failure", e2);
//                }
//            }
//
//// Create a data stream so we can talk to server.
//
//            try {
//                outStream = btSocket.getOutputStream();
//
//                inStream = btSocket.getInputStream();
//
//            } catch (IOException e) {
//                Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
//            }
//
//
//
//
//
//           writeByte(new byte[]{(byte)0x02,(byte)0x00,(byte)0x2F,(byte)0x08,(byte)0x25});
//            writeByte(new byte[]{(byte)0xFF,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xFF});
//
//        }
//
//
//        public void run() {
//            connect();
//            //Keep listening to the InputStream until an exception occurs
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    if (Is_connect){
//                        // Read from the InputStream
//                        temp_connect = inStream.read(buffer,0,buffer.length);
//                        if (temp_connect != -1) {
//                            String SBUF;
//                            String St= StringConverter.byte2HexStr(buffer);
//                            //Log.d(TAG, "String St:"+St);
//                            str_recive	= St.substring(0, 2*temp_connect);
//                            Log.d(TAG, "str_recive:"+str_recive);
//                            for(int i=0;i<temp_connect;i++)	{
//                                SBUF= St.substring(2*i, 2*(i+1));
//                                //Log.d(TAG, "SBUF "+i+':'+ SBUF);
//                                if(rec_flag==0)		{
//                                    if(SBUF.equalsIgnoreCase("FF"))	{
//                                        //Log.d(TAG, "data:start");
//                                        rec_flag=1;
//                                        rec_num=0;
//                                    }
//                                }
//                                else{
//                                    if(SBUF.equalsIgnoreCase("FF")){
//                                        if(rec_num==3){
//                                            Log.d(TAG, "received data: "+ RecvData[0]+' '+RecvData[1]+' '+RecvData[2]);
//                                            DataDecode();
//                                            rec_flag=0;
//                                        }
//                                        rec_num=0;
//                                    }
//                                    else{
//                                        //Log.d(TAG, "data: "+rec_num+' '+ buffer[i]);
//                                        if(rec_num>2){
//                                            rec_num=0;
//                                            return;
//                                        }
//
//                                        RecvData[rec_num]=SBUF;
//                                        rec_num++;
//                                    }
//                                }
//                            }
//                        }
//                        else Log.d("wifirobot", "recive datas: -1 ");
//                    }
//                    Thread.sleep(1);
//                } catch (IOException e) {
//                    Log.d(TAG, "IOException ",e);
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        private void DataDecode() {
//            if (RecvData[0].equals("00")) {
//                float humidity=(float) (Integer.parseInt(RecvData[1],16)*256+Integer.parseInt(RecvData[2],16))/10;
//                socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'humidityUpdate','humidity':"+humidity+",'direction':'up'}"));
//                myActivity.humValue=humidity;
//                Log.d(TAG, "humidity is: "+ humidity);
//            }
//            if (RecvData[0].equals("01")) {
//                float temperature=(float)(Integer.parseInt(RecvData[1],16)*256+Integer.parseInt(RecvData[2],16))/10;
//                socketIo.mSocket.emit("airCtrl", Tools.getJsonFromString("{'event':'temperatureUpdate','temperature':"+temperature+",'direction':'up'}"));
//                Log.d(TAG, "temperature is: "+ temperature);
//                myActivity.airConditioner.airConditionerAutoCtrl(temperature);
//                myActivity.temValue=temperature;
//            }
//
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        public void write(String message) {
//            try {
//                byte[] msgBuffer = message.getBytes();
//                outStream.write(msgBuffer);
//            } catch (IOException e) {
//                Log.e(TAG, "ON RESUME: Exception during write.", e);
//            }
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        public void writeByte(byte[] msgBuffer) {
//            if(Is_connect) {
//                try {
//                    outStream.write(msgBuffer);
//                } catch (IOException e) {
//                    Log.e(TAG, "ON RESUME: Exception during write.", e);
//                }
//            }
//        }
//
//        /* Call this from the main activity to shutdown the connection */
//        public void cancel() {
//            try {
//                if(!(outStream==null)){outStream.close();}
//                if(!(btSocket==null)){btSocket.close();}
//            } catch (IOException e) {}
//        }
//
//
//
//
//
//
//    }


}
