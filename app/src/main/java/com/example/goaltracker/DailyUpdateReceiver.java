package com.example.goaltracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DailyUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "DailyUpdateReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "DailyUpdateReceiver triggered - checking and updating habits");
        updateHabits(context);
    }
    

    private void updateHabits(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
        Set<String> habitSet = sharedPreferences.getStringSet("habits", new HashSet<>());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        

        Calendar calendar = Calendar.getInstance();

        long currentTime = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();

        int habitsUpdated = 0;
        

        for (String habitName : habitSet) {
            long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
            int currentStreak = sharedPreferences.getInt(habitName + "_streak", 0);
            

            if (lastMarkedTime < todayStart && currentStreak > 0) {
                long daysPassed = 1;
                if (lastMarkedTime > 0) {

                    Calendar lastMarkedCal = Calendar.getInstance();
                    lastMarkedCal.setTimeInMillis(lastMarkedTime);
                    lastMarkedCal.set(Calendar.HOUR_OF_DAY, 0);
                    lastMarkedCal.set(Calendar.MINUTE, 0);
                    lastMarkedCal.set(Calendar.SECOND, 0);
                    lastMarkedCal.set(Calendar.MILLISECOND, 0);
                    
                    long diffMillis = todayStart - lastMarkedCal.getTimeInMillis();
                    daysPassed = TimeUnit.MILLISECONDS.toDays(diffMillis);
                    

                    daysPassed = Math.max(1, daysPassed);
                }
                
                int currentPoints = sharedPreferences.getInt(habitName + "_points", 100);
                int deduction = (int) (5 * daysPassed);
                int newPoints = Math.max(0, currentPoints - deduction);

                editor.putInt(habitName + "_streak", 0);
                editor.putInt(habitName + "_points", newPoints);
                
                habitsUpdated++;
                
                Log.d(TAG, "Habit '" + habitName + "' was not completed for " + daysPassed + 
                      " days - resetting streak and deducting " + deduction + " points");
            }
        }
        

        editor.apply();
        
        Log.d(TAG, "Daily update completed - updated " + habitsUpdated + " habits");
    }
} 