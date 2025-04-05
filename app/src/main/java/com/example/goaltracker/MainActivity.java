package com.example.goaltracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatDelegate;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.widget.CheckBox;
import android.widget.TimePicker;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.app.Activity;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> habits;
    private HashMap<String, Integer> streaks;
    private ListView habitListView;
    private HabitAdapter habitAdapter;
    private SharedPreferences sharedPreferences;
    private Button addButton;
    private ImageButton themeToggleButton;
    private ImageButton calendarButton;
    private ImageButton statsButton;
    private ImageButton reminderButton;
    private ActivityResultLauncher<Intent> habitDetailLauncher;
    private ImageButton addHabitButton;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    private FirebaseAuth mAuth;
    private TextView textViewWelcome;
    private ImageButton profileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        habitDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String habitName = data.getStringExtra("habit_name");
                        if (habitName != null) {
                            int position = habits.indexOf(habitName);
                            if (position >= 0) {
                                habitAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });


        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);


        habits = new ArrayList<>(sharedPreferences.getStringSet("habits", new HashSet<>()));
        streaks = loadStreaks();


        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }


        habitListView = findViewById(R.id.habitListView);
        addButton = findViewById(R.id.addButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        calendarButton = findViewById(R.id.calendarButton);
        statsButton = findViewById(R.id.statsButton);
        reminderButton = findViewById(R.id.reminderButton);
        profileButton = findViewById(R.id.profile_button);


        if (habitListView == null || addButton == null || themeToggleButton == null || 
            calendarButton == null || statsButton == null || reminderButton == null || 
            profileButton == null) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            return;
        }

        habitAdapter = new HabitAdapter(this, habits);
        habitListView.setAdapter(habitAdapter);

        updateThemeToggleButton(isDarkMode);


        setupButtonListeners();


        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationHelper.createNotificationChannel(this);
        loadHabits();


        if (getIntent() != null && getIntent().getBooleanExtra("restore_alarms", false)) {
            restoreAlarms();
        }
    }

    private void setupButtonListeners() {

        themeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
                isDarkMode = !isDarkMode;
                sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply();
                updateThemeToggleButton(isDarkMode);
                

                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                

                recreate();
            }
        });


        calendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        reminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReminderActivity.class);
            startActivity(intent);
        });

        addButton.setOnClickListener(v -> showAddHabitDialog());

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });


        habitListView.setOnItemClickListener((parent, view, position, id) -> {
                String habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habit_name", habit);
                int points = sharedPreferences.getInt(habit + "_points", 100);
                int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;
                intent.putExtra("habit_points", points);
                intent.putExtra("habit_streak", streak);
                habitDetailLauncher.launch(intent);
        });
    }

    private void updateThemeToggleButton(boolean isDarkMode) {
        if (isDarkMode) {
            themeToggleButton.setImageResource(R.drawable.light_icon);
        } else {
            themeToggleButton.setImageResource(R.drawable.dark_icon);
        }
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Habit");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_habit, null);
        EditText habitNameInput = dialogView.findViewById(R.id.habitNameInput);
        

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            habitNameInput.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            habitNameInput.setTextColor(getResources().getColor(android.R.color.black));
        }
        
        builder.setView(dialogView);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = habitNameInput.getText().toString().trim();
                

                if (habitName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (habits.contains(habitName)) {
                    Toast.makeText(MainActivity.this, "A habit with the name '" + habitName + "' already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                habits.add(habitName);
                streaks.put(habitName, 0);
                saveHabits();
                saveStreaks();
                habitAdapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(isDarkMode ? 
            R.color.app_card_background : android.R.color.white);
        dialog.show();
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit");

        final EditText input = new EditText(this);
        input.setText(habits.get(position));

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            input.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            input.setTextColor(getResources().getColor(android.R.color.black));
        }
        
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString().trim();
                String oldHabit = habits.get(position);

                if (habitName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (habitName.equals(oldHabit)) {
                    return;
                }
                if (habits.contains(habitName)) {
                    Toast.makeText(MainActivity.this, "A habit with the name '" + habitName + "' already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                habits.set(position, habitName);
                int streak = streaks.remove(oldHabit);
                streaks.put(habitName, streak);
                saveHabits();
                saveStreaks();
                habitAdapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();

        alertDialog.getWindow().setBackgroundDrawableResource(isDarkMode ? 
            R.color.app_card_background : android.R.color.white);
        alertDialog.show();
    }

    private void updateHabitList() {
        habitAdapter.notifyDataSetChanged();
    }

    private ArrayList<String> loadHabits() {
        ArrayList<String> habitList = new ArrayList<>();
        int size = sharedPreferences.getInt("habits_size", 0);
        for (int i = 0; i < size; i++) {
            habitList.add(sharedPreferences.getString("habit_" + i, null));
        }
        return habitList;
    }

    private void saveHabits() {
        Set<String> habitSet = new HashSet<>(habits);
        sharedPreferences.edit().putStringSet("habits", habitSet).apply();
    }

    private HashMap<String, Integer> loadStreaks() {
        HashMap<String, Integer> streakMap = new HashMap<>();
        for (String habit : habits) {
            streakMap.put(habit, sharedPreferences.getInt(habit + "_streak", 0));
        }
        return streakMap;
    }

    private void saveStreaks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String habit : streaks.keySet()) {
            editor.putInt(habit + "_streak", streaks.get(habit));
        }
        editor.apply();
    }

    private class HabitAdapter extends ArrayAdapter<String> {
        public HabitAdapter(Context context, ArrayList<String> habits) {
            super(context, 0, habits);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.habit_item, parent, false);
            }

            TextView habitText = convertView.findViewById(R.id.habitText);
            ImageView streakIcon = convertView.findViewById(R.id.streakIcon);
            TextView streakText = convertView.findViewById(R.id.streakText);

            String habit = getItem(position);
            habitText.setText(habit);

            int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;

            long lastMarkedTime = sharedPreferences.getLong(habit + "_last_marked", 0);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            boolean isMarked = lastMarkedTime >= startOfDay;

            if (streak >= 1) {
                streakIcon.setVisibility(View.VISIBLE);
                streakText.setVisibility(View.VISIBLE);
                streakText.setText(" " + streak);

                streakIcon.setImageResource(isMarked ? R.drawable.streak_fire : R.drawable.streak_icon_u);
            } else {
                streakIcon.setVisibility(View.GONE);
                streakText.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    private void showReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        builder.setView(dialogView);

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

        builder.setPositiveButton("Set Reminder", (dialog, which) -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (everydayCheckBox.isChecked()) {
                scheduleDailyReminder(calendar);
            } else if (tomorrowCheckBox.isChecked()) {
                scheduleOneTimeReminder(calendar);
            } else if (todayCheckBox.isChecked()) {
                scheduleOneTimeReminderForToday(calendar);
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
                    scheduleWeeklyReminder(calendar, selectedDays);
                }
            }
        });


        builder.setNeutralButton("Test Notification", (dialog, which) -> {

            sendTestNotification();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void scheduleDailyReminder(Calendar calendar) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("has_reminders", true);
        editor.putBoolean("has_daily_reminder", true);
        editor.putInt("reminder_hour", calendar.get(Calendar.HOUR_OF_DAY));
        editor.putInt("reminder_minute", calendar.get(Calendar.MINUTE));
        editor.apply();
        
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_message", "Time to check your habits for today!");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
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
                        1,
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
            

            Log.d("MainActivity", "Daily reminder scheduled for " + calendar.getTime());
        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling daily reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleWeeklyReminder(Calendar calendar, int selectedDays) {
        cancelAllReminders();

        int requestCode = 10;
        Calendar now = Calendar.getInstance();
        StringBuilder scheduledDays = new StringBuilder();
        
        try {
            for (int i = 1; i <= 7; i++) {
                if ((selectedDays & (1 << i)) != 0) {
                    Calendar dayCalendar = (Calendar) calendar.clone();
                    dayCalendar.set(Calendar.DAY_OF_WEEK, i);
                    if (dayCalendar.getTimeInMillis() <= now.getTimeInMillis()) {
                        dayCalendar.add(Calendar.DAY_OF_YEAR, 7);
                    }

                    Intent intent = new Intent(this, NotificationReceiver.class);
                    intent.putExtra("notification_message", "Time to check your weekly habits!");
                    
                    PendingIntent dayIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode++,
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
                                requestCode++,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );
                            
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                dayCalendar.getTimeInMillis() + AlarmManager.INTERVAL_DAY * 7,
                                AlarmManager.INTERVAL_DAY * 7,
                                repeatingIntent
                            );
                            

                            if (scheduledDays.length() == 0) {
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
                                requestCode++,
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
                    

                    if (scheduledDays.length() > 0) {
                        scheduledDays.append(", ");
                    }
                    scheduledDays.append(getDayName(i));

                    Log.d("MainActivity", "Weekly reminder scheduled for " + getDayName(i) + 
                          " at " + formatTime(dayCalendar));
                }
            }

            String days = formatSelectedDays(selectedDays);
            
            if (!days.isEmpty()) {
                boolean isExact = !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms());
                String exactStatus = isExact ? "" : "(inexact) ";
                Toast.makeText(this, "Weekly reminders " + exactStatus + "set for " + days + " at " + 
                    formatTime(calendar), Toast.LENGTH_SHORT).show();
                    

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("has_reminders", true);
                editor.putBoolean("has_weekly_reminder", true);

                editor.apply();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling weekly reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
    
    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "Unknown";
        }
    }

    private void scheduleOneTimeReminder(Calendar calendar) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_message", "Don't forget to check your habits tomorrow!");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            2,
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
                    

                    Toast.makeText(this, "One-time reminder set (inexact) for tomorrow at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                    

                    requestExactAlarmPermission();
                } else {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    
                    Toast.makeText(this, "One-time reminder set for tomorrow at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                }
            } else {

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
                
                Toast.makeText(this, "One-time reminder set for tomorrow at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
            }

            Log.d("MainActivity", "One-time reminder scheduled for tomorrow at " + formatTime(calendar));
        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling one-time reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleOneTimeReminderForToday(Calendar calendar) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Cannot set reminder for past time today", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_message", "It's time to check your habits!");
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            3,
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
                    

                    Toast.makeText(this, "One-time reminder set (inexact) for today at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                    

                    requestExactAlarmPermission();
                } else {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    
                    Toast.makeText(this, "One-time reminder set for today at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
                }
            } else {

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
                
                Toast.makeText(this, "One-time reminder set for today at " + formatTime(calendar), Toast.LENGTH_SHORT).show();
            }
            

            Log.d("MainActivity", "One-time reminder scheduled for today at " + formatTime(calendar));
        } catch (Exception e) {
            Log.e("MainActivity", "Error scheduling one-time reminder: " + e.getMessage());
            Toast.makeText(this, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelAllReminders() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        for (int i = 0; i < 7; i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllReminders();
    }


    private String formatTime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d", hour, minute);
    }

    private void sendTestNotification() {

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_message", "This is a test notification. Your reminders are working!");
        

        sendBroadcast(intent);
        
        Toast.makeText(this, "Test notification sent. You should see it momentarily.", Toast.LENGTH_SHORT).show();
        Log.d("MainActivity", "Test notification sent");
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
                Toast.makeText(this, "Unable to open alarm settings. Please enable exact alarms manually in system settings.", Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Error opening alarm settings: " + e.getMessage());
            }
        }
    }

    private void restoreAlarms() {
        SharedPreferences sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);
        

        boolean hasDailyReminder = sharedPreferences.getBoolean("has_daily_reminder", false);
        if (hasDailyReminder) {
            int hour = sharedPreferences.getInt("reminder_hour", 9);
            int minute = sharedPreferences.getInt("reminder_minute", 0);
            
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            scheduleDailyReminder(calendar);
        }
        

    }
}
