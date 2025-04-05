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
        
        // Get habit data if available
        String habitName = intent.getStringExtra("habit_name");
        String message = intent.getStringExtra("notification_message");
        boolean isOneTime = intent.getBooleanExtra("is_one_time", false);
        
        // Use default message if none provided
        if (message == null) {
            message = getRandomMessage();
        }
        
        // Use default title if no habit name
        String title = (habitName != null) ? habitName : "Habit Reminder";
        
        Log.d(TAG, "Preparing notification: " + title + " - " + message);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create intent for when user taps notification
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Ensure channel exists
        createNotificationChannel(context, notificationManager);

        // Build notification
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

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification sent with ID: " + notificationId);

        // If it's a one-time reminder, remove it from SharedPreferences
        if (isOneTime) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
            Set<String> reminderSet = sharedPreferences.getStringSet("reminder_items", new HashSet<>());
            String reminderId = String.valueOf(intent.getIntExtra("reminder_id", -1));
            reminderSet.removeIf(reminder -> reminder.startsWith(reminderId + "|"));
            sharedPreferences.edit().putStringSet("reminder_items", reminderSet).apply();
            Log.d(TAG, "One-time reminder removed from SharedPreferences");
        }
    }
    
    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }
    }
    
    private String getRandomMessage() {
        Random random = new Random();
        return MOTIVATIONAL_MESSAGES[random.nextInt(MOTIVATIONAL_MESSAGES.length)];
    }
} 