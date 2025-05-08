package com.example.goaltracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.Manifest;

public class ReminderActivity extends AppCompatActivity {

    private static final String TAG = "ReminderActivity";
    private ListView reminderListView;
    private Button addReminderButton;
    private TextView noRemindersText;
    private ArrayList<ReminderItem> reminders;
    private ReminderAdapter reminderAdapter;
    private AlarmManager alarmManager;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        
        Log.d(TAG, "ReminderActivity onCreate started");

        reminderListView = findViewById(R.id.reminderListView);
        addReminderButton = findViewById(R.id.addReminderButton);
        noRemindersText = findViewById(R.id.noRemindersText);
        
        if (reminderListView == null || addReminderButton == null || noRemindersText == null) {
            Log.e(TAG, "Error finding views: listView=" + (reminderListView == null) 
                + ", addButton=" + (addReminderButton == null) 
                + ", noText=" + (noRemindersText == null));
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton == null) {
            Log.e(TAG, "Error finding back button");
            Toast.makeText(this, "Error initializing back button", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ThemeManager.applyNavigationButtonStyle(backButton);
        backButton.setOnClickListener(v -> finish());
        
        applyThemeColors();

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
        

        loadReminders();

        addReminderButton.setOnClickListener(v -> {
            Log.d(TAG, "Add reminder button clicked");
            showAddReminderDialog();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
        
        Log.d(TAG, "ReminderActivity onCreate completed");
    }
    
    private void loadReminders() {
        Log.d(TAG, "Loading reminders from SharedPreferences");
        reminders = new ArrayList<>();
        Set<String> reminderSet = sharedPreferences.getStringSet("reminder_items", new HashSet<>());
        
        Log.d(TAG, "Found " + reminderSet.size() + " reminders in SharedPreferences");
        
        for (String reminderData : reminderSet) {
            try {
                String[] parts = reminderData.split("\\|");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0]);
                    String type = parts[1];
                    String time = parts[2];
                    String days = parts[3];
                    
                    ReminderItem item;
                    

                    if (parts.length >= 6) {
                        int habitId = Integer.parseInt(parts[4]);
                        String habitName = parts[5];
                        item = new ReminderItem(id, type, time, days, habitId, habitName);
                        Log.d(TAG, "Loaded habit-specific reminder for: " + habitName);
                    } else {

                        item = new ReminderItem(id, type, time, days);
                        Log.d(TAG, "Loaded general reminder");
                    }
                    
                    reminders.add(item);
                    Log.d(TAG, "Loaded reminder: " + type + " at " + time + " on " + days);
                } else {
                    Log.e(TAG, "Invalid reminder data format: " + reminderData);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing reminder data: " + e.getMessage());
            }
        }

        if (reminders.isEmpty()) {
            Log.d(TAG, "No reminders found, showing empty state");
            noRemindersText.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Found " + reminders.size() + " reminders, hiding empty state");
            noRemindersText.setVisibility(View.GONE);
        }

        reminderAdapter = new ReminderAdapter(this, reminders);
        reminderListView.setAdapter(reminderAdapter);
        Log.d(TAG, "Reminder adapter set on ListView");
    }
    
    private void saveReminders() {
        Set<String> reminderSet = new HashSet<>();
        
        for (ReminderItem reminder : reminders) {
            String reminderData = reminder.id + "|" + 
                                reminder.type + "|" + 
                                reminder.time + "|" + 
                                reminder.days;
            

            if (reminder.isHabitSpecific()) {
                reminderData += "|" + reminder.habitId + "|" + reminder.habitName;
            }
            
            reminderSet.add(reminderData);
        }
        
        sharedPreferences.edit()
            .putStringSet("reminder_items", reminderSet)
            .putBoolean("has_reminders", !reminders.isEmpty())
            .apply();
    }
    
    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        builder.setView(dialogView);
        builder.setTitle("Set Reminder");

        try {
            CheckBox everydayCheckBox = dialogView.findViewById(R.id.everydayCheckBox);
            CheckBox mondayCheckBox = dialogView.findViewById(R.id.mondayCheckBox);
            CheckBox tuesdayCheckBox = dialogView.findViewById(R.id.tuesdayCheckBox);
            CheckBox wednesdayCheckBox = dialogView.findViewById(R.id.wednesdayCheckBox);
            CheckBox thursdayCheckBox = dialogView.findViewById(R.id.thursdayCheckBox);
            CheckBox fridayCheckBox = dialogView.findViewById(R.id.fridayCheckBox);
            CheckBox saturdayCheckBox = dialogView.findViewById(R.id.saturdayCheckBox);
            CheckBox sundayCheckBox = dialogView.findViewById(R.id.sundayCheckBox);
            CheckBox tomorrowCheckBox = dialogView.findViewById(R.id.tomorrowCheckBox);
            CheckBox todayCheckBox = dialogView.findViewById(R.id.todayCheckBox);
            TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
            

            CheckBox specificHabitCheckBox = dialogView.findViewById(R.id.specificHabitCheckBox);
            
            if (everydayCheckBox == null || timePicker == null) {
                throw new NullPointerException("Dialog views not found");
            }
            

            Button selectHabitButton = dialogView.findViewById(R.id.selectHabitButton);
            

            final int[] selectedHabitId = {-1};
            final String[] selectedHabitName = {null};
            

            if (specificHabitCheckBox != null && selectHabitButton != null) {

                selectHabitButton.setVisibility(View.GONE);
                

                specificHabitCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    selectHabitButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                    if (!isChecked) {
                        selectedHabitId[0] = -1;
                        selectedHabitName[0] = null;
                        selectHabitButton.setText("Select Habit");
                    }
                });
                

                selectHabitButton.setOnClickListener(v -> {
                    showHabitSelectionDialog(selectedHabitId, selectedHabitName, selectHabitButton);
                });
            }

            builder.setPositiveButton("Set Reminder", (dialog, which) -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                boolean isHabitSpecific = specificHabitCheckBox != null && 
                                         specificHabitCheckBox.isChecked() && 
                                         selectedHabitId[0] != -1 && 
                                         selectedHabitName[0] != null;

                if (everydayCheckBox.isChecked()) {
                    addDailyReminder(calendar, isHabitSpecific ? selectedHabitId[0] : -1, 
                                    isHabitSpecific ? selectedHabitName[0] : null);
                } else if (tomorrowCheckBox.isChecked()) {
                    addOneTimeReminder(calendar, true, isHabitSpecific ? selectedHabitId[0] : -1, 
                                      isHabitSpecific ? selectedHabitName[0] : null);
                } else if (todayCheckBox.isChecked()) {
                    addOneTimeReminderForToday(calendar, isHabitSpecific ? selectedHabitId[0] : -1, 
                                              isHabitSpecific ? selectedHabitName[0] : null);
                } else {
                    int selectedDays = 0;
                    if (mondayCheckBox.isChecked()) selectedDays |= 1 << Calendar.MONDAY;
                    if (tuesdayCheckBox.isChecked()) selectedDays |= 1 << Calendar.TUESDAY;
                    if (wednesdayCheckBox.isChecked()) selectedDays |= 1 << Calendar.WEDNESDAY;
                    if (thursdayCheckBox.isChecked()) selectedDays |= 1 << Calendar.THURSDAY;
                    if (fridayCheckBox.isChecked()) selectedDays |= 1 << Calendar.FRIDAY;
                    if (saturdayCheckBox.isChecked()) selectedDays |= 1 << Calendar.SATURDAY;
                    if (sundayCheckBox.isChecked()) selectedDays |= 1 << Calendar.SUNDAY;

                    if (selectedDays != 0) {
                        addWeeklyReminder(calendar, selectedDays, isHabitSpecific ? selectedHabitId[0] : -1, 
                                         isHabitSpecific ? selectedHabitName[0] : null);
                    } else {
                        Toast.makeText(ReminderActivity.this, "Please select at least one day or option", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNeutralButton("Test Notification", (dialog, which) -> {
                sendTestNotification();
            });

            builder.setNegativeButton("Cancel", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage());
            Toast.makeText(this, "Could not show reminder dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showHabitSelectionDialog(final int[] selectedHabitId, final String[] selectedHabitName, final Button selectHabitButton) {

        SharedPreferences habitsPrefs = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
        Set<String> habitSet = habitsPrefs.getStringSet("habit_items", new HashSet<>());
        
        if (habitSet.isEmpty()) {
            Toast.makeText(this, "No habits found. Create habits first.", Toast.LENGTH_SHORT).show();
            return;
        }
        

        ArrayList<String> habitNames = new ArrayList<>();
        final ArrayList<Integer> habitIds = new ArrayList<>();
        
        for (String habitData : habitSet) {
            try {
                String[] parts = habitData.split("\\|");
                if (parts.length >= 2) {
                    habitIds.add(Integer.parseInt(parts[0]));
                    habitNames.add(parts[1]);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing habit data: " + e.getMessage());
            }
        }
        

        String[] habitNamesArray = habitNames.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Habit")
               .setItems(habitNamesArray, (dialog, which) -> {
                   selectedHabitId[0] = habitIds.get(which);
                   selectedHabitName[0] = habitNames.get(which);
                   selectHabitButton.setText("Habit: " + selectedHabitName[0]);
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void addDailyReminder(Calendar calendar, int habitId, String habitName) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        

        int reminderId = (int) System.currentTimeMillis();
        

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("has_reminders", true);
        editor.putBoolean("has_daily_reminder", true);
        editor.putInt("reminder_hour", calendar.get(Calendar.HOUR_OF_DAY));
        editor.putInt("reminder_minute", calendar.get(Calendar.MINUTE));
        editor.apply();
        
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.setAction("com.example.goaltracker.REMINDER_NOTIFICATION");
        

        if (habitId != -1 && habitName != null) {
            intent.putExtra("notification_message", "Time to check your habit: " + habitName);
            intent.putExtra("habit_name", habitName);
            intent.putExtra("habit_id", habitId);
            intent.putExtra("is_habit_specific", true);
        } else {
            intent.putExtra("notification_message", "Time to check your habits for today!");
            intent.putExtra("is_habit_specific", false);
        }
        
        intent.putExtra("reminder_id", reminderId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );

                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    );
                    
                    Toast.makeText(this, "Daily reminder set (inexact) for " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                    requestExactAlarmPermission();
                } else {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    

                    PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                        this,
                        reminderId + 1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY,
                        AlarmManager.INTERVAL_DAY,
                        repeatingIntent
                    );
                    
                    Toast.makeText(this, "Daily reminder set for " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                }
            } else {

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                );
                
                Toast.makeText(this, "Daily reminder set for " + formatTime(calendar), Toast.LENGTH_SHORT).show();
            }
            

            ReminderItem reminder = new ReminderItem(
                reminderId,
                "Daily",
                formatTime(calendar),
                "Every day",
                habitId,
                habitName
            );
            reminders.add(reminder);
            reminderAdapter.notifyDataSetChanged();
            saveReminders();
            

            if (noRemindersText.getVisibility() == View.VISIBLE) {
                noRemindersText.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "Daily reminder scheduled for " + calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling daily reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addWeeklyReminder(Calendar calendar, int selectedDays, int habitId, String habitName) {
        int baseId = (int) System.currentTimeMillis();
        Calendar now = Calendar.getInstance();
        
        try {
            String daysStr = formatSelectedDays(selectedDays);
            
            for (int i = 1; i <= 7; i++) {
                if ((selectedDays & (1 << i)) != 0) {
                    Calendar dayCalendar = (Calendar) calendar.clone();
                    dayCalendar.set(Calendar.DAY_OF_WEEK, i);
                    if (dayCalendar.getTimeInMillis() <= now.getTimeInMillis()) {
                        dayCalendar.add(Calendar.DAY_OF_YEAR, 7);
                    }

                    int reminderId = baseId + i;
                    Intent intent = new Intent(this, NotificationReceiver.class);
                    intent.setAction("com.example.goaltracker.REMINDER_NOTIFICATION");
                    

                    if (habitId != -1 && habitName != null) {
                        intent.putExtra("notification_message", "Time to check your weekly habit: " + habitName);
                        intent.putExtra("habit_name", habitName);
                        intent.putExtra("habit_id", habitId);
                        intent.putExtra("is_habit_specific", true);
                    } else {
                        intent.putExtra("notification_message", "Time to check your weekly habits!");
                        intent.putExtra("is_habit_specific", false);
                    }
                    
                    intent.putExtra("reminder_id", reminderId);
                    
                    PendingIntent dayIntent = PendingIntent.getBroadcast(
                        this,
                        reminderId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {

                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                dayCalendar.getTimeInMillis(),
                                dayIntent
                            );
                            

                            PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                                this,
                                reminderId + 100,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );
                            
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                dayCalendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY * 7,
                                AlarmManager.INTERVAL_DAY * 7,
                                repeatingIntent
                            );
                            

                            if (i == 1) {
                                requestExactAlarmPermission();
                            }
                        } else {

                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                dayCalendar.getTimeInMillis(),
                                dayIntent
                            );
                            

                            PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                                this,
                                reminderId + 100,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );
                            
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                dayCalendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY * 7,
                                AlarmManager.INTERVAL_DAY * 7,
                                repeatingIntent
                            );
                        }
                    } else {
                        alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            dayCalendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY * 7,
                            dayIntent
                        );
                    }
                }
            }

            if (!daysStr.isEmpty()) {
                boolean isExact = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms());
                String exactStatus = isExact ? "" : "(inexact) ";
                Toast.makeText(this, "Weekly reminders " + exactStatus + "set for " + daysStr + " at " + 
                    formatTime(calendar), Toast.LENGTH_SHORT).show();

                ReminderItem reminder = new ReminderItem(
                    baseId,
                    "Weekly",
                    formatTime(calendar),
                    daysStr,
                    habitId,
                    habitName
                );
                reminders.add(reminder);
                reminderAdapter.notifyDataSetChanged();
                saveReminders();
                

                if (noRemindersText.getVisibility() == View.VISIBLE) {
                    noRemindersText.setVisibility(View.GONE);
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("has_reminders", true);
                editor.putBoolean("has_weekly_reminder", true);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling weekly reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addOneTimeReminder(Calendar calendar, boolean isTomorrow, int habitId, String habitName) {
        if (isTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        int reminderId = (int) System.currentTimeMillis();
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.setAction("com.example.goaltracker.REMINDER_NOTIFICATION");
        

        if (habitId != -1 && habitName != null) {
            intent.putExtra("notification_message", "Don't forget to check your habit: " + habitName);
            intent.putExtra("habit_name", habitName);
            intent.putExtra("habit_id", habitId);
            intent.putExtra("is_habit_specific", true);
        } else {
            intent.putExtra("notification_message", "Don't forget to check your habits!");
            intent.putExtra("is_habit_specific", false);
        }
        
        intent.putExtra("reminder_id", reminderId);
        intent.putExtra("is_one_time", true);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    
                    String day = isTomorrow ? "tomorrow" : "on " + formatDate(calendar);
                    Toast.makeText(this, "One-time reminder set (inexact) for " + day + " at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                    requestExactAlarmPermission();
                } else {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    
                    String day = isTomorrow ? "tomorrow" : "on " + formatDate(calendar);
                    Toast.makeText(this, "One-time reminder set for " + day + " at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
                
                String day = isTomorrow ? "tomorrow" : "on " + formatDate(calendar);
                Toast.makeText(this, "One-time reminder set for " + day + " at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
            }
            

            String dateStr = isTomorrow ? "Tomorrow" : formatDate(calendar);
            ReminderItem reminder = new ReminderItem(
                reminderId,
                "One-time",
                formatTime(calendar),
                dateStr,
                habitId,
                habitName
            );
            reminders.add(reminder);
            reminderAdapter.notifyDataSetChanged();
            saveReminders();
            

            if (noRemindersText.getVisibility() == View.VISIBLE) {
                noRemindersText.setVisibility(View.GONE);
            }
            
            Log.d(TAG, "One-time reminder scheduled for " + calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling one-time reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addOneTimeReminderForToday(Calendar calendar, int habitId, String habitName) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Cannot set reminder for past time today", Toast.LENGTH_SHORT).show();
            return;
        }

        addOneTimeReminder(calendar, false, habitId, habitName);
    }
    
    private void deleteReminder(int position) {
        ReminderItem reminder = reminders.get(position);
        

        Intent intent = new Intent(this, NotificationReceiver.class);

        int[] intentIds = {
            reminder.id,
            reminder.id + 1,
            reminder.id + 100,
        };
        
        for (int intentId : intentIds) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                intentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }

        reminders.remove(position);
        reminderAdapter.notifyDataSetChanged();
        saveReminders();
        

        if (reminders.isEmpty()) {
            noRemindersText.setVisibility(View.VISIBLE);
        }
        
        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
    }
    

    private void sendTestNotification() {

        CharSequence[] options = new CharSequence[]{"General Reminder", "Habit-Specific Reminder"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Test Notification Type")
               .setItems(options, (dialog, which) -> {
                   if (which == 0) {

                       sendTestGeneralNotification();
                   } else {

                       showHabitSelectionForTestNotification();
                   }
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void sendTestGeneralNotification() {
        try {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.setAction("com.example.goaltracker.REMINDER_NOTIFICATION");
            intent.putExtra("notification_message", "This is a test notification for all habits. Your reminders are working!");
            intent.putExtra("is_habit_specific", false);
            intent.putExtra("reminder_id", 99998); // Use a unique ID for test notifications
            
            Log.d(TAG, "Sending test general notification broadcast");
            sendBroadcast(intent);
            
            Toast.makeText(this, "Test general notification sent. You should see it momentarily.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Test general notification broadcast sent");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send test notification: " + e.getMessage());
            Toast.makeText(this, "Failed to send test notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showHabitSelectionForTestNotification() {

        SharedPreferences habitsPrefs = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
        Set<String> habitSet = habitsPrefs.getStringSet("habit_items", new HashSet<>());
        
        if (habitSet.isEmpty()) {
            Toast.makeText(this, "No habits found. Create habits first.", Toast.LENGTH_SHORT).show();
            return;
        }
        

        ArrayList<String> habitNames = new ArrayList<>();
        final ArrayList<Integer> habitIds = new ArrayList<>();
        
        for (String habitData : habitSet) {
            try {
                String[] parts = habitData.split("\\|");
                if (parts.length >= 2) {
                    habitIds.add(Integer.parseInt(parts[0]));
                    habitNames.add(parts[1]);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing habit data: " + e.getMessage());
            }
        }
        

        String[] habitNamesArray = habitNames.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Habit for Test Notification")
               .setItems(habitNamesArray, (dialog, which) -> {
                   int habitId = habitIds.get(which);
                   String habitName = habitNames.get(which);
                   sendTestHabitSpecificNotification(habitId, habitName);
               })
               .setNegativeButton("Cancel", null)
               .show();
    }
    
    private void sendTestHabitSpecificNotification(int habitId, String habitName) {
        try {
            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.setAction("com.example.goaltracker.REMINDER_NOTIFICATION");
            intent.putExtra("notification_message", "This is a test notification for habit: " + habitName);
            intent.putExtra("habit_name", habitName);
            intent.putExtra("habit_id", habitId);
            intent.putExtra("is_habit_specific", true);
            intent.putExtra("reminder_id", 99999); // Use a unique ID for test notifications
            
            Log.d(TAG, "Sending test habit-specific notification broadcast");
            sendBroadcast(intent);
            
            Toast.makeText(this, "Test habit-specific notification sent. You should see it momentarily.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Test habit-specific notification broadcast sent");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send test notification: " + e.getMessage());
            Toast.makeText(this, "Failed to send test notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }
    

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
                Toast.makeText(this, "Please grant permission to set exact alarms", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open alarm settings", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error opening alarm settings: " + e.getMessage());
            }
        }
    }
    

    private String formatTime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d", hour, minute);
    }
    

    private String formatDate(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%02d/%02d", month, day);
    }

    private String formatSelectedDays(int selectedDays) {
        StringBuilder result = new StringBuilder();
        if ((selectedDays & (1 << Calendar.MONDAY)) != 0) result.append("Monday, ");
        if ((selectedDays & (1 << Calendar.TUESDAY)) != 0) result.append("Tuesday, ");
        if ((selectedDays & (1 << Calendar.WEDNESDAY)) != 0) result.append("Wednesday, ");
        if ((selectedDays & (1 << Calendar.THURSDAY)) != 0) result.append("Thursday, ");
        if ((selectedDays & (1 << Calendar.FRIDAY)) != 0) result.append("Friday, ");
        if ((selectedDays & (1 << Calendar.SATURDAY)) != 0) result.append("Saturday, ");
        if ((selectedDays & (1 << Calendar.SUNDAY)) != 0) result.append("Sunday, ");
        
        String days = result.toString();
        if (!days.isEmpty()) {
            days = days.substring(0, days.length() - 2);
        }
        return days;
    }

    private static class ReminderItem {
        int id;
        String type;
        String time;
        String days;
        int habitId = -1;
        String habitName = null;
        

        ReminderItem(int id, String type, String time, String days) {
            this.id = id;
            this.type = type;
            this.time = time;
            this.days = days;
        }
        

        ReminderItem(int id, String type, String time, String days, int habitId, String habitName) {
            this.id = id;
            this.type = type;
            this.time = time;
            this.days = days;
            this.habitId = habitId;
            this.habitName = habitName;
        }
        

        boolean isHabitSpecific() {
            return habitId != -1 && habitName != null;
        }
    }

    private class ReminderAdapter extends ArrayAdapter<ReminderItem> {
        
        public ReminderAdapter(Context context, ArrayList<ReminderItem> reminders) {
            super(context, 0, reminders);
        }
        
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.reminder_item, parent, false);
            }
            
            ReminderItem reminder = getItem(position);
            
            TextView typeText = convertView.findViewById(R.id.reminderTypeText);
            TextView timeText = convertView.findViewById(R.id.reminderTimeText);
            TextView daysText = convertView.findViewById(R.id.reminderDaysText);
            ImageButton deleteButton = convertView.findViewById(R.id.deleteReminderButton);
            
            String typeLabel;
            if (reminder.isHabitSpecific()) {

                typeLabel = reminder.type + " - " + reminder.habitName;
            } else {

                typeLabel = reminder.type + " - All Habits";
            }
            
            typeText.setText(typeLabel);
            timeText.setText(reminder.time);
            daysText.setText(reminder.days);
            
            deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(ReminderActivity.this)
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete this reminder?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteReminder(position))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
            
            return convertView;
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    1001);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted");
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission denied");
                Toast.makeText(this, 
                    "Notification permission denied. You won't receive reminders.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Apply theme colors including background to the activity
     */
    private void applyThemeColors() {
        // Get the primary color from ThemeManager
        int primaryColor = ThemeManager.getPrimaryColor(this);
        
        // Apply to buttons
        addReminderButton.setBackgroundColor(primaryColor);
        
        // Apply background color to the main layout
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View mainLayout = ((ViewGroup) rootView).getChildAt(0);
            if (mainLayout != null) {
                // Apply a tinted background that's lighter than the primary color
                int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                mainLayout.setBackgroundColor(lightPrimaryColor);
            }
        }
        
        // Set the status bar color to match the primary theme color
        // Clear any existing flags first to ensure proper coloring
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
    }
    
    /**
     * Helper method to create a lighter version of a color
     */
    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }
}