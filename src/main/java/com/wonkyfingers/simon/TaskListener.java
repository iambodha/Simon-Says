package com.wonkyfingers.simon;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TaskListener implements Listener {
    private final TaskManager taskManager;

    public TaskListener(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Add any necessary player initialization here
    }
}