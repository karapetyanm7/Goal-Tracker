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
import android.text.InputType;
import android.graphics.Color;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ArrayList<HabitItem> habits;
    private HashMap<String, Integer> streaks;
    private ListView habitListView;
    private HabitAdapter habitAdapter;
    private SharedPreferences sharedPreferences;
    private Button addButton;
    private ImageButton themeToggleButton;
    private ImageButton calendarButton;
    private ImageButton statsButton;
    private ImageButton reminderButton;
    private ImageButton profileButton;
    private ImageButton settingsButton;
    private ActivityResultLauncher<Intent> habitDetailLauncher;
    private ImageButton addHabitButton;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private FirebaseAuth mAuth;
    private TextView textViewWelcome;
    private ActivityResultLauncher<Intent> themeSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mAuth = FirebaseAuth.getInstance();
        

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, NotificationReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);

        ThemeManager.applyTheme(this);
        
        setContentView(R.layout.activity_main);
        

        fixStatusBarColor();


        if (!isUserLoggedIn()) {

            Log.d(TAG, "User not signed in, redirecting to LoginActivity");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        Log.d(TAG, "User is signed in");

        habitDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String action = data.getStringExtra("action");
                        if (action != null) {
                            if (action.equals("delete")) {
                                String habitName = data.getStringExtra("habit_name");
                                if (habitName != null) {

                                    for (int i = 0; i < habits.size(); i++) {
                                        if (habits.get(i).name.equals(habitName)) {
                                            habits.remove(i);
                                            habitAdapter.notifyDataSetChanged();
                                            break;
                                        }
                                    }
                                }
                            } else if (action.equals("edit")) {

                                String oldName = data.getStringExtra("old_habit_name");
                                String newName = data.getStringExtra("new_habit_name");
                                
                                if (oldName != null && newName != null) {

                                    for (int i = 0; i < habits.size(); i++) {
                                        if (habits.get(i).name.equals(oldName)) {
                                            habits.get(i).name = newName;
                                            break;
                                        }
                                    }
                                    

                                    if (streaks.containsKey(oldName)) {
                                        int streak = streaks.get(oldName);
                                        streaks.remove(oldName);
                                        streaks.put(newName, streak);
                                    }

                                    habitAdapter.notifyDataSetChanged();
                                }
                            } else if (action.equals("update")) {
                                String habitName = data.getStringExtra("habit_name");
                                if (habitName != null) {

                                    habitAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                }
            });


        themeSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {

                    recreate();
                }
            });

        sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);

        habits = new ArrayList<>();
        Set<String> habitSet = sharedPreferences.getStringSet("habits", new HashSet<>());
        for (String habitName : habitSet) {
            habits.add(new HabitItem(habitName));
        }
        streaks = loadStreaks();

        habitListView = findViewById(R.id.habitListView);
        addButton = findViewById(R.id.addButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        calendarButton = findViewById(R.id.calendarButton);
        statsButton = findViewById(R.id.statsButton);
        reminderButton = findViewById(R.id.reminderButton);
        profileButton = findViewById(R.id.profile_button);
        settingsButton = findViewById(R.id.settingsButton);

        if (habitListView == null || addButton == null || themeToggleButton == null || 
            calendarButton == null || statsButton == null || reminderButton == null || 
            profileButton == null || settingsButton == null) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            return;
        }

        habitAdapter = new HabitAdapter(this, habits);
        habitListView.setAdapter(habitAdapter);

        updateThemeToggleButton(ThemeManager.isDarkMode(this));
        

        applyThemeColors();

        setupButtonListeners();

        NotificationHelper.createNotificationChannel(this);
        loadHabits();
        checkAndUpdateStreaks();

        if (getIntent() != null && getIntent().getBooleanExtra("restore_alarms", false)) {
            restoreAlarms();
        }
        

        if (!sharedPreferences.getBoolean("daily_update_scheduled", false)) {
            scheduleDailyHabitUpdate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        

        refreshHabitsList();
    }
    

    private void refreshHabitsList() {

        Set<String> latestHabitSet = sharedPreferences.getStringSet("habits", new HashSet<>());
        

        habits.clear();
        for (String habitName : latestHabitSet) {
            habits.add(new HabitItem(habitName));
        }

        streaks = loadStreaks();

        if (habitAdapter != null) {
            habitAdapter.notifyDataSetChanged();
        }
    }

    private void setupButtonListeners() {

        themeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isDarkMode = ThemeManager.isDarkMode(MainActivity.this);
                isDarkMode = !isDarkMode;
                ThemeManager.setDarkMode(MainActivity.this, isDarkMode);
                updateThemeToggleButton(isDarkMode);
                
                recreate();
            }
        });
        
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ThemeSettingsActivity.class);
                themeSettingsLauncher.launch(intent);
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
                HabitItem habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habit_name", habit.name);
                int points = sharedPreferences.getInt(habit.name + "_points", 100);
                int streak = streaks.containsKey(habit.name) ? streaks.get(habit.name) : 0;
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

        int primaryColor = ThemeManager.getPrimaryColor(this);
        habitNameInput.getBackground().setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_ATOP);

        boolean isDarkMode = ThemeManager.isDarkMode(MainActivity.this);
        if (isDarkMode) {
            habitNameInput.setTextColor(getResources().getColor(android.R.color.white));
            habitNameInput.setBackgroundColor(Color.BLACK);
        } else {
            habitNameInput.setTextColor(getResources().getColor(android.R.color.black));
            habitNameInput.setBackgroundColor(Color.WHITE);
        }

        LinearLayout dialogContainer = dialogView.findViewById(R.id.dialog_add_habit_container);
        if (dialogContainer != null) {
            dialogContainer.setBackgroundColor(isDarkMode ? 
                ContextCompat.getColor(this, R.color.app_card_background) : Color.WHITE);
        }
        
        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
                String habitName = habitNameInput.getText().toString().trim();
            

            if (habitName.isEmpty()) {
                Toast.makeText(MainActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            for (HabitItem habit : habits) {
                if (habit.name.equalsIgnoreCase(habitName)) {
                    Toast.makeText(MainActivity.this, "A habit with this name already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            HabitItem newHabit = new HabitItem(habitName);
            habits.add(newHabit);
                    streaks.put(habitName, 0);
                    saveHabits();
                    saveStreaks();
                    habitAdapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                isDarkMode ? ContextCompat.getColor(this, R.color.app_card_background) : Color.WHITE));
        }

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            
            if (positiveButton != null) {
                positiveButton.setTextColor(primaryColor);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(primaryColor);
            }
        });
        
        dialog.show();
    }

    private void showEditHabitDialog(int position) {
        HabitItem habit = habits.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit");

        int primaryColor = ThemeManager.getPrimaryColor(this);
        boolean isDarkMode = ThemeManager.isDarkMode(this);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(habit.name);
        input.setPadding(12, 12, 12, 12);

        input.getBackground().setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_ATOP);

        if (isDarkMode) {
            input.setTextColor(Color.WHITE);
            input.setBackgroundColor(Color.BLACK);
        } else {
            input.setTextColor(Color.BLACK);
            input.setBackgroundColor(Color.WHITE);
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);
        container.setBackgroundColor(isDarkMode ? 
            ContextCompat.getColor(this, R.color.app_card_background) : Color.WHITE);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Save", null); // Set to null first
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                isDarkMode ? ContextCompat.getColor(this, R.color.app_card_background) : Color.WHITE));
        }
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(primaryColor);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(primaryColor);
            }
            
            positiveButton.setOnClickListener(v -> {
                String newName = input.getText().toString().trim();
                
                if (newName.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }


                if (newName.equals(habit.name)) {
                    dialog.dismiss();
                    return;
                }


                boolean isDuplicate = false;
                for (int i = 0; i < habits.size(); i++) {
                    if (i != position && habits.get(i).name.equalsIgnoreCase(newName)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    Toast.makeText(MainActivity.this, "A habit with this name already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                String oldName = habit.name;
                habit.name = newName;

                if (streaks.containsKey(oldName)) {
                    int streak = streaks.get(oldName);
                    streaks.remove(oldName);
                    streaks.put(newName, streak);
                }
                saveHabits();
                saveStreaks();
                habitAdapter.notifyDataSetChanged();
                
                Toast.makeText(MainActivity.this, "Habit renamed successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateHabitList() {
        habitAdapter.notifyDataSetChanged();
    }

    private ArrayList<HabitItem> loadHabits() {
        ArrayList<HabitItem> habitList = new ArrayList<>();
        int size = sharedPreferences.getInt("habits_size", 0);
        for (int i = 0; i < size; i++) {
            habitList.add(new HabitItem(sharedPreferences.getString("habit_" + i, null)));
        }
        return habitList;
    }

    private void saveHabits() {
        Set<String> habitSet = new HashSet<>();
        for (HabitItem habit : habits) {
            habitSet.add(habit.name);
        }
        sharedPreferences.edit().putStringSet("habits", habitSet).apply();
    }

    private HashMap<String, Integer> loadStreaks() {
        HashMap<String, Integer> streakMap = new HashMap<>();
        for (HabitItem habit : habits) {
            streakMap.put(habit.name, sharedPreferences.getInt(habit.name + "_streak", 0));
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

    private class HabitAdapter extends ArrayAdapter<HabitItem> {
        public HabitAdapter(Context context, ArrayList<HabitItem> habits) {
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

            int primaryColor = ThemeManager.getPrimaryColor(MainActivity.this);

            View itemContainer = convertView.findViewById(R.id.habit_item_container);

            ViewGroup cardView = (ViewGroup) convertView;
            if (cardView != null) {

                int cardColor = darkenColor(primaryColor, 0.8f);
                

                try {

                    Class<?> cardViewClass = cardView.getClass();
                    java.lang.reflect.Method setCardBackgroundColorMethod = 
                        cardViewClass.getMethod("setCardBackgroundColor", int.class);
                    setCardBackgroundColorMethod.invoke(cardView, cardColor);
                } catch (Exception e) {
                    cardView.setBackgroundColor(cardColor);
                    Log.e(TAG, "Failed to set CardView background: " + e.getMessage());
                }
            }
            
            if (itemContainer != null) {

                itemContainer.setBackgroundColor(Color.TRANSPARENT);
            }


            boolean isDarkColor = isColorDark(primaryColor);
            int textColor = isDarkColor ? Color.WHITE : Color.BLACK;
            habitText.setTextColor(textColor);

            HabitItem habit = getItem(position);
            habitText.setText(habit.name);

            int streak = streaks.containsKey(habit.name) ? streaks.get(habit.name) : 0;

            long lastMarkedTime = sharedPreferences.getLong(habit.name + "_last_marked", 0);
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
                streakText.setTextColor(textColor);

                streakIcon.setImageResource(isMarked ? R.drawable.streak_fire : R.drawable.streak_icon_u);

                streakIcon.setColorFilter(isMarked ? Color.YELLOW : Color.GRAY);
            } else {
                streakIcon.setVisibility(View.GONE);
                streakText.setVisibility(View.GONE);
            }

            return convertView;
        }


        private int darkenColor(int color, float factor) {
            int alpha = Color.alpha(color);
            int red = (int) (Color.red(color) * factor);
            int green = (int) (Color.green(color) * factor);
            int blue = (int) (Color.blue(color) * factor);
            return Color.argb(alpha, Math.max(red, 0), Math.max(green, 0), Math.max(blue, 0));
        }

       
        private boolean isColorDark(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness > 0.5;
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

        if (alarmManager == null) {
            Log.d(TAG, "AlarmManager is null, cannot cancel reminders");
            return;
        }
        
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


    private boolean isUserLoggedIn() {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User signed in with Firebase: " + 
                (currentUser.getEmail() != null ? currentUser.getEmail() : "No email"));
            return true;
        }

        SharedPreferences localPrefs = getSharedPreferences("LocalAuth", Context.MODE_PRIVATE);
        boolean isLocallyLoggedIn = localPrefs.getBoolean("is_logged_in", false);
        
        if (isLocallyLoggedIn) {
            String email = localPrefs.getString("user_email", "No email");
            Log.d(TAG, "User signed in locally: " + email);
            return true;
        }
        
        return false;
    }

    private void checkAndUpdateStreaks() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();
        

        for (HabitItem habit : habits) {
            long lastMarkedTime = sharedPreferences.getLong(habit.name + "_last_marked", 0);
            int currentStreak = sharedPreferences.getInt(habit.name + "_streak", 0);
            

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

                int currentPoints = sharedPreferences.getInt(habit.name + "_points", 100);
                int deduction = (int) (5 * daysPassed); // 5 points per day
                int newPoints = Math.max(0, currentPoints - deduction);
                

                sharedPreferences.edit()
                    .putInt(habit.name + "_streak", 0)
                    .putInt(habit.name + "_points", newPoints)
                    .apply();
                

                streaks.put(habit.name, 0);
                

                long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                if (lastMarkedTime > oneWeekAgo) {
                    Toast.makeText(this, habit.name + ": Streak lost! -" + deduction + 
                        " points (" + daysPassed + " days missed)", Toast.LENGTH_SHORT).show();
                }
            }
        }

        habitAdapter.notifyDataSetChanged();
    }

    private void scheduleDailyHabitUpdate() {
        Log.d(TAG, "Scheduling daily habit update alarm");
        

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 5);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        

        Intent dailyUpdateIntent = new Intent(this, DailyUpdateReceiver.class);
        dailyUpdateIntent.setAction("com.example.goaltracker.DAILY_UPDATE");
        

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            dailyUpdateIntent,
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
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    );
                    
                    Log.d(TAG, "Daily habit update alarm scheduled (inexact) for " + calendar.getTime());
                } else {

                    alarmManager.setExactAndAllowWhileIdle(
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
                    
                    Log.d(TAG, "Daily habit update alarm scheduled (exact) for " + calendar.getTime());
                }
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                );
                
                Log.d(TAG, "Daily habit update alarm scheduled for " + calendar.getTime());
            }
            

            sharedPreferences.edit().putBoolean("daily_update_scheduled", true).apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling daily habit update: " + e.getMessage());
        }
    }

    private void applyThemeColors() {
        int primaryColor = ThemeManager.getPrimaryColor(this);

        fixStatusBarColor();
        addButton.setBackgroundColor(primaryColor);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View mainLayout = ((ViewGroup) rootView).getChildAt(0);
            if (mainLayout != null) {

                int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                mainLayout.setBackgroundColor(lightPrimaryColor);
            }
        }

        if (habitAdapter != null) {
            habitAdapter.notifyDataSetChanged();
        }
        

        if (habitListView != null) {
            habitListView.setDivider(new android.graphics.drawable.ColorDrawable(primaryColor));
            habitListView.setDividerHeight(1);
        }
        

        ThemeManager.applyNavigationButtonStyle(calendarButton);
        ThemeManager.applyNavigationButtonStyle(statsButton);
        ThemeManager.applyNavigationButtonStyle(reminderButton);
        ThemeManager.applyNavigationButtonStyle(settingsButton);
        ThemeManager.applyNavigationButtonStyle(profileButton);
        ThemeManager.applyNavigationButtonStyle(themeToggleButton);
    }


    private int lightenColor(int color, float factor) {
        int alpha = Color.alpha(color);
        int red = (int) (Color.red(color) + (255 - Color.red(color)) * factor);
        int green = (int) (Color.green(color) + (255 - Color.green(color)) * factor);
        int blue = (int) (Color.blue(color) + (255 - Color.blue(color)) * factor);
        return Color.argb(alpha, Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
    }

    private void fixStatusBarColor() {
        int primaryColor = ThemeManager.getPrimaryColor(this);

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
    }
}
