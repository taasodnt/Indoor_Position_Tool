package com.example.btle_scanner03;

import android.util.Log;

public class RssiMeanValue {

    String mac_address;
    private double rssi;
    private double numberOfData;
    public RssiMeanValue(String mac_address,int rssi) {
        this.mac_address = mac_address;
        this.rssi = rssi;
        numberOfData = 1;
    }

    public void add(int rssi) {
        numberOfData += 1;
        this.rssi += rssi;
    }

    public String getMac_address() {
        return mac_address;
    }

    public double getMeanValueOfRssi() {
        double meanValue = rssi/numberOfData;
        Log.d(RssiMeanValue.class.getSimpleName(),"rssi: "+rssi+"  numberOfData: "+numberOfData+"  meanValue: "+meanValue);
        return meanValue;
    }
}
