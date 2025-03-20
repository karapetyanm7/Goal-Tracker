package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private static final String TAG = "CalendarActivity";
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private LinearLayout habitsListLayout;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy");

            calendarView = findViewById(R.id.calendarView);
            selectedDateTextView = findViewById(R.id.selectedDateTextView);
            habitsListLayout = findViewById(R.id.habitsListLayout);
            ImageButton backButton = findViewById(R.id.backButton);

            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());
            if (habits.isEmpty()) {
                Log.d(TAG, "No habits found, creating test habit");
                habits.add("Test Habit");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("habits", habits);
                editor.putInt("Test Habit_points", 0);
                editor.putInt("Test Habit_streak", 0);
                editor.putLong("Test Habit_last_marked", 0);
                editor.putLong("Test Habit_created", System.currentTimeMillis());
                editor.apply();
                Toast.makeText(this, "Created test habit", Toast.LENGTH_SHORT).show();
            }

            updateSelectedDate(calendarView.getDate());

            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                updateSelectedDate(calendar.getTimeInMillis());
            });

            backButton.setOnClickListener(v -> finish());
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing calendar view", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedDate(long dateInMillis) {
        try {
            Date date = new Date(dateInMillis);
            selectedDateTextView.setText("Selected Date: " + dateFormat.format(date));
            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(dateInMillis);
            selectedCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedCal.set(Calendar.MINUTE, 0);
            selectedCal.set(Calendar.SECOND, 0);
            selectedCal.set(Calendar.MILLISECOND, 0);
            
            Calendar currentCal = Calendar.getInstance();
            currentCal.set(Calendar.HOUR_OF_DAY, 0);
            currentCal.set(Calendar.MINUTE, 0);
            currentCal.set(Calendar.SECOND, 0);
            currentCal.set(Calendar.MILLISECOND, 0);
            
            if (selectedCal.after(currentCal)) {
                habitsListLayout.removeAllViews();
                TextView futureDateText = new TextView(this);
                futureDateText.setText("No habits available for future dates");
                futureDateText.setTextSize(16);
                futureDateText.setGravity(View.TEXT_ALIGNMENT_CENTER);
                futureDateText.setPadding(16, 16, 16, 16);
                habitsListLayout.addView(futureDateText);
                return;
            }
            
            updateHabitsList(dateInMillis);
        } catch (Exception e) {
            Log.e(TAG, "Error updating selected date", e);
            Toast.makeText(this, "Error updating date", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateHabitsList(long dateInMillis) {
        try {
            habitsListLayout.removeAllViews();

            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());
            Log.d(TAG, "Found " + habits.size() + " habits");

            Toast.makeText(this, "Found " + habits.size() + " habits", Toast.LENGTH_SHORT).show();

            for (String habit : habits) {
                Log.d(TAG, "Habit: " + habit);
            }

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(dateInMillis);
            selectedCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedCal.set(Calendar.MINUTE, 0);
            selectedCal.set(Calendar.SECOND, 0);
            selectedCal.set(Calendar.MILLISECOND, 0);
            long startOfDay = selectedCal.getTimeInMillis();
            selectedCal.set(Calendar.HOUR_OF_DAY, 23);
            selectedCal.set(Calendar.MINUTE, 59);
            selectedCal.set(Calendar.SECOND, 59);
            selectedCal.set(Calendar.MILLISECOND, 999);
            long endOfDay = selectedCal.getTimeInMillis();

            boolean hasValidHabits = false;

            for (String habitName : habits) {
                Log.d(TAG, "Processing habit: " + habitName);

                long createdDate = sharedPreferences.getLong(habitName + "_created", System.currentTimeMillis());

                if (createdDate > endOfDay) {
                    Log.d(TAG, "Skipping habit " + habitName + " - created after selected date");
                    continue;
                }
                
                hasValidHabits = true;
                
                View habitItem = getLayoutInflater().inflate(R.layout.calendar_habit_item, habitsListLayout, false);
                
                TextView habitText = habitItem.findViewById(R.id.habitText);
                ImageView streakIcon = habitItem.findViewById(R.id.streakIcon);
                TextView streakText = habitItem.findViewById(R.id.streakText);
                ImageView markCompleteButton = habitItem.findViewById(R.id.markCompleteButton);

                int points = sharedPreferences.getInt(habitName + "_points", 0);
                int streak = sharedPreferences.getInt(habitName + "_streak", 0);
                long lastMarked = sharedPreferences.getLong(habitName + "_last_marked", 0);
                
                Log.d(TAG, "Habit data - Points: " + points + ", Streak: " + streak + ", Last Marked: " + lastMarked);

                boolean isDone = lastMarked >= startOfDay && lastMarked <= endOfDay;
                Log.d(TAG, "Habit " + habitName + " marked status: " + isDone);

                String status = isDone ? " (Done)" : " (Not Done)";
                habitText.setText(habitName + status);
                streakText.setText(String.valueOf(streak));

                if (streak > 0) {
                    streakIcon.setVisibility(View.VISIBLE);
                    streakText.setVisibility(View.VISIBLE);
                } else {
                    streakIcon.setVisibility(View.GONE);
                    streakText.setVisibility(View.GONE);
                }

                markCompleteButton.setImageResource(isDone ? R.drawable.ic_unmark : R.drawable.ic_mark);
                habitItem.setBackgroundResource(isDone ? 
                    R.drawable.lavender_border : 
                    android.R.color.transparent);
                
                habitsListLayout.addView(habitItem);
                Log.d(TAG, "Added habit item to layout: " + habitName);
            }
            if (!hasValidHabits) {
                TextView noHabitsText = new TextView(this);
                noHabitsText.setText("No habits were created on this date");
                noHabitsText.setTextSize(16);
                noHabitsText.setGravity(View.TEXT_ALIGNMENT_CENTER);
                noHabitsText.setPadding(16, 16, 16, 16);
                habitsListLayout.addView(noHabitsText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating habits list", e);
            Toast.makeText(this, "Error updating habits list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
} 