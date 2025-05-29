package com.example.goaltracker;

import java.util.HashSet;
import java.util.Set;

public class Habit {


    public static final String FREQUENCY_DAILY = "daily";
    public static final String FREQUENCY_CUSTOM = "custom";


    private String name;
    private int points;
    private int completionCount;
    private int maxStreak;
    private int currentStreak;
    

    private String frequency = FREQUENCY_DAILY;
    private Set<Integer> selectedDays = new HashSet<>();
    private int timesPerPeriod = 1;
    
    public Habit(String name) {
        this.name = name;
        this.points = 100;
        this.completionCount = 0; 
        this.maxStreak = 0;
        this.currentStreak = 0;
    }
    
    public Habit(String name, String frequency) {
        this(name);
        this.frequency = frequency;
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
    
    // Frequency getters and setters
    public String getFrequency() {
        return frequency;
    }
    
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    public Set<Integer> getSelectedDays() {
        return selectedDays;
    }
    
    public void setSelectedDays(Set<Integer> selectedDays) {
        this.selectedDays = selectedDays;
    }
    
    public void addSelectedDay(int day) {
        selectedDays.add(day);
    }
    
    public void removeSelectedDay(int day) {
        selectedDays.remove(day);
    }
    
    public int getTimesPerPeriod() {
        return timesPerPeriod;
    }
    
    public void setTimesPerPeriod(int timesPerPeriod) {
        this.timesPerPeriod = timesPerPeriod;
    }

    public boolean isScheduledForToday() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        

        int adjustedDay = dayOfWeek == java.util.Calendar.SUNDAY ? 7 : dayOfWeek - 1;
        
        switch (frequency) {
            case FREQUENCY_DAILY:
                return true;
            case FREQUENCY_CUSTOM:

                return selectedDays.contains(adjustedDay);
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return "Habit: " + name;
    }
}
