package com.wonkyfingers.simon;

public class Game_Setup {
    public void setUpBorder(int borderWidth, int borderHeight, int[][] worldBorder) {
        for (int i = 0; i < borderWidth; i++) {
            for (int j = 0; j < borderHeight; j++) {
                if (i == 0 || i == borderWidth - 1 || j == 0 || j == borderHeight - 1) {
                    worldBorder[i][j] = 1;
                } else {
                    worldBorder[i][j] = 0;
                }
            }
        }
    }
}
