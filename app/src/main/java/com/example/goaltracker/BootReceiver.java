package com.example.goaltracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || 
                                          intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                                          intent.getAction().equals("android.intent.action.REBOOT"))) {
            Log.d(TAG, "Boot completed, restoring alarms");
            
            scheduleDailyHabitUpdate(context);

            SharedPreferences sharedPreferences = context.getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
            boolean hasReminders = sharedPreferences.getBoolean("has_reminders", false);
            
            if (hasReminders) {
                Intent mainIntent = new Intent(context, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mainIntent.putExtra("restore_alarms", true);
                context.startActivity(mainIntent);
            }
        }
    }
    

    private void scheduleDailyHabitUpdate(Context context) {
        Log.d(TAG, "Scheduling daily habit update alarm from BootReceiver");
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null, cannot schedule daily update");
            return;
        }
        

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        

        Intent dailyUpdateIntent = new Intent(context, DailyUpdateReceiver.class);
        dailyUpdateIntent.setAction("com.example.goaltracker.DAILY_UPDATE");
        

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            dailyUpdateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        

        try {

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            );
            

            SharedPreferences sharedPreferences = context.getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
            sharedPreferences.edit().putBoolean("daily_update_scheduled", true).apply();
            
            Log.d(TAG, "Daily habit update alarm scheduled for " + calendar.getTime() + " after device boot");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling daily habit update: " + e.getMessage());
        }
    }
} 