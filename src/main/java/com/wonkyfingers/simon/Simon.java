package com.wonkyfingers.simon;

import org.bukkit.plugin.java.JavaPlugin;

public final class Simon extends JavaPlugin {
    private Game_Setup gameSetup;
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        // Initialize task manager
        taskManager = new TaskManager(this);

        // Initialize game setup
        gameSetup = new Game_Setup(this);

        // Register commands with task manager
        GameCommands gameCommands = new GameCommands(gameSetup, taskManager);
        getCommand("startgame").setExecutor(gameCommands);
        getCommand("stopgame").setExecutor(gameCommands);

        // Register task listener
        getServer().getPluginManager().registerEvents(new TaskListener(taskManager), this);

        getLogger().info("Simon plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (gameSetup != null) {
            gameSetup.cleanup();
        }
        if (taskManager != null) {
            taskManager.cleanup();
        }
    }
}