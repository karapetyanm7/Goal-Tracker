package com.example.goaltracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Random;

public class NotificationHelper {
    private static final String CHANNEL_ID = "habit_reminder_channel";
    private static final String CHANNEL_NAME = "Habit Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for habit reminders";
    private static final int NOTIFICATION_ID = 1;

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

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = getRandomMessage();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.clock_icon)
            .setContentTitle("Habit Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setLights(android.graphics.Color.BLUE, 3000, 3000)
            .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private static String getRandomMessage() {
        Random random = new Random();
        return MOTIVATIONAL_MESSAGES[random.nextInt(MOTIVATIONAL_MESSAGES.length)];
    }
} 