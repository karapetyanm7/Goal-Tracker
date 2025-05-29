package com.example.goaltracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import android.view.ViewGroup;

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
        applyThemeColors();
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
        int completions = mPrefs.getInt(habitName + "_completed_count", 0);
        int points = mPrefs.getInt(habitName + "_points", 0);
        completedEverTextView.setText(String.valueOf(completions));
        
        long lastMarkedDate = mPrefs.getLong(habitName + "_last_marked", 0);
        
        int weeklyCompletions = getCompletionsForCurrentWeek(lastMarkedDate);
        int monthlyCompletions = getCompletionsForCurrentMonth(lastMarkedDate);
        
        completedThisWeekTextView.setText(String.valueOf(weeklyCompletions));
        completedThisMonthTextView.setText(String.valueOf(monthlyCompletions));
        
        int currentStreak = mPrefs.getInt(habitName + "_streak", 0);
        int maxStreak = mPrefs.getInt(habitName + "_max_streak", 0);
        long creationDate = mPrefs.getLong(habitName + "_created", System.currentTimeMillis());
        
        long daysSinceCreation = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - creationDate) + 1;
        
        double successRate = daysSinceCreation > 0 ? 
            Math.min(100, Math.round((completions * 100.0) / daysSinceCreation)) : 0;
        
        double consistencyScore = daysSinceCreation > 0 ? 
            Math.min(100, Math.round((currentStreak * 100.0) / daysSinceCreation)) : 0;

        int textColor = ContextCompat.getColor(this, R.color.text_primary);
        
        addAdvancedStats(advancedStatsContainer, points, currentStreak, maxStreak, 
                points, creationDate, textColor, (int)daysSinceCreation, (int)successRate, 
                (int)consistencyScore);
    }
    
    private String getMilestoneText(int points) {
        if (points < 200) {
            return "Milestone: " + points + "/200";
        } else if (points < 500) {
            return "Milestone: " + points + "/500";
        } else {
            return "Master Milestone: " + points + "+";
        }
    }
    
    private int getCompletionsForCurrentWeek(long lastMarkedDate) {
        if (lastMarkedDate == 0) return 0;
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastMarkedDate);
        
        Calendar currentCal = Calendar.getInstance();
        

        if (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
            cal.get(Calendar.WEEK_OF_YEAR) == currentCal.get(Calendar.WEEK_OF_YEAR)) {
            return 1;
        }
        
        return 0;
    }
    
    private int getCompletionsForCurrentMonth(long lastMarkedDate) {
        if (lastMarkedDate == 0) return 0;
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lastMarkedDate);
        
        Calendar currentCal = Calendar.getInstance();
        

        if (cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && 
            cal.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)) {
            return 1;
        }
        
        return 0;
    }
    
    private void addAdvancedStats(LinearLayout container, int points, int currentStreak, int maxStreak,
                               int pointsEarned, long creationDate, int textColor, int daysSinceCreation,
                               int successRate, int consistencyScore) {
        container.removeAllViews();


        addStreakCard(container, currentStreak, maxStreak, textColor, daysSinceCreation, 
                     successRate, consistencyScore);
    }
    
    private void addStreakCard(LinearLayout container, int currentStreak, int maxStreak, int textColor,
                             int daysSinceCreation, int successRate, int consistencyScore) {
        CardView cardView = new CardView(this);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.app_card_background));
        
        LinearLayout statsLayout = new LinearLayout(this);
        statsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        statsLayout.setOrientation(LinearLayout.VERTICAL);
        statsLayout.setPadding(16, 16, 16, 16);
        
        int points = mPrefs.getInt(habitName + "_points", 0);
        String milestoneText = getMilestoneText(points);
        int progress;
        if (points < 200) {
            progress = (points * 100) / 200;
        } else if (points < 500) {
            progress = ((points - 200) * 100) / 300;
        } else {
            progress = 100;
        }
        
        addProgressBar(statsLayout, milestoneText, progress, textColor);

        addStatRow(statsLayout, "Success Rate:", successRate + "%", textColor);

        cardView.addView(statsLayout);
        container.addView(cardView);
    }
    
    private void addProgressBar(LinearLayout container, String label, int progress, int textColor) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 16, 0, 8);
        

        TextView labelView = new TextView(this);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        labelView.setText(label);
        labelView.setTextColor(textColor);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.addView(labelView);
        LinearLayout progressBarLayout = new LinearLayout(this);
        progressBarLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(16)
        ));
        progressBarLayout.setBackgroundResource(R.drawable.progress_bar_background);
        progressBarLayout.setPadding(2, 2, 2, 2);
        
        View progressView = new View(this);
        int width = (int)((progress / 100.0) * (getResources().getDisplayMetrics().widthPixels * 0.8));
        progressView.setLayoutParams(new LinearLayout.LayoutParams(
                width,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        progressView.setBackgroundResource(R.drawable.progress_bar);
        
        progressBarLayout.addView(progressView);
        row.addView(progressBarLayout);
        
        container.addView(row);
    }
    
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    private int calculateProgressToNextMilestone(int points) {
        if (points < 200) {
            return (points * 100) / 200;
        } else if (points < 500) {

            return ((points - 200) * 100) / 300;
        }
        return 100;
    }
    
    private String formatWithEmoji(int value, String emoji) {
        if (value > 0) {
            return value + " " + emoji;
        }
        return String.valueOf(value);
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
    

    
    private void applyThemeColors() {
        int primaryColor = ThemeManager.getPrimaryColor(this);

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