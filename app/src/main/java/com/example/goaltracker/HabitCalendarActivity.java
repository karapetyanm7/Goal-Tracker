package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class HabitCalendarActivity extends AppCompatActivity {
    private static final String TAG = "HabitCalendarActivity";
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private TextView completionStatusTextView;
    private TextView habitNameTextView;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;
    private String habitName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_calendar);

        try {
            habitName = getIntent().getStringExtra("habit_name");
            if (habitName == null) {
                Toast.makeText(this, "Error: Habit name not provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            calendarView = findViewById(R.id.calendarView);
            selectedDateTextView = findViewById(R.id.selectedDateTextView);
            completionStatusTextView = findViewById(R.id.completionStatusTextView);
            habitNameTextView = findViewById(R.id.habitNameTextView);
            ImageButton backButton = findViewById(R.id.backButton);
            habitNameTextView.setText(habitName + " Calendar");
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
                completionStatusTextView.setText("Completion Status: Not available for future dates");
                return;
            }
            long startOfDay = selectedCal.getTimeInMillis();
            selectedCal.set(Calendar.HOUR_OF_DAY, 23);
            selectedCal.set(Calendar.MINUTE, 59);
            selectedCal.set(Calendar.SECOND, 59);
            selectedCal.set(Calendar.MILLISECOND, 999);
            long endOfDay = selectedCal.getTimeInMillis();
            long createdDate = sharedPreferences.getLong(habitName + "_created", System.currentTimeMillis());
            if (createdDate > endOfDay) {
                completionStatusTextView.setText("Completion Status: Habit not created yet");
                return;
            }
            long lastMarked = sharedPreferences.getLong(habitName + "_last_marked", 0);

            boolean isDone = lastMarked >= startOfDay && lastMarked <= endOfDay;

            String status = isDone ? "Completed" : "Not completed";
            completionStatusTextView.setText("Completion Status: " + status);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating selected date", e);
            Toast.makeText(this, "Error updating date", Toast.LENGTH_SHORT).show();
        }
    }
} 