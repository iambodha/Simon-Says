package com.wonkyfingers.simon;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameCommands implements CommandExecutor {
    private final Game_Setup gameSetup;
    private final TaskManager taskManager;

    public GameCommands(Game_Setup gameSetup, TaskManager taskManager) {
        this.gameSetup = gameSetup;
        this.taskManager = taskManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be executed by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("startgame")) {
            gameSetup.startGame(player.getWorld(), player.getLocation());
            taskManager.startTasks();
            player.sendMessage("§aStarting game with Simon Says tasks!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("stopgame")) {
            gameSetup.stopGame();
            taskManager.cleanup();
            player.sendMessage("§cGame stopped!");
            return true;
        }

        return false;
    }
}