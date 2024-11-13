package com.wonkyfingers.simon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class TaskManager {
    private final Simon plugin;
    private final List<SimonTask> availableTasks;
    private SimonTask currentTask;
    private SimonTask contradictoryTask;
    private final Map<UUID, TaskProgress> playerProgress;
    private BukkitRunnable taskTimer;
    private BossBar taskBar;
    private boolean isSimonSaysTask;
    private boolean hasContradictoryCommand;

    private static final long[] TASK_TIMINGS = {60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 780, 840, 900, 960, 1020}; // Task timing intervals
    private static final long TASK_DURATION = 15; // Reduced to 15 seconds for faster gameplay

    private static class TaskProgress {
        boolean completed;
        Set<String> completedSubtasks;

        TaskProgress() {
            this.completed = false;
            this.completedSubtasks = new HashSet<>();
        }
    }

    // Enhanced command prefixes for more variety
    private static final String[] COMMAND_PREFIXES = {
            "Simon says",
            "Simon demands",
            "Simon requests",
            "The mighty Simon commands",
            "Simon whispers",
            "Simon suggests",
            "Please",
            "You must",
            "Everyone should",
            "Quickly",
            "The game master says",
            "Listen carefully and"
    };

    // Dynamic task adjectives for more engaging descriptions
    private static final String[] TASK_ADJECTIVES = {
            "quickly",
            "carefully",
            "sneakily",
            "gracefully",
            "immediately",
            "precisely",
            "cleverly",
            "cautiously",
            "swiftly",
            "silently"
    };

    public TaskManager(Simon plugin) {
        this.plugin = plugin;
        this.availableTasks = new ArrayList<>();
        this.playerProgress = new HashMap<>();
        initializeTasks();
    }

    private void initializeTasks() {
        // Basic Movement Tasks
        availableTasks.add(new SimonTask("Jump and Sneak",
                player -> !player.isOnGround() && player.isSneaking(),
                "Perform both actions simultaneously"));

        availableTasks.add(new SimonTask("Look Up While Running",
                player -> player.getLocation().getPitch() < -80 && player.isSprinting(),
                "Sprint while looking at the sky"));

        // Item Interaction Tasks
        availableTasks.add(new SimonTask("Switch Hands Three Times",
                player -> {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();
                    player.getInventory().setItemInOffHand(mainHand);
                    player.getInventory().setItemInMainHand(offHand);
                    return true; // This is simplified, you'd need to track the switches
                },
                "Swap items between main and off hand"));

        availableTasks.add(new SimonTask("Drop and Catch an Item",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld().getEntitiesByClass(org.bukkit.entity.Item.class)
                            .stream()
                            .anyMatch(item -> item.getLocation().distance(loc) < 2);
                },
                "Drop an item and pick it up before it hits the ground"));

        // Environment Interaction Tasks
        availableTasks.add(new SimonTask("Stand Between Two Blocks",
                player -> {
                    Location loc = player.getLocation();
                    Location blockBehind = loc.clone().add(0, 0, 1);
                    Location blockInFront = loc.clone().add(0, 0, -1);
                    return !blockBehind.getBlock().isEmpty() && !blockInFront.getBlock().isEmpty();
                },
                "Position yourself with blocks on both sides"));

        // Complex Movement Tasks
        availableTasks.add(new SimonTask("Sprint Jump While Looking Down",
                player -> player.isSprinting() && !player.isOnGround() && player.getLocation().getPitch() > 80,
                "Combination of sprinting, jumping, and looking down"));

        // Multi-step Tasks
        availableTasks.add(new SimonTask("Perform The Dance",
                player -> {
                    TaskProgress progress = playerProgress.get(player.getUniqueId());
                    if (progress.completedSubtasks.size() == 3) return true;

                    if (!player.isOnGround()) {
                        progress.completedSubtasks.add("jump");
                    }
                    if (player.isSneaking()) {
                        progress.completedSubtasks.add("sneak");
                    }
                    if (player.isSprinting()) {
                        progress.completedSubtasks.add("sprint");
                    }

                    return progress.completedSubtasks.size() == 3;
                },
                "Jump, then sneak, then sprint"));

        // Inventory Tasks
        availableTasks.add(new SimonTask("Organize Your Hotbar",
                player -> {
                    // Check if items are sorted by material name
                    boolean sorted = true;
                    String prevName = "";
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null) {
                            String currentName = item.getType().name();
                            if (prevName.compareTo(currentName) > 0) {
                                sorted = false;
                                break;
                            }
                            prevName = currentName;
                        }
                    }
                    return sorted;
                },
                "Sort your hotbar items alphabetically"));
    }


    private String getRandomPrefix() {
        return COMMAND_PREFIXES[new Random().nextInt(COMMAND_PREFIXES.length)];
    }

    private String getRandomAdjective() {
        return TASK_ADJECTIVES[new Random().nextInt(TASK_ADJECTIVES.length)];
    }

    private void broadcastTaskMessage(String prefix, String task, boolean isSimonSays) {
        String adjective = getRandomAdjective();
        ChatColor prefixColor = isSimonSays ? ChatColor.GREEN : ChatColor.BLUE;
        ChatColor messageColor = isSimonSays ? ChatColor.YELLOW : ChatColor.GRAY;

        String message = String.format("%s%s %s%s %s!",
                prefixColor, prefix,
                messageColor, adjective,
                task.toLowerCase());

        // Send message to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Title display
            player.sendTitle(
                    prefixColor + prefix + "!",
                    messageColor + task,
                    10, 40, 10
            );

            // Chat message
            player.sendMessage(ChatColor.GOLD + "➤ " + message);

            // Sound effect
            if (isSimonSays) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                player.spawnParticle(Particle.NOTE, player.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                player.spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    private void createBossBar(String taskDescription, boolean isSimonSays) {
        if (taskBar != null) {
            taskBar.removeAll();
        }

        String prefix = getRandomPrefix();
        BarColor barColor = isSimonSays ? BarColor.GREEN : BarColor.BLUE;

        taskBar = Bukkit.createBossBar(
                ChatColor.GOLD + prefix + ": " + taskDescription,
                barColor,
                BarStyle.SEGMENTED_20
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            taskBar.addPlayer(player);
        }
        taskBar.setVisible(true);

        // Broadcast the task in chat and with visual effects
        broadcastTaskMessage(prefix, taskDescription, isSimonSays);
    }

    public void startTasks() {
        Collections.shuffle(availableTasks);
        scheduleAllTasks();
    }

    private void scheduleAllTasks() {
        for (int i = 0; i < TASK_TIMINGS.length; i++) {
            final int taskIndex = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (taskIndex < availableTasks.size()) {
                        startNewTask(availableTasks.get(taskIndex));
                    }
                }
            }.runTaskLater(plugin, TASK_TIMINGS[taskIndex] * 20L);
        }
    }

    private void startNewTask(SimonTask task) {
        currentTask = task;
        playerProgress.clear();

        // 60% chance of being a real Simon Says command
        isSimonSaysTask = new Random().nextDouble() < 0.6;

        // 30% chance of adding a contradictory command
        hasContradictoryCommand = new Random().nextDouble() < 0.3;

        if (hasContradictoryCommand) {
            // Select a different task as the contradictory one
            List<SimonTask> otherTasks = new ArrayList<>(availableTasks);
            otherTasks.remove(task);
            contradictoryTask = otherTasks.get(new Random().nextInt(otherTasks.size()));
        }

        createBossBar(task.getDescription(), isSimonSaysTask);

        // Initialize progress tracking for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerProgress.put(player.getUniqueId(), new TaskProgress());
        }

        startTaskTimer();
    }

    private void startTaskTimer() {
        if (taskTimer != null) {
            taskTimer.cancel();
        }

        taskTimer = new BukkitRunnable() {
            private int timeLeft = (int) TASK_DURATION;
            private boolean contradictoryCommandIssued = false;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    endCurrentTask();
                    cancel();
                    return;
                }

                // Issue contradictory command halfway through
                if (hasContradictoryCommand && !contradictoryCommandIssued && timeLeft == TASK_DURATION/2) {
                    contradictoryCommandIssued = true;
                    String prefix = isSimonSaysTask ? "Simon says" : getRandomPrefix();
                    broadcastTaskMessage(prefix, contradictoryTask.getDescription(), !isSimonSaysTask);
                }

                double progress = (double) timeLeft / TASK_DURATION;
                taskBar.setProgress(progress);

                if (timeLeft <= 5) {
                    taskBar.setColor(BarColor.RED);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    }
                }

                checkTaskCompletion();
                timeLeft--;
            }
        };

        taskTimer.runTaskTimer(plugin, 0L, 20L);
    }

    private void checkTaskCompletion() {
        if (currentTask == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            TaskProgress progress = playerProgress.get(playerId);

            if (!progress.completed) {
                boolean hasCompletedTask = currentTask.isCompleted(player);

                if ((hasCompletedTask && !isSimonSaysTask) || (!hasCompletedTask && isSimonSaysTask)) {
                    failPlayer(player);
                    progress.completed = true;
                } else if (hasCompletedTask && isSimonSaysTask) {
                    progress.completed = true;
                    playSuccessEffect(player);
                }
            }
        }
    }

    private void playSuccessEffect(Player player) {
        // Enhanced success effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 2, 0), 50, 0.5, 0.5, 0.5, 0.5);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);

        player.sendTitle(
                ChatColor.GREEN + "SUCCESS!",
                ChatColor.YELLOW + "You followed Simon perfectly!",
                10, 40, 10
        );

        // Give small rewards for successful completion
        if (new Random().nextDouble() < 0.3) { // 30% chance
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 0)); // 1-minute speed boost
        }
    }

    private void playFailEffect(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        player.spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.5);

        String failMessage = isSimonSaysTask ?
                "Simon said to do it!" :
                "Simon didn't say!";

        player.sendTitle(
                ChatColor.RED + "FAILED!",
                ChatColor.GRAY + failMessage,
                10, 40, 10
        );

        // Send failure message in chat
        player.sendMessage(ChatColor.RED + "✗ " + ChatColor.GRAY + "You failed because: " + failMessage);
    }

    private void failPlayer(Player player) {
        // Enhanced punishment system
        int duration = 600; // 5 minutes
        PotionEffectType[] effects = {
                PotionEffectType.WEAKNESS,
                PotionEffectType.SLOWNESS,
                PotionEffectType.NAUSEA,
                PotionEffectType.BLINDNESS,
                PotionEffectType.HUNGER
        };

        // Apply 1-3 random effects
        int numEffects = new Random().nextInt(3) + 1;
        Set<PotionEffectType> chosenEffects = new HashSet<>();
        while (chosenEffects.size() < numEffects) {
            chosenEffects.add(effects[new Random().nextInt(effects.length)]);
        }

        for (PotionEffectType effect : chosenEffects) {
            PotionEffect current = player.getPotionEffect(effect);
            int newAmplifier = (current != null) ? Math.min(current.getAmplifier() + 1, 3) : 0;
            player.addPotionEffect(new PotionEffect(effect, duration, newAmplifier));
        }

        playFailEffect(player);
    }

    private void endCurrentTask() {
        if (currentTask == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            TaskProgress progress = playerProgress.get(playerId);

            // Check if the player completed the task correctly
            boolean hasCompletedTask = currentTask.isCompleted(player);

            // If the task was a Simon Says task and the player completed it correctly
            if (hasCompletedTask && isSimonSaysTask) {
                progress.completed = true;
                playSuccessEffect(player); // Play success effects and provide rewards
            }
            // If the task was not a Simon Says task and the player completed it
            else if (!hasCompletedTask && !isSimonSaysTask) {
                progress.completed = true;
                playSuccessEffect(player); // Success, as the player should not complete the task
            } else {
                // The player failed, play fail effects and handle punishment
                failPlayer(player);
            }
        }

        // Announce task end
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "➤ " + ChatColor.GRAY + "Time's up! Next task coming soon...");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
        }

        playerProgress.clear();

        if (taskBar != null) {
            taskBar.removeAll();
            taskBar = null;
        }
        currentTask = null;
        contradictoryTask = null;
        hasContradictoryCommand = false;
    }

    public void cleanup() {
        if (taskTimer != null) {
            taskTimer.cancel();
        }
        if (taskBar != null) {
            taskBar.removeAll();
            taskBar = null;
        }
        currentTask = null;
        playerProgress.clear();
    }
}