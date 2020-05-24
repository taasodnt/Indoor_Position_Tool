package com.example.btle_scanner03;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.icu.util.IslamicCalendar;

import java.util.HashMap;
import java.util.jar.Attributes;

public class SessionManager {
    public static final String PersonalInfoPref = "PersonalInfo";

    private static final String PhotoKey = "photo";
    private static final String NameKey = "name";
    private static final String IdKey = "id";
    public static Boolean IsLogin = false;

    private SharedPreferences sharedPreferences;
    private Context _context;
    private SharedPreferences.Editor editor;


    public SessionManager(Context context){
        this._context = context;
        sharedPreferences = _context.getSharedPreferences(PersonalInfoPref, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public  void createLoginSession(Bitmap rawImage,String name,String id){
        editor.putString(PhotoKey,String.valueOf(rawImage));
        editor.putString(NameKey,name);
        editor.putString(IdKey,id);
        editor.commit();
        IsLogin = true;
    }

    public HashMap<String,String> getUserDetail(){
        HashMap<String,String> user = new HashMap<>();
        user.put(PhotoKey,sharedPreferences.getString(PhotoKey,null));
        user.put(NameKey,sharedPreferences.getString(NameKey,null));
        user.put(IdKey,sharedPreferences.getString(IdKey,null));
        return user;
    }

    public void checkIsLogin(){
        if(!IsLogin){
            Intent loginIntent = new Intent(_context,LoginActivity.class);
            _context.startActivity(loginIntent);
        }
    }

    public void logOutUser(){
        editor.clear();
        editor.commit();
        IsLogin = false;
        Intent loginIntent = new Intent(_context,LoginActivity.class);
        _context.startActivity(loginIntent);
    }
}
