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

    private EditText editTextHabit;
    private Button buttonAddHabit;
    private RecyclerView recyclerViewHabits;
    private com.example.goaltracker.HabitAdapter habitAdapter;
    private ArrayList<String> habitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextHabit = findViewById(R.id.editTextHabit);
        buttonAddHabit = findViewById(R.id.buttonAddHabit);
        recyclerViewHabits = findViewById(R.id.recyclerViewHabits);

        habitList = new ArrayList<>();
        habitAdapter = new com.example.goaltracker.HabitAdapter(habitList);

        recyclerViewHabits.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHabits.setAdapter(habitAdapter);

        buttonAddHabit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newHabit = editTextHabit.getText().toString();
                if (!newHabit.isEmpty()) {
                    habitList.add(newHabit);
                    habitAdapter.notifyDataSetChanged();
                    editTextHabit.setText("");
                }
            }
        });
    }
}
