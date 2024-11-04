package com.wonkyfingers.simon;

import org.bukkit.plugin.java.JavaPlugin;

public final class Simon extends JavaPlugin {

    private final int BORDER_WIDTH = 10;
    private final int BORDER_HEIGHT = 10;
    private int[][] worldBorder = new int[BORDER_WIDTH][BORDER_HEIGHT];

    @Override
    public void onEnable() {
        Game_Setup gameSetup = new Game_Setup();
        
        gameSetup.setUpBorder(BORDER_WIDTH, BORDER_HEIGHT, worldBorder);

        printBorder();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Print the border for testing purposes
    private void printBorder() {
        for (int i = 0; i < BORDER_WIDTH; i++) {
            for (int j = 0; j < BORDER_HEIGHT; j++) {
                System.out.print(worldBorder[i][j] + " ");
            }
            System.out.println();
        }
    }
}
