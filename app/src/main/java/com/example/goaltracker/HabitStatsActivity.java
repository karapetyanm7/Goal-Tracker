package com.example.goaltracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HabitStatsActivity extends AppCompatActivity {

    private SharedPreferences mPrefs;
    private String habitName;
    private TextView habitNameTextView;
    private TextView completedEverTextView;
    private TextView completedThisWeekTextView;
    private TextView completedThisMonthTextView;
    private LinearLayout advancedStatsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_habit_stats);

        mPrefs = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
        
        Intent intent = getIntent();
        habitName = intent.getStringExtra("habitName");
        
        initializeViews();
        setupBackButton();
        updateStats();
    }

    private void initializeViews() {
        habitNameTextView = findViewById(R.id.habitNameTextView);
        completedEverTextView = findViewById(R.id.completedEverTextView);
        completedThisWeekTextView = findViewById(R.id.completedThisWeekTextView);
        completedThisMonthTextView = findViewById(R.id.completedThisMonthTextView);
        advancedStatsContainer = findViewById(R.id.advancedStatsContainer);
        
        habitNameTextView.setText(habitName);
    }
    
    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setImageResource(R.drawable.back_icon);
        backButton.setOnClickListener(v -> finish());
    }
    
    private void updateStats() {
        // Get the total number of completions
        int completions = mPrefs.getInt(habitName + "_completed_count", 0);
        completedEverTextView.setText(String.valueOf(completions));
        
        // Get the last marked date
        long lastMarkedDate = mPrefs.getLong(habitName + "_last_marked", 0);
        
        // Calculate completions for current week and month
        int weeklyCompletions = getCompletionsForCurrentWeek(lastMarkedDate);
        int monthlyCompletions = getCompletionsForCurrentMonth(lastMarkedDate);
        
        completedThisWeekTextView.setText(String.valueOf(weeklyCompletions));
        completedThisMonthTextView.setText(String.valueOf(monthlyCompletions));
        
        // Add advanced statistics
        int currentStreak = mPrefs.getInt(habitName + "_streak", 0);
        int maxStreak = mPrefs.getInt(habitName + "_max_streak", 0);
        int points = mPrefs.getInt(habitName + "_points", 0);
        long creationDate = mPrefs.getLong(habitName + "_created", System.currentTimeMillis());
        
        boolean isDarkMode = ThemeManager.isDarkMode(this);
        int textColor = isDarkMode ? 
                ContextCompat.getColor(this, R.color.text_primary_dark) : 
                ContextCompat.getColor(this, R.color.text_primary);
        
        addAdvancedStats(advancedStatsContainer, completions, currentStreak, maxStreak, 
                points, creationDate, isDarkMode, textColor);
    }
    
    private int getCompletionsForCurrentWeek(long lastMarkedDate) {
        if (lastMarkedDate == 0) return 0;
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastMarkedDate);
        
        Calendar currentCal = Calendar.getInstance();
        
        // Check if the lastMarkedDate is in the current week
        if (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
            cal.get(Calendar.WEEK_OF_YEAR) == currentCal.get(Calendar.WEEK_OF_YEAR)) {
            return 1; // Assuming one completion per day
        }
        
        return 0;
    }
    
    private int getCompletionsForCurrentMonth(long lastMarkedDate) {
        if (lastMarkedDate == 0) return 0;
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastMarkedDate);
        
        Calendar currentCal = Calendar.getInstance();
        
        // Check if the lastMarkedDate is in the current month
        if (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
            cal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)) {
            return 1; // Assuming one completion per day
        }
        
        return 0;
    }
    
    private void addAdvancedStats(LinearLayout container, int completions, int currentStreak, 
                                 int maxStreak, int points, long creationDate, 
                                 boolean isDarkMode, int textColor) {
        // Clear any existing views
        container.removeAllViews();
        
        // Calculate days since creation
        long daysTracked = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - creationDate) + 1;
        if (daysTracked < 1) daysTracked = 1; // Ensure we don't divide by zero
        
        // Create a CardView for advanced stats
        CardView cardView = new CardView(this);
        CardView.LayoutParams cardParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 32);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(32);
        cardView.setElevation(8);
        cardView.setCardBackgroundColor(ThemeManager.getPrimaryColor(this));
        
        // Create a container for the stats
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        statsLayout.setOrientation(LinearLayout.VERTICAL);
        statsLayout.setPadding(32, 32, 32, 32);
        
        // Add the title
        addSectionTitle(statsLayout, "Advanced Statistics", Color.WHITE);
        
        // Add stats
        addSeparator(statsLayout);
        
        // Days tracked
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String startDate = sdf.format(new Date(creationDate));
        addStatRow(statsLayout, "Days Tracked:", String.valueOf(daysTracked), Color.WHITE);
        
        // Success Rate
        float successRate = (float) completions / daysTracked * 100;
        addStatRow(statsLayout, "Success Rate:", String.format("%.1f%%", successRate), Color.WHITE);
        
        // Average points per day
        float avgPointsPerDay = (float) points / daysTracked;
        addStatRow(statsLayout, "Average Points Per Day:", String.format("%.1f", avgPointsPerDay), Color.WHITE);
        
        // Streak Efficiency
        float streakEfficiency = daysTracked > 0 ? (float) maxStreak / daysTracked * 100 : 0;
        addStatRow(statsLayout, "Streak Efficiency:", String.format("%.1f%%", streakEfficiency), Color.WHITE);
        
        // Consistency Score
        int consistencyScore = calculateConsistencyScore(successRate, currentStreak, maxStreak);
        addStatRow(statsLayout, "Consistency Score:", consistencyScore + "/100", Color.WHITE);
        
        // Add feedback based on consistency score
        addSeparator(statsLayout);
        addSectionTitle(statsLayout, "Feedback", Color.WHITE);
        
        String feedback;
        if (consistencyScore >= 90) {
            feedback = "Excellent! You're maintaining this habit exceptionally well.";
        } else if (consistencyScore >= 70) {
            feedback = "Good job! You're consistently keeping up with this habit.";
        } else if (consistencyScore >= 50) {
            feedback = "You're doing okay, but there's room for improvement in consistency.";
        } else if (consistencyScore >= 30) {
            feedback = "You're struggling with consistency. Try setting reminders or adjusting your goal.";
        } else {
            feedback = "This habit needs more attention. Consider revising your approach or setting a more achievable goal.";
        }
        
        TextView feedbackView = new TextView(this);
        feedbackView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        feedbackView.setText(feedback);
        feedbackView.setTextColor(Color.WHITE);
        feedbackView.setPadding(0, 16, 0, 16);
        statsLayout.addView(feedbackView);
        
        // Add the stats layout to the card
        cardView.addView(statsLayout);
        
        // Add the card to the container
        container.addView(cardView);
    }
    
    private void addSectionTitle(LinearLayout container, String title, int textColor) {
        TextView titleView = new TextView(this);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleView.setText(title);
        titleView.setTextColor(textColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 16, 0, 16);
        container.addView(titleView);
    }
    
    private void addStatRow(LinearLayout container, String label, String value, int textColor) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        
        TextView labelView = new TextView(this);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        labelView.setText(label);
        labelView.setTextColor(textColor);
        
        TextView valueView = new TextView(this);
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        valueView.setText(value);
        valueView.setTextColor(textColor);
        valueView.setTypeface(null, Typeface.BOLD);
        
        row.addView(labelView);
        row.addView(valueView);
        container.addView(row);
    }
    
    private void addSeparator(LinearLayout container) {
        View separator = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        );
        params.setMargins(0, 16, 0, 16);
        separator.setLayoutParams(params);
        separator.setBackgroundColor(Color.WHITE);
        container.addView(separator);
    }
    
    private int calculateConsistencyScore(float successRate, int currentStreak, int maxStreak) {
        // Weight factors
        float successWeight = 0.5f;
        float currentStreakWeight = 0.3f;
        float maxStreakWeight = 0.2f;
        
        // Normalize streak values (assuming 30 days is a perfect score)
        float normalizedCurrentStreak = Math.min(currentStreak / 30.0f, 1.0f) * 100;
        float normalizedMaxStreak = Math.min(maxStreak / 30.0f, 1.0f) * 100;
        
        // Calculate weighted score
        float score = (successRate * successWeight) +
                     (normalizedCurrentStreak * currentStreakWeight) +
                     (normalizedMaxStreak * maxStreakWeight);
        
        return Math.round(score);
    }
}