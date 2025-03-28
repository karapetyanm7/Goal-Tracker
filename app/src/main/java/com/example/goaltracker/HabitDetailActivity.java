package com.example.goaltracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class HabitDetailActivity extends AppCompatActivity {

    private TextView habitNameTextView;
    private TextView habitPointsTextView;
    private TextView habitStreakTextView;
    private TextView habitMaxStreakTextView;
    private ImageView habitTreeImageView;
    private ImageView currentStreakIconImageView;
    private ImageView maxStreakIconImageView;
    private Button markCompleteButton;
    private ImageButton editButton;
    private ImageButton deleteButton;
    private ImageButton backButton;
    private ImageButton calendarButton;
    private ImageButton statsButton;
    private SharedPreferences sharedPreferences;
    private String habitName;
    private int habitPoints;
    private int currentStreak;
    private int maxStreak;
    private int completedCount;
    private boolean isMarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_detail);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);

        habitName = getIntent().getStringExtra("habit_name");
        habitPoints = getIntent().getIntExtra("habit_points", 100);
        currentStreak = getIntent().getIntExtra("habit_streak", 0);
        maxStreak = Math.max(currentStreak, sharedPreferences.getInt(habitName + "_max_streak", 0));
        completedCount = sharedPreferences.getInt(habitName + "_completed_count", 0);

        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1); // Go back one day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long yesterdayStart = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        long todayStart = calendar.getTimeInMillis();

        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);

        if (lastMarkedTime < yesterdayStart && currentStreak > 0) {
            currentStreak = 0;
            habitPoints = Math.max(0, habitPoints - 5);
            
            sharedPreferences.edit()
                .putInt(habitName + "_streak", currentStreak)
                .putInt(habitName + "_points", habitPoints)
                .apply();
                
            Toast.makeText(this, "Streak lost! -5 points", Toast.LENGTH_SHORT).show();
        }

        isMarked = lastMarkedTime >= todayStart;

        if (maxStreak > sharedPreferences.getInt(habitName + "_max_streak", 0)) {
            sharedPreferences.edit()
                .putInt(habitName + "_max_streak", maxStreak)
                .apply();
        }

        habitNameTextView = findViewById(R.id.habitNameTextView);
        habitPointsTextView = findViewById(R.id.habitPointsTextView);
        habitStreakTextView = findViewById(R.id.habitStreakTextView);
        habitMaxStreakTextView = findViewById(R.id.habitMaxStreakTextView);
        habitTreeImageView = findViewById(R.id.habitTreeImageView);
        currentStreakIconImageView = findViewById(R.id.currentStreakIconImageView);
        maxStreakIconImageView = findViewById(R.id.maxStreakIconImageView);
        markCompleteButton = findViewById(R.id.markCompleteButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        calendarButton = findViewById(R.id.calendarButton);
        statsButton = findViewById(R.id.statsButton);

        updateUI();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        markCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMarked && canMarkComplete()) {
                    sharedPreferences.edit()
                        .putInt(habitName + "_previous_points", habitPoints)
                        .putInt(habitName + "_previous_streak", currentStreak)
                        .putInt(habitName + "_previous_completed", completedCount)
                        .putInt(habitName + "_previous_max_streak", maxStreak)
                        .apply();

                    int basePoints = 10;
                    int bonusPoints = 0;

                    if (currentStreak >= 1) {
                        if (currentStreak >= 10) {
                            bonusPoints = basePoints;
                        } else if (currentStreak >= 9) {
                            bonusPoints = (int) (basePoints * 0.9);
                        } else if (currentStreak >= 8) {
                            bonusPoints = (int)(basePoints * 0.8);
                        } else if (currentStreak >= 7) {
                            bonusPoints = (int)(basePoints * 0.7);
                        } else if (currentStreak >= 6) {
                            bonusPoints = (int)(basePoints * 0.6);
                        } else if (currentStreak >= 5) {
                            bonusPoints = (int)(basePoints * 0.5);
                        } else if (currentStreak >= 4) {
                            bonusPoints = (int)(basePoints * 0.4);
                        } else if (currentStreak >= 3) {
                            bonusPoints = (int)(basePoints * 0.3);
                        } else if (currentStreak >= 2) {
                            bonusPoints = (int)(basePoints * 0.2);
                        } else if (currentStreak == 1) {
                            bonusPoints = (int)(basePoints * 0.1);
                        }
                    }

                    habitPoints += (basePoints + bonusPoints);
                    currentStreak++;
                    completedCount++;

                    int totalPointsAwarded = basePoints + bonusPoints;

                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak;
                    }

                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    sharedPreferences.edit()
                        .putLong(habitName + "_last_marked", currentTime)
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .putInt(habitName + "_streak", currentStreak)
                        .putInt(habitName + "_completed_count", completedCount)
                        .putInt(habitName + "_last_points_awarded", totalPointsAwarded)
                        .apply();

                    String message = "+" + basePoints + " points";
                    if (bonusPoints > 0) {
                        message += " (+" + bonusPoints + " streak bonus)";
                    }
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();

                    isMarked = true;
                    updateUI();
                } else if (isMarked) {
                    habitPoints = sharedPreferences.getInt(habitName + "_previous_points", habitPoints);
                    currentStreak = sharedPreferences.getInt(habitName + "_previous_streak", currentStreak);
                    completedCount = sharedPreferences.getInt(habitName + "_previous_completed", completedCount);
                    maxStreak = Math.max(currentStreak, sharedPreferences.getInt(habitName + "_previous_max_streak", maxStreak));
                    
                    sharedPreferences.edit()
                        .remove(habitName + "_last_marked")
                        .remove(habitName + "_last_points_awarded")
                        .remove(habitName + "_previous_points")
                        .remove(habitName + "_previous_streak")
                        .remove(habitName + "_previous_completed")
                        .remove(habitName + "_previous_max_streak")
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_streak", currentStreak)
                        .putInt(habitName + "_completed_count", completedCount)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .apply();

                    Toast.makeText(HabitDetailActivity.this, "Habit unmarked", Toast.LENGTH_SHORT).show();
                    
                    isMarked = false;
                    updateUI();
                }
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditDialog();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        calendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitCalendarActivity.class);
            intent.putExtra("habit_name", habitName);
            startActivity(intent);
        });

        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitStatsActivity.class);
            intent.putExtra("habitName", habitName);
            startActivity(intent);
        });
    }

    private boolean canMarkComplete() {
        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        
        return lastMarkedTime < startOfDay;
    }

    private void updateUI() {
        habitNameTextView.setText(habitName);
        habitPointsTextView.setText("Points: " + habitPoints);
        habitStreakTextView.setText("Current Streak: " + currentStreak);
        habitMaxStreakTextView.setText("Max Streak: " + maxStreak);

        markCompleteButton.setText(isMarked ? "Unmark Complete" : "Mark Complete");
        markCompleteButton.setEnabled(true);

        currentStreakIconImageView.setVisibility(currentStreak > 0 ? View.VISIBLE : View.GONE);
        maxStreakIconImageView.setVisibility(maxStreak > 0 ? View.VISIBLE : View.GONE);

        if (habitPoints >= 500) {
            habitTreeImageView.setImageResource(R.drawable.tree_final);
        } else if (habitPoints >= 200) {
            habitTreeImageView.setImageResource(R.drawable.tree_stage2);
        } else {
            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        }
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit");

        final EditText input = new EditText(this);
        input.setText(habitName);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(habitName)) {
                    updateHabitName(newName);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void updateHabitName(String newName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(newName + "_points", habitPoints);
        editor.putInt(newName + "_streak", currentStreak);
        editor.putInt(newName + "_max_streak", maxStreak);
        editor.putInt(newName + "_completed_count", completedCount);
        editor.putLong(newName + "_last_marked", sharedPreferences.getLong(habitName + "_last_marked", 0));

        editor.remove(habitName + "_points");
        editor.remove(habitName + "_streak");
        editor.remove(habitName + "_max_streak");
        editor.remove(habitName + "_completed_count");
        editor.remove(habitName + "_last_marked");

        Set<String> habits = new HashSet<>(sharedPreferences.getStringSet("habits", new HashSet<>()));
        habits.remove(habitName);
        habits.add(newName);
        editor.putStringSet("habits", habits);
        
        editor.apply();

        habitName = newName;
        habitNameTextView.setText(habitName);
        Toast.makeText(this, "Habit renamed", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Habit");
        builder.setMessage("Are you sure you want to delete this habit?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("habit_name", habitName);
                resultIntent.putExtra("action", "delete");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("habit_name", habitName);
        resultIntent.putExtra("habit_points", habitPoints);
        resultIntent.putExtra("action", "update");
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }
}