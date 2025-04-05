package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HabitCalendarActivity extends AppCompatActivity {
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private ListView completedHabitsListView;
    private ArrayAdapter<String> habitsAdapter;
    private List<String> completedHabits;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;
    private String habitName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
        habitName = getIntent().getStringExtra("habit_name");
        dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        completedHabits = new ArrayList<>();

        calendarView = findViewById(R.id.calendarView);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        completedHabitsListView = findViewById(R.id.completedHabitsListView);
        ImageButton backButton = findViewById(R.id.backButton);

        habitsAdapter = new ArrayAdapter<>(this, 
            R.layout.calendar_list_item, android.R.id.text1, completedHabits);
        completedHabitsListView.setAdapter(habitsAdapter);

        updateSelectedDate(Calendar.getInstance().getTimeInMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            updateSelectedDate(selectedCalendar.getTimeInMillis());
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void updateSelectedDate(long dateInMillis) {
        Date selectedDate = new Date(dateInMillis);
        String formattedDate = dateFormat.format(selectedDate);
        selectedDateTextView.setText("Selected Date: " + formattedDate);

        completedHabits.clear();

        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(dateInMillis);
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);
        long selectedDayStart = selectedCal.getTimeInMillis();
        
        selectedCal.add(Calendar.DAY_OF_MONTH, 1);
        long selectedDayEnd = selectedCal.getTimeInMillis();

        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
        if (lastMarkedTime >= selectedDayStart && lastMarkedTime < selectedDayEnd) {
            completedHabits.add(habitName + " ");
        } else {
            completedHabits.add("Not completed on this date");
        }

        habitsAdapter.notifyDataSetChanged();
    }
}