package com.wonkyfingers.simon;

import org.bukkit.entity.Player;
import java.util.function.Predicate;

public class SimonTask {
    private final String description;
    private final Predicate<Player> completionCheck;
    private final String hint;  // Optional hint or guidance for the task

    // Constructor to set both description and completion check
    public SimonTask(String description, Predicate<Player> completionCheck, String hint) {
        this.description = description;
        this.completionCheck = completionCheck;
        this.hint = hint;
    }

    // Constructor for tasks without a hint
    public SimonTask(String description, Predicate<Player> completionCheck) {
        this(description, completionCheck, "No hint provided.");
    }

    public String getDescription() {
        return description;
    }

    public String getHint() {
        return hint;
    }

    // Method to check if the player has completed the task
    public boolean isCompleted(Player player) {
        return completionCheck.test(player);
    }

    @Override
    public String toString() {
        return description + " - " + hint;
    }
}
