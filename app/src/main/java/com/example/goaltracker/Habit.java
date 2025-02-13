package com.example.goaltracker;

public class Habit {

    private String name;
    private int points;
    private int completionCount;
    private int maxStreak;
    private int currentStreak;

    public Habit(String name) {
        this.name = name;
        this.points = 100;
        this.completionCount = 0;
        this.maxStreak = 0;
        this.currentStreak = 0;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void incrementCompletionCount() {
        this.completionCount++;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public void setMaxStreak(int maxStreak) {
        this.maxStreak = maxStreak;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    @Override
    public String toString() {
        return "Habit: " + name;
    }
}
