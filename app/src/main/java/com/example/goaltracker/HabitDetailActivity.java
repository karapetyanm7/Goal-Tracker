package com.example.goaltracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HabitDetailActivity extends AppCompatActivity {

    private TextView habitNameTextView;
    private TextView habitPointsTextView;
    private TextView habitStreakTextView;
    private ImageView habitTreeImageView;
    private ImageView streakIconImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_detail);

        String habitName = getIntent().getStringExtra("habit_name");
        int habitPoints = getIntent().getIntExtra("habit_points", 100);
        int currentStreak = getIntent().getIntExtra("habit_streak", 0);

        habitNameTextView = findViewById(R.id.habitNameTextView);
        habitPointsTextView = findViewById(R.id.habitPointsTextView);
        habitStreakTextView = findViewById(R.id.habitStreakTextView);
        habitTreeImageView = findViewById(R.id.habitTreeImageView);
        streakIconImageView = findViewById(R.id.streakIconImageView);

        habitNameTextView.setText(habitName);
        habitPointsTextView.setText("Points: " + habitPoints);
        habitStreakTextView.setText("Streak: " + currentStreak);

 
        if (habitPoints >= 500) {
            habitTreeImageView.setImageResource(R.drawable.tree_final);
        } else if (habitPoints >= 200) {
            habitTreeImageView.setImageResource(R.drawable.tree_stage2);
        } else {
            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        }

        if (currentStreak >= 1) {
            streakIconImageView.setImageResource(R.drawable.streak_fire);
        } else {
            streakIconImageView.setVisibility(ImageView.INVISIBLE);
        }
    }
}
