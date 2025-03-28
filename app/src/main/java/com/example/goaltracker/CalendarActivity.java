package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private static final String TAG = "CalendarActivity";
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private ListView completedHabitsListView;
    private ArrayAdapter<String> habitsAdapter;
    private List<String> completedHabits;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            completedHabits = new ArrayList<>();

            calendarView = findViewById(R.id.calendarView);
            selectedDateTextView = findViewById(R.id.selectedDateTextView);
            completedHabitsListView = findViewById(R.id.completedHabitsListView);
            ImageButton backButton = findViewById(R.id.backButton);

            habitsAdapter = new ArrayAdapter<>(this, 
                R.layout.calendar_list_item, android.R.id.text1, completedHabits);
            completedHabitsListView.setAdapter(habitsAdapter);

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
            }


            updateSelectedDate(Calendar.getInstance().getTimeInMillis());

            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                updateSelectedDate(selectedCalendar.getTimeInMillis());
            });

            backButton.setOnClickListener(v -> finish());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing calendar", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedDate(long dateInMillis) {
        try {
            Date selectedDate = new Date(dateInMillis);
            String formattedDate = dateFormat.format(selectedDate);
            selectedDateTextView.setText("Selected Date: " + formattedDate);


            completedHabits.clear();


            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());

            for (String habit : habits) {
                long lastMarkedTime = sharedPreferences.getLong(habit + "_last_marked", 0);
                if (lastMarkedTime > 0) {
                    Calendar habitCal = Calendar.getInstance();
                    habitCal.setTimeInMillis(lastMarkedTime);
                    
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.setTimeInMillis(dateInMillis);
                    
                    if (isSameDay(habitCal, selectedCal)) {
                        completedHabits.add(habit);
                    }
                }
            }


            habitsAdapter.notifyDataSetChanged();

            if (completedHabits.isEmpty()) {
                completedHabits.add("No habits completed on this date");
                habitsAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating selected date", e);
            Toast.makeText(this, "Error updating date", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
}