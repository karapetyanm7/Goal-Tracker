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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);

        habits = new ArrayList<>(sharedPreferences.getStringSet("habits", new HashSet<>()));
        streaks = loadStreaks();

        habitListView = findViewById(R.id.habitListView);
        habitAdapter = new HabitAdapter(this, habits);
        habitListView.setAdapter(habitAdapter);

        addButton = findViewById(R.id.addButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);
        calendarButton = findViewById(R.id.calendarButton);
        statsButton = findViewById(R.id.statsButton);
        reminderButton = findViewById(R.id.reminderButton);

        if (addButton == null || themeToggleButton == null || calendarButton == null || 
            statsButton == null || reminderButton == null) {
            Toast.makeText(this, "Error initializing buttons", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        updateThemeToggleButton(isDarkMode);

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

        reminderButton.setOnClickListener(v -> showReminderDialog());

        habitDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String action = result.getData().getStringExtra("action");
                    String habitName = result.getData().getStringExtra("habit_name");
                    
                    if ("delete".equals(action)) {
                        habits.remove(habitName);
                        streaks.remove(habitName);
                        saveHabits();
                        saveStreaks();
                        habitAdapter.notifyDataSetChanged();
                    } else if ("edit".equals(action)) {
                        String oldName = result.getData().getStringExtra("old_habit_name");
                        String newName = result.getData().getStringExtra("new_habit_name");
                        int index = habits.indexOf(oldName);
                        if (index != -1) {
                            habits.set(index, newName);
                            
                            int streak = streaks.remove(oldName);
                            streaks.put(newName, streak);
                            
                            int points = sharedPreferences.getInt(oldName + "_points", 0);
                            long lastMarked = sharedPreferences.getLong(oldName + "_last_marked", 0);
                            
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(newName + "_points", points);
                            editor.putLong(newName + "_last_marked", lastMarked);
                            editor.remove(oldName + "_points");
                            editor.remove(oldName + "_last_marked");
                            editor.apply();
                            
                            saveHabits();
                            saveStreaks();
                            habitAdapter.notifyDataSetChanged();
                        }
                    } else if ("update".equals(action)) {
                        int points = result.getData().getIntExtra("habit_points", 0);
                        sharedPreferences.edit().putInt(habitName + "_points", points).apply();
                        int streak = sharedPreferences.getInt(habitName + "_streak", 0);
                        streaks.put(habitName, streak);
                        saveStreaks();
                        habitAdapter.notifyDataSetChanged();
                    }
                }
            }
        );

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        habitListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habit_name", habit);
                int points = sharedPreferences.getInt(habit + "_points", 100);
                int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;
                intent.putExtra("habit_points", points);
                intent.putExtra("habit_streak", streak);
                habitDetailLauncher.launch(intent);
            }
        });

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationHelper.createNotificationChannel(this);

        loadHabits();
    }

    private void updateThemeToggleButton(boolean isDarkMode) {
        if (isDarkMode) {
            themeToggleButton.setImageResource(R.drawable.light_icon);
        } else {
            themeToggleButton.setImageResource(R.drawable.dark_icon);
        }
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Habit");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString().trim();
                if (!habitName.isEmpty()) {
                    habits.add(habitName);
                    streaks.put(habitName, 0);
                    saveHabits();
                    saveStreaks();
                    habitAdapter.notifyDataSetChanged();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit");

        final EditText input = new EditText(this);
        input.setText(habits.get(position));
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString();
                if (!habitName.isEmpty()) {
                    String oldHabit = habits.get(position);
                    habits.set(position, habitName);

                    int streak = streaks.remove(oldHabit);
                    streaks.put(habitName, streak);

                    saveHabits();
                    saveStreaks();
                    habitAdapter.notifyDataSetChanged();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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
            ImageView markCompleteButton = convertView.findViewById(R.id.markCompleteButton);

            String habit = getItem(position);
            habitText.setText(habit);

            int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;

            if (streak >= 1) {
                streakIcon.setVisibility(View.VISIBLE);
                streakText.setVisibility(View.VISIBLE);
                streakText.setText(" " + streak);
            } else {
                streakIcon.setVisibility(View.GONE);
                streakText.setVisibility(View.GONE);
            }

            long lastMarkedTime = sharedPreferences.getLong(habit + "_last_marked", 0);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();
            boolean isMarked = lastMarkedTime >= startOfDay;

            markCompleteButton.setVisibility(View.VISIBLE);
            markCompleteButton.setImageResource(isMarked ? R.drawable.ic_unmark : R.drawable.ic_mark);

            markCompleteButton.setOnClickListener(v -> {
                if (!isMarked) {
                    int points = sharedPreferences.getInt(habit + "_points", 100);
                    points += 10;
                    streaks.put(habit, streak + 1);
                    int completedCount = sharedPreferences.getInt(habit + "_completed_count", 0);
                    completedCount++;

                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong(habit + "_last_marked", currentTime);
                    editor.putInt(habit + "_points", points);
                    editor.putInt(habit + "_completed_count", completedCount);
                    editor.apply();
                    
                    saveStreaks();
                    
                    Toast.makeText(getContext(), "+10 points added!", Toast.LENGTH_SHORT).show();
                } else {
                    int points = sharedPreferences.getInt(habit + "_points", 100);
                    points = Math.max(0, points - 10);
                    streaks.put(habit, Math.max(0, streak - 1));
                    int completedCount = sharedPreferences.getInt(habit + "_completed_count", 0);
                    completedCount = Math.max(0, completedCount - 1);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove(habit + "_last_marked");
                    editor.putInt(habit + "_points", points);
                    editor.putInt(habit + "_completed_count", completedCount);
                    editor.apply();
                    
                    saveStreaks();
                    
                    Toast.makeText(getContext(), "Habit unmarked", Toast.LENGTH_SHORT).show();
                }
                
                habitAdapter.notifyDataSetChanged();
            });

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
                if (mondayCheckBox.isChecked()) selectedDays |= Calendar.MONDAY;
                if (tuesdayCheckBox.isChecked()) selectedDays |= Calendar.TUESDAY;
                if (wednesdayCheckBox.isChecked()) selectedDays |= Calendar.WEDNESDAY;
                if (thursdayCheckBox.isChecked()) selectedDays |= Calendar.THURSDAY;
                if (fridayCheckBox.isChecked()) selectedDays |= Calendar.FRIDAY;
                if (saturdayCheckBox.isChecked()) selectedDays |= Calendar.SATURDAY;
                if (sundayCheckBox.isChecked()) selectedDays |= Calendar.SUNDAY;

                if (selectedDays != 0) {
                    scheduleWeeklyReminder(calendar, selectedDays);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void scheduleDailyReminder(Calendar calendar) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        );
        Toast.makeText(this, "Daily reminder set for " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + 
            calendar.get(Calendar.MINUTE), Toast.LENGTH_SHORT).show();
    }

    private void scheduleWeeklyReminder(Calendar calendar, int selectedDays) {
        cancelAllReminders();

        int requestCode = 0;
        Calendar now = Calendar.getInstance();
        
        for (int i = 0; i < 7; i++) {
            if ((selectedDays & (1 << i)) != 0) {
                Calendar dayCalendar = (Calendar) calendar.clone();
                dayCalendar.set(Calendar.DAY_OF_WEEK, i);
                if (dayCalendar.getTimeInMillis() <= now.getTimeInMillis()) {
                    dayCalendar.add(Calendar.DAY_OF_YEAR, 7);
                }

                Intent intent = new Intent(this, NotificationReceiver.class);
                PendingIntent dayIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode++,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    dayCalendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 7,
                    dayIntent
                );
            }
        }

        String days = "";
        if ((selectedDays & (1 << Calendar.MONDAY)) != 0) days += "Monday, ";
        if ((selectedDays & (1 << Calendar.TUESDAY)) != 0) days += "Tuesday, ";
        if ((selectedDays & (1 << Calendar.WEDNESDAY)) != 0) days += "Wednesday, ";
        if ((selectedDays & (1 << Calendar.THURSDAY)) != 0) days += "Thursday, ";
        if ((selectedDays & (1 << Calendar.FRIDAY)) != 0) days += "Friday, ";
        if ((selectedDays & (1 << Calendar.SATURDAY)) != 0) days += "Saturday, ";
        if ((selectedDays & (1 << Calendar.SUNDAY)) != 0) days += "Sunday, ";
        
        if (!days.isEmpty()) {
            days = days.substring(0, days.length() - 2);
            Toast.makeText(this, "Weekly reminders set for " + days + " at " + 
                calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleOneTimeReminder(Calendar calendar) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            pendingIntent
        );
        Toast.makeText(this, "One-time reminder set for tomorrow at " + 
            calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE), 
            Toast.LENGTH_SHORT).show();
    }

    private void scheduleOneTimeReminderForToday(Calendar calendar) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Cannot set reminder for past time today", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            pendingIntent
        );
        Toast.makeText(this, "One-time reminder set for today at " + 
            calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE), 
            Toast.LENGTH_SHORT).show();
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
}

