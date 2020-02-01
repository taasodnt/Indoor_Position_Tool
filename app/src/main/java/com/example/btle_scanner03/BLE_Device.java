package com.example.btle_scanner03;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class BLE_Device  {
    private double rssi;
    private double sumOfRssi;
    private BluetoothDevice bluetoothDevice;
    private int numberOfDataOfTheDevice;

    public BLE_Device(BluetoothDevice bluetoothDevice,int rssi) {
        this.rssi = rssi;
        sumOfRssi = rssi;
        this.bluetoothDevice = bluetoothDevice;
        numberOfDataOfTheDevice = 1;
    }

    public double getRssi() {
        return rssi;
    }
    public void setRssi(double new_rssi) {
        this.rssi = new_rssi;
        sumOfRssi = sumOfRssi+rssi;
        numberOfDataOfTheDevice = numberOfDataOfTheDevice + 1;
    }
    public String getName()  {
        return bluetoothDevice.getName();
    }
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }
    public double getRssiMeanValue(){
        double meanValue = sumOfRssi/numberOfDataOfTheDevice;
        return meanValue;
    }
    public double getNumberOfDataOfTheDevice() {
        return numberOfDataOfTheDevice;
    }
    public double getSumOfRssi() {
        return sumOfRssi;
    }
}
