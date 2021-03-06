package com.example.btle_scanner03;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 2;
    private static int UPDATED_COUNTER = 0;
    private static final String SERVICE_STATE = "SERVICE_STATE";
    public static final String SCAN_INTERVAL_ID = "SCAN_INTERVAL";
    public static final String SCAN_OPTION = "SCAN_OPTION";
    public static final String BEACON_LIST = "BEACON_LIST";
    private String uriAPI_4 = "http://163.18.53.144/F459/PHP/beacon_result/PhonePJ_resultSave.php";
    private String beaconListAPI = "http://163.18.53.144/F459/php/beacon_result/PhonePJ_beaconList.php";
    private boolean serviceState;
    private Intent serviceIntent;
    private MyReceiver myReceiver;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> arrayAdapter;

    private LocationManager locationManager;


    private BluetoothAdapter bluetoothAdapter;
    private WifiManager wifiManager;

    private ListView deviceListView;
    private Button startAndStopBtn;
    private EditText scanIntervalET;

    private CheckBox labCB;
    private CheckBox compareCB;

    private TextView resultTV;

    private SimpleMacAddress simpleMacAddress;

    private LocalBroadcastManager localBroadcastManager;

    private String[] deviceAddresses;

    private int threadCount = 0;
    Set<Thread> excessThreads = null;
//    private Handler mHandler;

    private static class GetBeaconListRunnable implements Runnable {
        MainActivity activity;

        private GetBeaconListRunnable(MainActivity activity){
            WeakReference<MainActivity> activityWeakReference = new WeakReference<>(activity);
            this.activity = activityWeakReference.get();
        }
        @Override
        public void run() {
            String tmp;
            try{
                do{
                    tmp = activity.sendPostDataToInternet(activity.beaconListAPI,"");
                }while(tmp == null);
                activity.deviceAddresses = tmp.split(",");
                Log.d("get beacon list","get beacon list success");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.startAndStopBtn.setEnabled(true);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanIntervalET = findViewById(R.id.interval_et);
        //labCB = findViewById(R.id.lab_cb);
        compareCB = findViewById(R.id.compare_cb);
        resultTV = findViewById(R.id.result_tv);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission();
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            if(!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,REQUEST_CODE);
            }
        }else{
            finish();
        }
        wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        if(wifiManager == null){
            Toast.makeText(this, "This device doesn't support wifi service", Toast.LENGTH_SHORT).show();
            finish();
        }

        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        if(locationManager == null){
            Toast.makeText(this, "This device doesn't support location service", Toast.LENGTH_SHORT).show();
            finish();
        }else{
            boolean gpsProvider,networkProvider;
            gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(!gpsProvider && !networkProvider){
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(" Turn on Location Service")
                        .setMessage("The app needs location service")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(locationIntent);
                            }
                        })
                        .show();
            }
        }
//        Intent enableWifiIntent = new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);
//        startActivity(enableWifiIntent);

        serviceIntent = new Intent(this,BLE_ScanningService.class);

        serviceState = false;
        startAndStopBtn = findViewById(R.id.startAndStop);
        startAndStopBtn.setEnabled(false);
        deviceListView = findViewById(R.id.list_item);

        myReceiver = new MyReceiver(this);
        deviceList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,deviceList);
        deviceListView.setAdapter(arrayAdapter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_ScanningService.SHOW_RESULT);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(myReceiver,intentFilter);
//        registerReceiver(myReceiver,intentFilter);

        simpleMacAddress = new SimpleMacAddress(getApplicationContext());

        new Thread(new GetBeaconListRunnable(this)).start();

//        mHandler = initialHandler();
    }

    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SERVICE_STATE, serviceState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceState = savedInstanceState.getBoolean(SERVICE_STATE);
    }

    private void requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("These permission is needed for scan wifi and BLE signal")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_FINE_LOCATION);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_REQUEST_FINE_LOCATION);
        }
    }



    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(myReceiver);
        super.onDestroy();
    }


    public void startAndStop(View view) {
        if(BLE_ScanningService.SERVICE_STATE_ON == false){
            boolean[] options = {compareCB.isChecked()};
            BLE_ScanningService.SERVICE_STATE_ON = true;
            Log.d(TAG,"Button Ok1");
            long scanInterval = BLE_ScanningService.SCAN_PERIOD;
            if(!scanIntervalET.getText().toString().isEmpty()){
                scanInterval = Long.parseLong(scanIntervalET.getText().toString());
            }
            serviceIntent.putExtra(SCAN_OPTION,options);
            serviceIntent.putExtra(SCAN_INTERVAL_ID,scanInterval);
            serviceIntent.putExtra(BEACON_LIST, deviceAddresses);
            startService(serviceIntent);

        }else{
            BLE_ScanningService.SERVICE_STATE_ON = false;
            stopService(serviceIntent);
            threadCount = Thread.activeCount();
            Log.d("Number of thread:",""+threadCount);
            excessThreads = Thread.getAllStackTraces().keySet();
            for(Thread thread:excessThreads){
                Log.d("Excess Threads",thread.getName());
            }
            resultTV.setText("Result:");
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_CODE:
                Log.d("ouob",Integer.toString(resultCode));
                if(resultCode == RESULT_CANCELED) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ouob","coarse location permission granted");
                }else{
                    finish();
                }
                return;
            }
        }
    }

    public void correct(View view) {
        final String mac = simpleMacAddress.getMacAddress();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendPostDataToInternet(uriAPI_4,mac + " 1");
            }
        }).start();
    }

    public void wrong(View view) {
        final String mac = simpleMacAddress.getMacAddress();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendPostDataToInternet(uriAPI_4,mac + " 2");
            }
        }).start();
    }

    private static class MyReceiver extends BroadcastReceiver {
        private MainActivity mainActivity ;
        private MyReceiver(MainActivity myMainActivity){
            WeakReference<MainActivity> mainActivityWeakReference = new WeakReference<>(myMainActivity);
            mainActivity = mainActivityWeakReference.get();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive","receive broadcast");
            String action = intent.getAction();
            if (action == BLE_ScanningService.SHOW_RESULT){
                String result = intent.getStringExtra(BLE_ScanningService.COMPARE_RESULT);
                Log.d("compare result",result);
                mainActivity.resultTV.setText("Result: " + result + " " + UPDATED_COUNTER);
                UPDATED_COUNTER++;
            }
        }
    }

    private String sendPostDataToInternet(String uri,String strTxt) {
        int state;
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpRequest = new HttpPost(uri);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("data", strTxt));

        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            state = httpResponse.getStatusLine().getStatusCode();
            Log.d("ouo","Error code:" +state);
            //狀態碼為200的狀況才會執行
            if (state == 200) {
                Log.d("ouo",strTxt);
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