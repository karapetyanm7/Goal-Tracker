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
    private TextView longestStreakTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            
            weeklyStatsTextView = findViewById(R.id.weeklyStatsTextView);
            monthlyStatsTextView = findViewById(R.id.monthlyStatsTextView);
            longestStreakTextView = findViewById(R.id.longestStreakTextView);
            
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
            if (habits.isEmpty()) {
                updateEmptyStats();
                return;
            }

            Calendar calendar = Calendar.getInstance();
            long currentTime = calendar.getTimeInMillis();

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.DAY_OF_WEEK, -calendar.get(Calendar.DAY_OF_WEEK) + 1);
            long weekStart = calendar.getTimeInMillis();

            calendar.set(Calendar.DAY_OF_MONTH, 1);
            long monthStart = calendar.getTimeInMillis();

            int weeklyCompleted = 0;
            int monthlyCompleted = 0;
            int maxStreak = 0;

            for (String habitName : habits) {
                long lastMarked = sharedPreferences.getLong(habitName + "_last_marked", 0);
                int streak = sharedPreferences.getInt(habitName + "_max_streak", 0);

                if (lastMarked >= weekStart) {
                    weeklyCompleted++;
                }
                if (lastMarked >= monthStart) {
                    monthlyCompleted++;
                }

                maxStreak = Math.max(maxStreak, streak);
            }

            weeklyStatsTextView.setText("Weekly Completed Habits: " + weeklyCompleted);
            monthlyStatsTextView.setText("Monthly Completed Habits: " + monthlyCompleted);
            longestStreakTextView.setText("Longest Streak: " + maxStreak + " days");

        } catch (Exception e) {
            Log.e(TAG, "Error updating stats", e);
            updateEmptyStats();
        }
    }

    private void updateEmptyStats() {
        weeklyStatsTextView.setText("Weekly Completed Habits: 0");
        monthlyStatsTextView.setText("Monthly Completed Habits: 0");
        longestStreakTextView.setText("Longest Streak: 0 days");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }
}