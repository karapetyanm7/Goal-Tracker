package com.example.goaltracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;

public class GoalTrackerApp extends Application {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            initializeTheme();
        } catch (Exception e) {
            Log.e("GoalTrackerApp", "Error initializing theme", e);
        }
    }
    
    private void initializeTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(ThemeManager.KEY_DARK_MODE, false);
        

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
} 