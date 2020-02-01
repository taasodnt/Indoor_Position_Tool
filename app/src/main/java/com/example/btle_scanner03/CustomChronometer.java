package com.example.btle_scanner03;

import android.content.Context;
import android.os.SystemClock;
import android.widget.Chronometer;

import java.lang.ref.WeakReference;

public class CustomChronometer extends Chronometer {
    public CustomChronometer(Context context) {
        super(context);
    }

    @Override
    public void setBase(long base) {
        super.setBase(base);
    }

    private long getElapsedMillis() {
        return SystemClock.elapsedRealtime() - this.getBase();
    }

    public String getElapsedTime() {
        long elapsedMillis = getElapsedMillis();
        long timeInSeconds= elapsedMillis / 1000;
        long hours = timeInSeconds / 3600 ;
        timeInSeconds = timeInSeconds - (hours * 3600);
        long minutes = timeInSeconds / 60 ;
        timeInSeconds = timeInSeconds - (minutes * 60);
        String minutesString = String.format("%02d",minutes);
        String secondString = String.format("%02d",timeInSeconds);
        String elapsedTime = minutesString + ":" + secondString;

        return elapsedTime;
    }
}
