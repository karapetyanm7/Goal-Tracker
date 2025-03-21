package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class HabitStatsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private String habitName;
    private TextView habitNameTextView;
    private TextView completedEverTextView;
    private TextView completedThisWeekTextView;
    private TextView completedThisMonthTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_stats);

        prefs = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
        habitName = getIntent().getStringExtra("habitName");

        initializeViews();
        setupBackButton();
        updateStats();
    }

    private void initializeViews() {
        habitNameTextView = findViewById(R.id.habitNameTextView);
        completedEverTextView = findViewById(R.id.completedEverTextView);
        completedThisWeekTextView = findViewById(R.id.completedThisWeekTextView);
        completedThisMonthTextView = findViewById(R.id.completedThisMonthTextView);
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void updateStats() {
        habitNameTextView.setText(habitName);

        int completedCount = prefs.getInt(habitName + "_completed", 0);
        completedEverTextView.setText("Completed (Ever): " + completedCount);

        int completedThisWeek = calculateCompletedThisWeek();
        completedThisWeekTextView.setText("Completed This Week: " + completedThisWeek);

        int completedThisMonth = calculateCompletedThisMonth();
        completedThisMonthTextView.setText("Completed This Month: " + completedThisMonth);
    }

    private int calculateCompletedThisWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        Set<String> markedDates = prefs.getStringSet(habitName + "_markedDates", new HashSet<>());
        int count = 0;

        for (String dateStr : markedDates) {
            long timestamp = Long.parseLong(dateStr);
            if (timestamp >= calendar.getTimeInMillis()) {
                count++;
            }
        }

        return count;
    }

    private int calculateCompletedThisMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Set<String> markedDates = prefs.getStringSet(habitName + "_markedDates", new HashSet<>());
        int count = 0;

        for (String dateStr : markedDates) {
            long timestamp = Long.parseLong(dateStr);
            if (timestamp >= calendar.getTimeInMillis()) {
                count++;
            }
        }

        return count;
    }
} 