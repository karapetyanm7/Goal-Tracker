package com.example.goaltracker;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTextGoal;
    private Button buttonAddGoal;
    private RecyclerView recyclerViewGoals;
    private GoalAdapter goalAdapter;
    private ArrayList<Goal> goalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextGoal = findViewById(R.id.editTextGoal);
        buttonAddGoal = findViewById(R.id.buttonAddGoal);
        recyclerViewGoals = findViewById(R.id.recyclerViewGoals);

        goalList = new ArrayList<>();
        goalAdapter = new GoalAdapter(goalList);

        recyclerViewGoals.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGoals.setAdapter(goalAdapter);

        buttonAddGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newGoal = editTextGoal.getText().toString();
                if (!newGoal.isEmpty()) {
                    Goal goal = new Goal(newGoal);
                    goalList.add(goal);
                    goalAdapter.notifyDataSetChanged();
                    editTextGoal.setText("");
                }
            }
        });
    }
}
