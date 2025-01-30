package com.wonkyfingers.simon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Game_Setup {
    private final JavaPlugin plugin;
    private GameConfig config;
    private BukkitTask borderTask;
    private BukkitTask damageTask;
    private BukkitTask timerTask;
    private Location centerLocation;
    private World gameWorld;
    private boolean isGameRunning = false;
    private int timeRemaining; // in seconds
    private double currentBorderRadius;
    private boolean showParticles = true;
    private int currentPhase = 0;
    private boolean isCurrentlyShrinking = false;
    private double targetBorderRadius;
    private int shrinkTimeRemaining = 0; // seconds remaining in current shrink
    private double shrinkSpeedPerSecond; // New variable for constant shrink speed

    // Phase timing configuration (in seconds)
    // 20 minute game times
    private static final int[] PHASE_WAIT_TIMES = {240, 180, 180, 120, 90, 60}; // Time before shrink starts
    private static final int[] PHASE_SHRINK_TIMES = {90, 60, 45, 30, 20, 15}; // Time taken to shrink
    private static final double[] PHASE_SIZES = {1.0, 0.7, 0.4, 0.2, 0.1, 0.05, 0}; // Size multiplier for each phase

    public static class GameConfig {
        private final int borderDiameter;
        private final int particlesPerCircle;
        private final double damageAmount;
        private final int wallSections;
        private final long borderUpdateTicks;
        private final long damageCheckTicks;
        private final Color borderColor;

        public GameConfig(Builder builder) {
            this.borderDiameter = builder.borderDiameter;
            this.particlesPerCircle = builder.particlesPerCircle;
            this.damageAmount = builder.damageAmount;
            this.wallSections = builder.wallSections;
            this.borderUpdateTicks = builder.borderUpdateTicks;
            this.damageCheckTicks = builder.damageCheckTicks;
            this.borderColor = builder.borderColor;
        }

        public static class Builder {
            private int borderDiameter = 300;
            private int particlesPerCircle = 100;
            private double damageAmount = 2.0;
            private int wallSections = 16;
            private long borderUpdateTicks = 2L;
            private long damageCheckTicks = 10L;
            private Color borderColor = Color.RED;

            public Builder borderDiameter(int diameter) {
                this.borderDiameter = diameter;
                return this;
            }

            public Builder particlesPerCircle(int particles) {
                this.particlesPerCircle = particles;
                return this;
            }

            public Builder damageAmount(double damage) {
                this.damageAmount = damage;
                return this;
            }

            public Builder wallSections(int sections) {
                this.wallSections = sections;
                return this;
            }

            public Builder borderUpdateTicks(long ticks) {
                this.borderUpdateTicks = ticks;
                return this;
            }

            public Builder damageCheckTicks(long ticks) {
                this.damageCheckTicks = ticks;
                return this;
            }

            public Builder borderColor(Color color) {
                this.borderColor = color;
                return this;
            }

            public GameConfig build() {
                return new GameConfig(this);
            }
        }
    }

    public Game_Setup(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startGame(World world, Location center) {
        if (isGameRunning) {
            cleanup();
        }

        this.config = new GameConfig.Builder()
                .borderDiameter(300)
                .particlesPerCircle(100)
                .damageAmount(2.0)
                .wallSections(16)
                .borderColor(Color.RED)
                .build();

        isGameRunning = true;
        showParticles = true;
        currentPhase = 0;
        isCurrentlyShrinking = false;
        initialize(world, center);

        startNextPhase();

        broadcastMessage("§a§lGame started! Get ready for zone movement!");
    }

    private void startNextPhase() {
        if (currentPhase >= PHASE_WAIT_TIMES.length) {
            endGame();
            return;
        }

        // Calculate next border size
        targetBorderRadius = (config.borderDiameter / 2.0) * PHASE_SIZES[currentPhase + 1];

        // Start wait period
        isCurrentlyShrinking = false;
        timeRemaining = PHASE_WAIT_TIMES[currentPhase];

        // Calculate constant shrink speed for this phase
        double totalDistanceToShrink = currentBorderRadius - targetBorderRadius;
        shrinkSpeedPerSecond = totalDistanceToShrink / PHASE_SHRINK_TIMES[currentPhase];

        // Announce next zone
        String message = String.format("§e§lZone will start shrinking in %d seconds! Next safe zone size: %.1f blocks",
                timeRemaining, targetBorderRadius * 2);
        broadcastMessage(message);
    }

    private void startShrinking() {
        isCurrentlyShrinking = true;
        shrinkTimeRemaining = PHASE_SHRINK_TIMES[currentPhase];

        String message = "§c§lZone is now shrinking!";
        broadcastMessage(message);
    }

    private void updateBorderSize() {
        if (!isCurrentlyShrinking) return;

        // Move at constant speed (adjusted for tick rate)
        double moveAmount = shrinkSpeedPerSecond / 20; // Convert per-second speed to per-tick speed
        currentBorderRadius = Math.max(targetBorderRadius, currentBorderRadius - moveAmount);
    }

    private void initialize(World world, Location center) {
        this.gameWorld = world;
        this.centerLocation = center;
        this.currentBorderRadius = config.borderDiameter / 2.0;

        resetWorldBorder();
        startBorderVisualization();
        startDamageCheck();
        startGameTimer();
    }

    private void startBorderVisualization() {
        if (borderTask != null) {
            borderTask.cancel();
        }

        borderTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isGameRunning || !showParticles) {
                    this.cancel();
                    return;
                }
                drawParticleBorder();
            }
        }.runTaskTimer(plugin, 0L, config.borderUpdateTicks);
    }

    private void startDamageCheck() {
        if (damageTask != null) {
            damageTask.cancel();
        }

        damageTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isGameRunning) {
                    this.cancel();
                    return;
                }
                checkAllPlayersLocation();
            }
        }.runTaskTimer(plugin, 0L, config.damageCheckTicks);
    }

    private void startGameTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isGameRunning) {
                    this.cancel();
                    return;
                }

                timeRemaining--;

                if (isCurrentlyShrinking) {
                    shrinkTimeRemaining--;
                    updateBorderSize();

                    if (shrinkTimeRemaining <= 0) {
                        currentPhase++;
                        startNextPhase();
                    }
                } else {
                    if (timeRemaining <= 0) {
                        startShrinking();
                    } else if (timeRemaining <= 30 && timeRemaining % 10 == 0) {
                        broadcastMessage("§e§lZone shrinks in " + timeRemaining + " seconds!");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }

    private void drawParticleBorder() {
        if (!showParticles) return;

        int maxHeight = gameWorld.getMaxHeight();

        // Calculate the circumference of the current circle
        double circumference = 2 * Math.PI * currentBorderRadius;

        // Adjust number of particles based on circumference
        // Using a ratio of 1 particle per block of circumference
        int adjustedParticleCount = (int) Math.max(50, Math.ceil(circumference));

        // Calculate height sections based on circumference
        int adjustedWallSections = (int) Math.max(8, Math.ceil(circumference / 20));
        int heightPerSection = maxHeight / adjustedWallSections;

        for (int i = 0; i < adjustedParticleCount; i++) {
            double angle = 2 * Math.PI * i / adjustedParticleCount;
            double x = centerLocation.getX() + (currentBorderRadius * Math.cos(angle));
            double z = centerLocation.getZ() + (currentBorderRadius * Math.sin(angle));

            for (int h = 0; h < adjustedWallSections; h++) {
                Location particleLoc = new Location(gameWorld, x, h * heightPerSection, z);
                spawnBorderParticle(particleLoc, heightPerSection);
            }
        }
    }

    private void spawnBorderParticle(Location location, int height) {
        gameWorld.spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                location,
                height,
                0, height, 0,
                new Particle.DustTransition(config.borderColor, config.borderColor, 1.0f)
        );
    }

    private void checkAllPlayersLocation() {
        for (Player player : gameWorld.getPlayers()) {
            checkPlayerLocation(player);
        }
    }

    private void checkPlayerLocation(Player player) {
        Location playerLoc = player.getLocation();
        Location flatPlayerLoc = getFlatLocation(playerLoc);
        Location flatCenter = getFlatLocation(centerLocation);
        double distance = flatPlayerLoc.distance(flatCenter);

        // Skip damage if player is exactly at the center block
        if (Math.floor(playerLoc.getX()) == Math.floor(centerLocation.getX()) &&
                Math.floor(playerLoc.getZ()) == Math.floor(centerLocation.getZ())) {
            return;
        }

        if (distance > currentBorderRadius + 0.5) {
            player.damage(config.damageAmount);
        }
    }

    private Location getFlatLocation(Location location) {
        return new Location(
                location.getWorld(),
                location.getX(),
                centerLocation.getY(),
                location.getZ()
        );
    }

    private void broadcastMessage(String message) {
        for (Player player : gameWorld.getPlayers()) {
            player.sendMessage(message);
        }
    }

    private void resetWorldBorder() {
        gameWorld.getWorldBorder().setSize(Integer.MAX_VALUE);
    }

    private void endGame() {
        String message = "§4§lFinal zone reached! Damage will continue until the game is stopped.";
        broadcastMessage(message);
        showParticles = false;
        timerTask.cancel();
    }

    public void stopGame() {
        if (isGameRunning) {
            broadcastMessage("§c§lGame stopped by administrator!");
            cleanup();
            isGameRunning = false;
        }
    }

    public void cleanup() {
        if (borderTask != null) {
            borderTask.cancel();
        }
        if (damageTask != null) {
            damageTask.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (gameWorld != null) {
            gameWorld.getWorldBorder().setSize(60000000);
        }
        showParticles = false;
        isGameRunning = false;
    }
}