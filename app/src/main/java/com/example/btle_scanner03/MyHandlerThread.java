package com.example.btle_scanner03;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class MyHandlerThread extends HandlerThread {

    public MyHandlerThread(String name) {
        super(name);

    }

}
