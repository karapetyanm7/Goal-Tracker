package com.example.goaltracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent;
import java.util.Random;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private static final String CHANNEL_ID = "habit_reminders";
    
    private static final String[] MOTIVATIONAL_MESSAGES = {
        "You're doing great! Don't forget your habits today.",
        "Consistency builds success. Time to complete your habits!",
        "Ready for your daily habits? Let's do this!",
        "A small step today brings you closer to your goal. Complete your habits!",
        "Stay focused! Time to check off your habits for today.",
        "Keep pushing! You're one habit away from progress.",
        "You've come this far. Don't skip your habits today!",
        "Your habit is waiting! Make progress and check it off.",
        "Don't break the streak! Time to complete today's habits.",
        "Remember, success is in the daily routine. Time for your habits!"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification receiver triggered");
        
        if (intent == null) {
            Log.e(TAG, "Received null intent");
            return;
        }
        
        Log.d(TAG, "Intent action: " + intent.getAction());
        Log.d(TAG, "Intent extras: " + intent.getExtras());
        
        // Get habit data if available
        String habitName = intent.getStringExtra("habit_name");
        int habitId = intent.getIntExtra("habit_id", -1);
        boolean isHabitSpecific = intent.getBooleanExtra("is_habit_specific", false);
        String message = intent.getStringExtra("notification_message");
        boolean isOneTime = intent.getBooleanExtra("is_one_time", false);
        int reminderId = intent.getIntExtra("reminder_id", -1);
        
        Log.d(TAG, "Habit name: " + habitName);
        Log.d(TAG, "Habit ID: " + habitId);
        Log.d(TAG, "Is habit specific: " + isHabitSpecific);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "Is one time: " + isOneTime);
        Log.d(TAG, "Reminder ID: " + reminderId);

        if (message == null) {
            message = getRandomMessage();
            Log.d(TAG, "Using random message: " + message);
        }
        

        String title;
        if (isHabitSpecific && habitName != null) {
            title = "Habit Reminder: " + habitName;
        } else {
            title = "Habit Reminder";
        }
        
        Log.d(TAG, "Preparing notification: " + title + " - " + message);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            Log.e(TAG, "Could not get notification manager");
            return;
        }
        

        Intent mainIntent;
        if (isHabitSpecific && habitId != -1) {

            mainIntent = new Intent(context, HabitDetailActivity.class);
            mainIntent.putExtra("habit_id", habitId);
            mainIntent.putExtra("habit_name", habitName);
        } else {

            mainIntent = new Intent(context, MainActivity.class);
        }
        
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        createNotificationChannel(context, notificationManager);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.clock_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setLights(android.graphics.Color.BLUE, 3000, 3000)
            .setContentIntent(pendingIntent);
            

        if (isHabitSpecific) {
            builder.setColor(android.graphics.Color.GREEN);
        } else {
            builder.setColor(android.graphics.Color.BLUE);
        }

        int notificationId = (reminderId != -1) ? reminderId : (int) System.currentTimeMillis();
        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent with ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification: " + e.getMessage());
            e.printStackTrace();
        }


        if (isOneTime) {
            try {
                SharedPreferences sharedPreferences = context.getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
                Set<String> reminderSet = sharedPreferences.getStringSet("reminder_items", new HashSet<>());
                
                if (reminderSet != null && !reminderSet.isEmpty()) {
                    Log.d(TAG, "Found " + reminderSet.size() + " reminders in SharedPreferences");
                    

                    Set<String> newReminderSet = new HashSet<>(reminderSet);
                    
                    String reminderIdStr = String.valueOf(reminderId);
                    boolean removed = false;
                    
                    for (String reminder : reminderSet) {
                        if (reminder.startsWith(reminderIdStr + "|")) {
                            newReminderSet.remove(reminder);
                            removed = true;
                            Log.d(TAG, "Removed one-time reminder: " + reminder);
                            break;
                        }
                    }
                    
                    if (removed) {
                        sharedPreferences.edit().putStringSet("reminder_items", newReminderSet).apply();
                        Log.d(TAG, "One-time reminder removed from SharedPreferences");
                    } else {
                        Log.d(TAG, "Could not find reminder with ID " + reminderIdStr + " to remove");
                    }
                } else {
                    Log.d(TAG, "No reminders found in SharedPreferences");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing one-time reminder: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    "Habit Reminders", 
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for habit reminders");
                channel.enableVibration(true);
                channel.enableLights(true);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create notification channel: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private String getRandomMessage() {
        Random random = new Random();
        return MOTIVATIONAL_MESSAGES[random.nextInt(MOTIVATIONAL_MESSAGES.length)];
    }
} 