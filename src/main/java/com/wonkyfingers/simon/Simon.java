package com.wonkyfingers.simon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;

public final class Simon extends JavaPlugin {

    private final int BORDER_DIAMETER = 10;
    private final int PARTICLES_PER_CIRCLE = 50;

    @Override
    public void onEnable() {
        World world = getServer().getWorlds().get(0);
        Location center = world.getSpawnLocation();

        new BukkitRunnable() {
            @Override
            public void run() {
                drawParticleBorder(center, BORDER_DIAMETER / 2);
            }
        }.runTaskTimer(this, 0L, 5L);
        
        getLogger().info("Particle border has been set!");
    }

    private void drawParticleBorder(Location center, double radius) {
        World world = center.getWorld();
        
        for(int i = 0; i < PARTICLES_PER_CIRCLE; i++) {
            double angle = 2 * Math.PI * i / PARTICLES_PER_CIRCLE;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            
            Location particleLoc = new Location(world, x, center.getY(), z);
            
            for(double y = 0; y < 256; y += 2) {
                particleLoc.setY(y);
                world.spawnParticle(
                    Particle.DUST_COLOR_TRANSITION, 
                    particleLoc, 
                    1, 
                    0, 0, 0,
                    new Particle.DustTransition(Color.RED, Color.RED, 1.0f)
                );
            }
        }
    }

    @Override
    public void onDisable() {

    }
}