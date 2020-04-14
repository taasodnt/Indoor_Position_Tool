package com.example.btle_scanner03;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class BLE_ScanningService extends Service {
    private String uriAPI = "http://163.18.53.144/F459/PHP/beacon/httpPostTest.php"; //所有訊號的資訊
    private String uriAPI_2 = "http://163.18.53.144/F459/PHP/beacon_result/PhonePJ_getJson.php"; //只有平均值
    private String uriAPI_3 = "http://163.18.53.144/F459/PHP/beacon_result/compare.php";
    private String wifiWeb = "http://163.18.53.144/F459/PHP/WIFI/PhonePJ_GetWifi.php";
//    private String[] DEVICE_ADDRESSES = {
//            "20:91:48:21:79:2C",
//            "20:91:48:21:7E:65",
//            "20:91:48:21:7E:57",
//            "20:91:48:21:92:0E",
//            "20:91:48:21:88:84",
//            "20:91:48:21:47:66",
//            "FF:FF:00:06:A1:BB",
//            "FF:FF:00:04:EA:15",
//            "FF:FF:00:05:8A:AD"};
    private String[] deviceaddress;
//    public static final String SCAN_DATA = "SCAN_DATA";
    private static int SCAN_LABEL = 1;
    public static final long SCAN_PERIOD = 10000;
    protected static final int REFRESH_DATA = 0x00000001;
//    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
//    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static boolean SERVICE_STATE_ON = false;

    private static final String TAG = BLE_ScanningService.class.getSimpleName();
    public static final String UPDATE_LIST_ACTION = "UPDATE_LIST_ACTION";
    public static final String SHOW_RESULT = "SHOW_RESUTL";
    public static final String DEVICE_LIST = "DEVICE_LIST";
    public static final String COMPARE_RESULT = "COMPARE_RESULT";
    private static final int FOREGROUND_SERVICE_CHANNEL_ID = 1;

    private List<ScanFilter> bleScanFilter;

//    private HashMap<String,BLE_Device> ble_deviceHashMap;
    private HashMap<String,RssiMeanValue> meanValueHashMap;
    private ArrayList<String> scanList = new ArrayList<>();
    private boolean isLab;
    private boolean isCompare;

    private static BluetoothAdapter bluetoothAdapter = null;
//    private static boolean transmissionState = false;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private Handler scanHandler;
    private final Object controlScanList = new Object();
    private ScanCallback bleScanCallback;
    private long scanPeriod;
    private CustomChronometer chronometer;
//    private BroadcastReceiver receiver;
//    private WifiManager wifiManager;
    private ScanSettings scanSettings = generateScanSetting();
    private SimpleMacAddress simpleMacAddress;
//    private WeakHandler mHandler = null;
    private int threadCount = 0;
    private ArrayList<android.bluetooth.le.ScanResult> scanResults;


    private static class SendingRunnable implements Runnable {
        private final WeakReference<BLE_ScanningService> serviceObject;
        private final BLE_ScanningService service;

        private SendingRunnable(BLE_ScanningService service){
            serviceObject = new WeakReference<>(service);
            this.service = serviceObject.get();
        }
        @Override
        public void run() {
            if(service.scanList.size() != 0){
                Log.d("scanListSize",Integer.toString(service.scanList.size()));
                String result;
                String mac = service.simpleMacAddress.getMacAddress();
                synchronized (service.controlScanList) {
                    if(service.isLab){
                        for (String data:service.scanList) {
                            String num2= mac+" "+data;
                            Log.d("num2:",num2);
                            result = service.sendPostDataToInternet(service.uriAPI,num2);
                            if(result == null) {
                                Log.d("ouo","Result is Null Pointer");
                            }else{
                                Log.d("ouo",result);
                            }
                        }
                    }
                    if(service.isCompare){
                        String data = "[";
                        Log.d("num3",String.format("%d",service.meanValueHashMap.size()));
                        for(String mac_address:service.meanValueHashMap.keySet()) {
                            RssiMeanValue rssiMeanValueObject = service.meanValueHashMap.get(mac_address);
                            if(rssiMeanValueObject != null){
                                String meanValue = String.format("%.3f",rssiMeanValueObject.getMeanValueOfRssi());

//                                String data = mac + " " +  mac_address + " : " +  meanValue +" " +  SCAN_LABEL +"(Mean value)";
                                data = data + "{\"PhoneMac\":"+"\""+mac+"\","+"\"BeaconMac\":"+"\""+mac_address+"\","+"\"Rssi\":"+"\""+meanValue+"\"},";
                                Log.d("num3",data);
////                                result = service.sendPostDataToInternet(service.uriAPI_2,data);
//                                if(result == null) {
//                                    Log.d("ouo","Result is Null Pointer");
//                                }else{
//                                    Log.d("ouo",result);
//                                    Log.d("test",data);
                            }
                        }
//                        if (data != null && data.length() > 0 && data.charAt(data.length() - 1) == ',') {
//                            data = data.substring(0, data.length() - 1);
//                        }
                        data = data + "{\"PhoneMac\":"+"\""+mac+"\""+",\"BreakPoint\":\"End\"}";
                        data = data + "]";
                        Log.d("final num3",data);
                        String compareResult= service.sendPostDataToInternet(service.uriAPI_2,data);
                        if(compareResult != null && compareResult != ""){
                            Log.d("compareResult",compareResult);
                            service.sendResultToActivity(compareResult);
                        }
                        SCAN_LABEL++;
                        service.meanValueHashMap.clear();
                    }
                }
                service.scanList.clear();
            }
        }
    }

    private SendingRunnable sendingRunnable = new SendingRunnable(this);

    private static class MyRunnable implements Runnable {
        private final WeakReference<BLE_ScanningService> serviceObject;
        private BLE_ScanningService service;

        private MyRunnable(BLE_ScanningService service){
            this.serviceObject = new WeakReference<>(service);
            this.service = this.serviceObject.get();
        }

        @Override
        public void run() {
            service.scanHandler.postDelayed(service.stopRunnable,service.scanPeriod);
//            service.mHandler.postDelayed(service.stopRunnable,service.scanPeriod);
            service.bluetoothLeScanner.startScan(service.bleScanFilter,service.scanSettings,service.bleScanCallback);
            service.threadCount = Thread.activeCount();
            Log.d("Number of thread:",""+service.threadCount);
            Log.d("leaking test","Runnable OK");
        }
    }

    private static class  MyStopRunnable implements Runnable{
        private final WeakReference<BLE_ScanningService> serviceObject;
        private final BLE_ScanningService service;

        private MyStopRunnable(BLE_ScanningService service){
            this.serviceObject = new WeakReference<>(service);
            this.service = serviceObject.get();
        }

        @Override
        public void run() {
            service.bluetoothLeScanner.stopScan(service.bleScanCallback);
            Log.d(TAG,"Stop");
            service.handleScanResults();
            service.update();
            Log.d("leaking test","StopRunnable OK");
            if(service.scanHandler != null) {
                service.scanHandler.post(service.runnable);
            }
        }
    }

    private final MyRunnable runnable = new MyRunnable(this);
    private final MyStopRunnable stopRunnable = new MyStopRunnable(this);

    private HandlerThread handlerThread = new HandlerThread("Scan HandlerThread");


    private void handleScanResults(){
        for(android.bluetooth.le.ScanResult result : scanResults){
            int rssi = result.getRssi();
            String mac_address = result.getDevice().getAddress();
            String data = mac_address + " " + rssi + " " +chronometer.getElapsedTime();

 //           synchronized (controlScanList) {
                scanList.add(data);
                if(meanValueHashMap.containsKey(mac_address)) {
                    RssiMeanValue rssiMeanValue = meanValueHashMap.get(mac_address);
                    if(rssiMeanValue != null){
                        rssiMeanValue.add(rssi);
                    }
                }else{
                    RssiMeanValue rssiMeanValueObject = new RssiMeanValue(mac_address,rssi);
                    meanValueHashMap.put(rssiMeanValueObject.getMac_address(),rssiMeanValueObject);
                }
                Log.d("ouo","Added message" + data);
//            }
        }
        scanResults.clear();
    }


//    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
//            TEST_COUNTER ++;
//            Log.d("TestCounter: ",Integer.toString(TEST_COUNTER));
//            String dataString = bluetoothDevice.getAddress()+" "+rssi;
//            Log.d(TAG,dataString);
//            update(dataString);
//        }
//    };
//    private void setArrayListAndDeviceListFile() {
//        arrayList.clear();
//
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()) {
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            String name  = tmpDevice.getName();
//            String address = tmpDevice.getAddress();
//            String rssi = Double.toString(tmpDevice.getRssi());
//            String tmpString = "Mac:"+address+"Name:"+name+"Rssi:"+rssi;
//           update(tmpString);
//            Log.d(TAG,tmpString);
//            Log.d(TAG,address+" Debug Counter: "+tmpDevice.getNumberOfDataOfTheDevice());
//            Log.d(TAG,"Sum of rssi: "+tmpDevice.getSumOfRssi());
//            printStream.println(tmpString);
//            arrayList.add(tmpString);
//        }
//        printStream.close();
//    }


    private void setChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.setFormat("MM:SS");
    }

    private void setBleScanFilter() {
        bleScanFilter = new ArrayList<>();
        for(String macAddress:deviceaddress){
            bleScanFilter.add(new ScanFilter.Builder().setDeviceAddress(macAddress).build());
        }
    }

    private void sendResultToActivity(String result) {
        Log.d("broadcast test","sendResultToActicity get called");

        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(SHOW_RESULT);
        broadCastIntent.putExtra(COMPARE_RESULT,result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadCastIntent);
    }

//    private void showDetail() {
//        arrayList.clear();
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()) {
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            String name  = tmpDevice.getName();
//            String address = tmpDevice.getAddress();
//            String rssi = Double.toString(tmpDevice.getRssi());
//            String tmpString = "Mac:"+address+"Name:"+name+"Rssi:"+rssi;
//            Log.d(TAG,"FromZ showDetail: "+tmpString+address+"  Debug Counter: "+tmpDevice.getNumberOfDataOfTheDevice()+"  Sum of rssi: "+tmpDevice.getSumOfRssi());
//            arrayList.add(tmpString);
//        }
//        Log.d(TAG,"FromZ showDetail:  End");
//    }
//
//    private void sendData(){
//        Log.d(TAG,"SendData get called");
//        if(ble_deviceHashMap.isEmpty()){
//            Log.d(TAG,"BLE_HashMap is empty!");
//            return;
//        }
//        for(String ble_deviceAddress: ble_deviceHashMap.keySet()){
//            BLE_Device tmpDevice = ble_deviceHashMap.get(ble_deviceAddress);
//            double rssiMeanValue = tmpDevice.getRssiMeanValue();
//               String name  = tmpDevice.getName();
//               String address = tmpDevice.getAddress();
//               String numberOfDataOfTheDevice = Double.toString(tmpDevice.getNumberOfDataOfTheDevice());
//               String sumOfRssi = Double.toString(tmpDevice.getSumOfRssi());
//               String tmpString = "Mac:"+address+" Name:"+name+" Rssi:"+rssiMeanValue+"  NumberOfDataOfTheDevice:"+numberOfDataOfTheDevice+"  Sum Of Rssi:"+sumOfRssi;
//               Log.d(TAG,"From sendData:  Mean Value: "+tmpString);
//               update(tmpString);
//        }
//        Log.d(TAG,"FromZ sendData:  End");
//    }
//    private void setBle_deviceHashMap(BluetoothDevice device,int rssi){
//        String address = device.getAddress();
//        if(!ble_deviceHashMap.containsKey(address)) {
//            BLE_Device myDevice = new BLE_Device(device,rssi);
//            ble_deviceHashMap.put(address,myDevice);
//        }else{
//            ble_deviceHashMap.get(address).setRssi(rssi*1.0);
//        }
//    }

    private static class MyScanCallback extends ScanCallback {
        private final WeakReference<BLE_ScanningService> serviceObject;
        private final BLE_ScanningService service;

        private MyScanCallback(BLE_ScanningService service){
            serviceObject = new WeakReference<>(service);
            this.service = serviceObject.get();
        }

        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);

            synchronized (service.controlScanList){
                service.scanResults.add(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG,"Scanning Failed Error Code:"+errorCode);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if(bluetoothLeScanner == null){
            stopSelf();
        }
//        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
//        meanValueHashMap = new HashMap<>();
        SERVICE_STATE_ON = true;
//        scanPeriod = SCAN_PERIOD;
//        ble_deviceHashMap = new HashMap<>();
//        try {
//            printStream = new PrintStream(openFileOutput("DeviceList.txt",MODE_PRIVATE));
//        } catch (FileNotFoundException e) {
//            Log.d(TAG,"Output Error!");
//            e.printStackTrace();
//        }
//        IntentFilter scanWifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                Log.d(TAG,"Receiver get called");
//                String action = intent.getAction();
//                if(action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION){
//                    if(wifiManager.getScanResults().size() != 0) {
//                        for(ScanResult result: wifiManager.getScanResults()) {
//                            //TODO 回傳wifi掃描結果
////                            String SSID = result.SSID;
////                            Log.d(TAG,SSID);
//                        }
//                    }else{
//                        Log.d(TAG,"Result is empty");
//                    }
//                    Log.d(TAG,"Receiver is finishing");
////                    wifiManager.startScan();
//                }
//            }
//        };
//        registerReceiver(receiver,scanWifiFilter);

        meanValueHashMap = new HashMap<>();

        handlerThread.start();

        scanHandler = new Handler(handlerThread.getLooper());

        bleScanCallback = new MyScanCallback(this);

        chronometer = new CustomChronometer(this);
        setChronometer();
        chronometer.start();

        simpleMacAddress = new SimpleMacAddress(getApplicationContext());

//        mHandler = new WeakHandler(this);

        scanResults = new ArrayList<>();


        Log.d(TAG,"Service on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//            wifiManager.startScan();
//        bluetoothAdapter.startLeScan(leScanCallback);
        if(intent != null){
            boolean[] tmpList = intent.getBooleanArrayExtra(MainActivity.SCAN_OPTION);
            if(tmpList != null){
                for (int i = 0; i<tmpList.length;i++) {
                    isLab = tmpList[0];
                    isCompare = tmpList[1];
                }
                scanPeriod = intent.getLongExtra(MainActivity.SCAN_INTERVAL_ID,SCAN_PERIOD);
                deviceaddress = intent.getStringArrayExtra(MainActivity.BEACON_LIST);
                setBleScanFilter();
                scanHandler.post(runnable);
            }else{
                Log.d("Service State","Service start failed");
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

//        unregisterReceiver(receiver);
//        bluetoothAdapter.stopLeScan(leScanCallback);
        scanHandler.removeCallbacks(runnable);
        scanHandler.removeCallbacks(stopRunnable);
        scanHandler.getLooper().quit();
        scanHandler = null;
//        mHandler.removeCallbacksAndMessages(runnable);
//        mHandler.removeCallbacksAndMessages(stopRunnable);
//        mHandler.removeCallbacksAndMessages(null);
        bluetoothLeScanner.stopScan(bleScanCallback);
        bluetoothLeScanner = null;
        bluetoothAdapter = null;
        clearScanList();
        chronometer.stop();
        Log.d(TAG,"Service is stop");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startForegroundService() {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,App.CHANNEL_ID);
        builder.setContentTitle("Scanning notification");
        builder.setContentTitle("Scanning Service");
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_android);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(FOREGROUND_SERVICE_CHANNEL_ID,notification);
    }

    private ScanSettings generateScanSetting(){
        ScanSettings.Builder scanSettingBuilder = new ScanSettings.Builder();
        scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        scanSettingBuilder.setReportDelay(0);
        return scanSettingBuilder.build();
    }

    private void clearScanList() {
        scanList.clear();
//        sendBroadCastToActivity();
    }



    private void update(){
        long sendThreadID;
        long uiThreadID;
        Thread thread = new Thread(sendingRunnable,"Sending Thread");
        thread.start();
        sendThreadID = thread.getId();
        uiThreadID = Thread.currentThread().getId();
        Log.d("thread id","Sending thread ID = "+sendThreadID);
        Log.d("thread id","Main thread ID = " + uiThreadID);
//        return thread.getId();
    }




    private String sendPostDataToInternet(String uri,String strTxt) {
        int state;
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpRequest = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("data", strTxt));

        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            state = httpResponse.getStatusLine().getStatusCode();
            Log.d("ouo","Error code:" +state);
            //狀態碼為200的狀況才會執行
            if (state == 200) {
                Log.d(BLE_ScanningService.class.getName(),strTxt);
                String strResult = EntityUtils.toString(httpResponse.getEntity());

                return strResult;
            }
        } catch (Exception e) {
            Log.d("ouo","exception ok");
            e.printStackTrace();
        }
        return null;
    }
}