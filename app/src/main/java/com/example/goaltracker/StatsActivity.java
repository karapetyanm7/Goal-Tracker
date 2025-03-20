package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private SharedPreferences sharedPreferences;
    private TextView weeklyStatsTextView;
    private TextView monthlyStatsTextView;
    private TextView currentMaxStreakTextView;
    private TextView allTimeMaxStreakTextView;
    private TextView totalHabitsTextView;
    private TextView totalCompletedTextView;
    private TextView streak10PlusTextView;
    private TextView streak100PlusTextView;
    private TextView streak1000PlusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            weeklyStatsTextView = findViewById(R.id.weeklyStatsTextView);
            monthlyStatsTextView = findViewById(R.id.monthlyStatsTextView);
            currentMaxStreakTextView = findViewById(R.id.currentMaxStreakTextView);
            allTimeMaxStreakTextView = findViewById(R.id.allTimeMaxStreakTextView);
            totalHabitsTextView = findViewById(R.id.totalHabitsTextView);
            totalCompletedTextView = findViewById(R.id.totalCompletedTextView);
            streak10PlusTextView = findViewById(R.id.streak10PlusTextView);
            streak100PlusTextView = findViewById(R.id.streak100PlusTextView);
            streak1000PlusTextView = findViewById(R.id.streak1000PlusTextView);
            ImageButton backButton = findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish());
            updateStats();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    private void updateStats() {
        try {
            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());
            int totalHabits = habits.size();
            totalHabitsTextView.setText("Total Habits: " + totalHabits);
            Calendar calendar = Calendar.getInstance();
            long currentTime = calendar.getTimeInMillis();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DAY_OF_WEEK, -calendar.get(Calendar.DAY_OF_WEEK) + 1);
            long weekStart = calendar.getTimeInMillis();
            calendar.add(Calendar.MONTH, -1);
            long monthStart = calendar.getTimeInMillis();
            int weeklyCompleted = 0;
            int monthlyCompleted = 0;
            int totalCompleted = 0;
            int streak10Plus = 0;
            int streak100Plus = 0;
            int streak1000Plus = 0;
            String currentMaxStreakHabit = "None";
            int currentMaxStreak = 0;
            String allTimeMaxStreakHabit = "None";
            int allTimeMaxStreak = 0;

            for (String habitName : habits) {
                long lastMarked = sharedPreferences.getLong(habitName + "_last_marked", 0);
                int currentStreak = sharedPreferences.getInt(habitName + "_streak", 0);
                int maxStreak = sharedPreferences.getInt(habitName + "_max_streak", 0);

                if (lastMarked > 0) {
                    totalCompleted++;
                    if (lastMarked >= weekStart) weeklyCompleted++;
                    if (lastMarked >= monthStart) monthlyCompleted++;
                }

                if (currentStreak >= 10) streak10Plus++;
                if (currentStreak >= 100) streak100Plus++;
                if (currentStreak >= 1000) streak1000Plus++;

                if (currentStreak > currentMaxStreak) {
                    currentMaxStreak = currentStreak;
                    currentMaxStreakHabit = habitName;
                }
                if (maxStreak > allTimeMaxStreak) {
                    allTimeMaxStreak = maxStreak;
                    allTimeMaxStreakHabit = habitName;
                }
            }

            weeklyStatsTextView.setText("Weekly Completed Habits: " + weeklyCompleted);
            monthlyStatsTextView.setText("Monthly Completed Habits: " + monthlyCompleted);
            currentMaxStreakTextView.setText("Current Max Streak: " + currentMaxStreakHabit + " (" + currentMaxStreak + ")");
            allTimeMaxStreakTextView.setText("All-Time Max Streak: " + allTimeMaxStreakHabit + " (" + allTimeMaxStreak + ")");
            totalCompletedTextView.setText("Total Completed Habits: " + totalCompleted);
            streak10PlusTextView.setText("Habits with 10+ Streak: " + streak10Plus);
            streak100PlusTextView.setText("Habits with 100+ Streak: " + streak100Plus);
            streak1000PlusTextView.setText("Habits with 1000+ Streak: " + streak1000Plus);

        } catch (Exception e) {
            Log.e(TAG, "Error updating stats", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }
} 