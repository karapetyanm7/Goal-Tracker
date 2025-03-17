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
    private SharedPreferences sharedPreferences;
    private String habitName;
    private int habitPoints;
    private int currentStreak;
    private int maxStreak;
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

        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        isMarked = lastMarkedTime >= startOfDay;

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
                    int basePoints = 10;
                    int bonusPoints = 0;

                    if (currentStreak >= 1) {
                        if (currentStreak >= 10) {
                            bonusPoints = basePoints;
                        } else if (currentStreak >= 9) {
                            bonusPoints = (int)(basePoints * 0.8);
                        } else if (currentStreak >= 8) {
                            bonusPoints = (int)(basePoints * 0.7);
                        } else if (currentStreak >= 7) {
                            bonusPoints = (int)(basePoints * 0.6);
                        } else if (currentStreak >= 6) {
                            bonusPoints = (int)(basePoints * 0.5);
                        } else if (currentStreak >= 5) {
                            bonusPoints = (int)(basePoints * 0.4);
                        } else if (currentStreak >= 4) {
                            bonusPoints = (int)(basePoints * 0.3);
                        } else if (currentStreak >= 3) {
                            bonusPoints = (int)(basePoints * 0.2);
                        } else if (currentStreak >= 2) {
                            bonusPoints = (int)(basePoints * 0.1);
                        }
                    }

                    habitPoints += (basePoints + bonusPoints);
                    currentStreak++;

                    if (currentStreak > maxStreak) {
                        // Save the previous max streak before updating
                        sharedPreferences.edit()
                            .putInt(habitName + "_previous_max_streak", maxStreak)
                            .apply();
                        maxStreak = currentStreak;
                    }

                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    sharedPreferences.edit()
                        .putLong(habitName + "_last_marked", currentTime)
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .putInt(habitName + "_streak", currentStreak)
                        .apply();

                    String message = "+" + basePoints + " points";
                    if (bonusPoints > 0) {
                        message += " (+" + bonusPoints + " streak bonus)";
                    }
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                } else if (isMarked) {

                    habitPoints -= 10;
                    currentStreak = Math.max(0, currentStreak - 1);

                    int previousMaxStreak = sharedPreferences.getInt(habitName + "_previous_max_streak", 0);
                    maxStreak = previousMaxStreak;

                    if (currentStreak == 0) {
                        maxStreak = 0;
                    }
                    
                    sharedPreferences.edit()
                        .remove(habitName + "_last_marked")
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_streak", currentStreak)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .apply();

                    Toast.makeText(HabitDetailActivity.this, "Habit unmarked", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HabitDetailActivity.this, "You can only mark complete once per day", Toast.LENGTH_SHORT).show();
                }
                
                isMarked = !isMarked;
                updateUI();
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
                String newHabitName = input.getText().toString();
                if (!newHabitName.isEmpty()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("old_habit_name", habitName);
                    resultIntent.putExtra("new_habit_name", newHabitName);
                    resultIntent.putExtra("action", "edit");
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
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
 
        if (habitPoints >= 500) {
            habitTreeImageView.setImageResource(R.drawable.tree_final);
        } else if (habitPoints >= 200) {
            habitTreeImageView.setImageResource(R.drawable.tree_stage2);
        } else {
            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        }

        currentStreakIconImageView.setVisibility(currentStreak >= 1 ? View.VISIBLE : View.GONE);
        maxStreakIconImageView.setVisibility(maxStreak >= 1 ? View.VISIBLE : View.GONE);

        markCompleteButton.setText(isMarked ? "Unmark Complete" : "Mark Complete");
        markCompleteButton.setEnabled(true);
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