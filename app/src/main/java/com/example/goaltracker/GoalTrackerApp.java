package com.example.goaltracker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class GoalTrackerApp extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        initializeTheme();
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