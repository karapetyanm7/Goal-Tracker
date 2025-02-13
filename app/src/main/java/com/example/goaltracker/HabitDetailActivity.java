package com.example.goaltracker;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HabitDetailActivity extends AppCompatActivity {

    private ImageView habitTreeImageView;
    private TextView habitNameTextView;
    private TextView habitPointsTextView;
    private TextView habitCompletionTextView;
    private TextView habitMaxStreakTextView;
    private TextView habitCurrentStreakTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_detail);

        habitTreeImageView = findViewById(R.id.habitTreeImageView);
        habitNameTextView = findViewById(R.id.habitNameTextView);
        habitPointsTextView = findViewById(R.id.habitPointsTextView);
        habitCompletionTextView = findViewById(R.id.habitCompletionTextView);
        habitMaxStreakTextView = findViewById(R.id.habitMaxStreakTextView);
        habitCurrentStreakTextView = findViewById(R.id.habitCurrentStreakTextView);

        String habitName = getIntent().getStringExtra("habitName");
        int habitPoints = getIntent().getIntExtra("habitPoints", 0);
        int habitCompletion = getIntent().getIntExtra("habitCompletion", 0);
        int habitMaxStreak = getIntent().getIntExtra("habitMaxStreak", 0);
        int habitCurrentStreak = getIntent().getIntExtra("habitCurrentStreak", 0);

        habitNameTextView.setText(habitName);
        habitPointsTextView.setText("Points: " + habitPoints);
        habitCompletionTextView.setText("Completion: " + habitCompletion);
        habitMaxStreakTextView.setText("Max Streak: " + habitMaxStreak);
        habitCurrentStreakTextView.setText("Current Streak: " + habitCurrentStreak);

        habitTreeImageView.setImageResource(R.drawable.tree_normal);
    }
}
