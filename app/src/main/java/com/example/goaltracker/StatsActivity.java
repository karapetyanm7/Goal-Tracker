package com.example.goaltracker;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";
    private SharedPreferences sharedPreferences;
    private TextView weeklyStatsTextView;
    private TextView monthlyStatsTextView;
    private TextView longestStreakTextView;
    private TextView totalPointsTextView;
    private TextView completionRateTextView;
    private TextView mostConsistentHabitTextView;
    private LinearLayout statsContainer;
    private int themeColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        ThemeManager.applyTheme(this);
        

        if (!GoalTrackerApp.checkAuthenticationStatus(this)) {
            return;
        }
        
        setContentView(R.layout.activity_stats);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            
            weeklyStatsTextView = findViewById(R.id.weeklyStatsTextView);
            monthlyStatsTextView = findViewById(R.id.monthlyStatsTextView);
            longestStreakTextView = findViewById(R.id.longestStreakTextView);
            totalPointsTextView = findViewById(R.id.totalPointsTextView);
            completionRateTextView = findViewById(R.id.completionRateTextView);
            mostConsistentHabitTextView = findViewById(R.id.mostConsistentHabitTextView);
            statsContainer = findViewById(R.id.statsContainer);
            
            ImageButton backButton = findViewById(R.id.backButton);
            ThemeManager.applyNavigationButtonStyle(backButton);
            backButton.setOnClickListener(v -> finish());
            
            applyThemeColors();
            
            updateStats();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        if (GoalTrackerApp.checkAuthenticationStatus(this)) {

            updateStats();
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
            

            Calendar weekCal = (Calendar) calendar.clone();
            weekCal.add(Calendar.DAY_OF_WEEK, -weekCal.get(Calendar.DAY_OF_WEEK) + 1);
            long weekStart = weekCal.getTimeInMillis();
            

            Calendar prevWeekCal = (Calendar) weekCal.clone();
            prevWeekCal.add(Calendar.WEEK_OF_YEAR, -1);
            long prevWeekStart = prevWeekCal.getTimeInMillis();
            

            Calendar monthCal = (Calendar) calendar.clone();
            monthCal.set(Calendar.DAY_OF_MONTH, 1);
            long monthStart = monthCal.getTimeInMillis();
            

            Calendar prevMonthCal = (Calendar) monthCal.clone();
            prevMonthCal.add(Calendar.MONTH, -1);
            long prevMonthStart = prevMonthCal.getTimeInMillis();

            int weeklyCompleted = 0;
            int monthlyCompleted = 0;
            int prevWeeklyCompleted = 0;
            int prevMonthlyCompleted = 0;
            int maxStreak = 0;
            int totalPoints = 0;
            
            Map<String, Integer> habitStreaks = new HashMap<>();
            Map<String, Integer> habitPoints = new HashMap<>();
            

            int totalHabits = habits.size();
            
            for (String habitName : habits) {
                long lastMarked = sharedPreferences.getLong(habitName + "_last_marked", 0);
                int streak = sharedPreferences.getInt(habitName + "_max_streak", 0);
                int points = sharedPreferences.getInt(habitName + "_points", 0);
                

                habitStreaks.put(habitName, streak);
                habitPoints.put(habitName, points);
                

                totalPoints += points;
                

                if (lastMarked >= weekStart) {
                    weeklyCompleted++;
                }
                if (lastMarked >= monthStart) {
                    monthlyCompleted++;
                }
                

                if (lastMarked >= prevWeekStart && lastMarked < weekStart) {
                    prevWeeklyCompleted++;
                }
                if (lastMarked >= prevMonthStart && lastMarked < monthStart) {
                    prevMonthlyCompleted++;
                }
                

                maxStreak = Math.max(maxStreak, streak);
            }
            

            String mostConsistentHabit = "";
            int highestMetric = 0;
            
            for (Map.Entry<String, Integer> entry : habitStreaks.entrySet()) {
                if (entry.getValue() > highestMetric) {
                    highestMetric = entry.getValue();
                    mostConsistentHabit = entry.getKey();
                } else if (entry.getValue() == highestMetric) {

                    if (habitPoints.get(entry.getKey()) > habitPoints.get(mostConsistentHabit)) {
                        mostConsistentHabit = entry.getKey();
                    }
                }
            }
            

            double weeklyRate = totalHabits > 0 ? (weeklyCompleted * 100.0 / totalHabits) : 0;
            double monthlyRate = totalHabits > 0 ? (monthlyCompleted * 100.0 / totalHabits) : 0;
            

            String weeklyImprovement = calculateImprovement(weeklyCompleted, prevWeeklyCompleted);
            String monthlyImprovement = calculateImprovement(monthlyCompleted, prevMonthlyCompleted);
            

            weeklyStatsTextView.setText("Weekly Completed Habits: " + weeklyCompleted);
            monthlyStatsTextView.setText("Monthly Completed Habits: " + monthlyCompleted);
            longestStreakTextView.setText("Longest Streak: " + maxStreak + " days");
            totalPointsTextView.setText("Total Points Earned: " + totalPoints);
            
            String completionRateText = String.format("Completion Rate: %.1f%% weekly, %.1f%% monthly", weeklyRate, monthlyRate);
            completionRateTextView.setText(completionRateText);
            
            if (!mostConsistentHabit.isEmpty()) {
                mostConsistentHabitTextView.setText("Most Consistent Habit: " + mostConsistentHabit);
            } else {
                mostConsistentHabitTextView.setText("Most Consistent Habit: None");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating stats", e);
            updateEmptyStats();
        }
    }
    
    private String calculateImprovement(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? "↑ New activity" : "― No change";
        }
        
        double percentChange = ((current - previous) * 100.0) / previous;
        
        if (Math.abs(percentChange) < 1) {
            return "― Stable";
        } else if (percentChange > 0) {
            return String.format("↑ +%.1f%%", percentChange);
        } else {
            return String.format("↓ %.1f%%", percentChange);
        }
    }

    private void updateEmptyStats() {
        weeklyStatsTextView.setText("Weekly Completed Habits: 0");
        monthlyStatsTextView.setText("Monthly Completed Habits: 0");
        longestStreakTextView.setText("Longest Streak: 0 days");
        totalPointsTextView.setText("Total Points Earned: 0");
        completionRateTextView.setText("Completion Rate: 0.0% weekly, 0.0% monthly");
        mostConsistentHabitTextView.setText("Most Consistent Habit: None");
    }

    

    private void applyThemeColors() {
        int primaryColor = ThemeManager.getPrimaryColor(this);
        this.themeColor = primaryColor;
        

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View mainLayout = ((ViewGroup) rootView).getChildAt(0);
            if (mainLayout != null) {

                int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                mainLayout.setBackgroundColor(lightPrimaryColor);
            }
        }
        

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        int statusBarColor = darkenColor(primaryColor, 0.2f);
        getWindow().setStatusBarColor(statusBarColor);

        getWindow().setNavigationBarColor(statusBarColor);
        

        if (statsContainer != null) {

            for (int i = 0; i < statsContainer.getChildCount(); i++) {
                View child = statsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    textView.setTextColor(darkenColor(primaryColor, 0.3f));
                }
            }
            

            int veryLightColor = lightenColor(primaryColor, 0.95f);
            statsContainer.setBackgroundColor(veryLightColor);
            

            statsContainer.setBackgroundResource(R.drawable.stats_border);
            

            TextView titleTextView = findViewById(R.id.statsTitle);
            if (titleTextView != null) {
                titleTextView.setTextColor(primaryColor);
            }
        }
    }
    

    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }
    

    private int darkenColor(int color, float factor) {
        int red = (int) (Color.red(color) * (1 - factor));
        int green = (int) (Color.green(color) * (1 - factor));
        int blue = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(red, green, blue);
    }
}