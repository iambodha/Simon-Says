package com.wonkyfingers.simon;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;

public final class Simon extends JavaPlugin {

   private final int BORDER_DIAMETER = 10;
   private final int PARTICLES_PER_CIRCLE = 100;
   private final double DAMAGE_AMOUNT = 2.0;
   private final double RADIUS = BORDER_DIAMETER / 2;

   @Override
   public void onEnable() {
       World world = getServer().getWorlds().get(0);
       Location center = world.getSpawnLocation();
       
       world.getWorldBorder().setSize(Integer.MAX_VALUE);
       
       new BukkitRunnable() {
           @Override
           public void run() {
               drawParticleBorder(center, RADIUS);
           }
       }.runTaskTimer(this, 0L, 2L);
       
       new BukkitRunnable() {
           @Override
           public void run() {
               for (Player player : world.getPlayers()) {
                   checkPlayerLocation(player, center);
               }
           }
       }.runTaskTimer(this, 0L, 10L);
       
       getLogger().info("Border with damage has been set!");
   }

   private void drawParticleBorder(Location center, double radius) {
       World world = center.getWorld();
       
       for(int i = 0; i < PARTICLES_PER_CIRCLE; i++) {
           double angle = 2 * Math.PI * i / PARTICLES_PER_CIRCLE;
           double x = center.getX() + (radius * Math.cos(angle));
           double z = center.getZ() + (radius * Math.sin(angle));
           
           Location particleLoc = new Location(world, x, 0, z);
           
           world.spawnParticle(
               Particle.DUST_COLOR_TRANSITION, 
               particleLoc, 
               world.getMaxHeight(),
               0, world.getMaxHeight(), 0,
               new Particle.DustTransition(Color.RED, Color.RED, 1.0f)
           );
       }
   }

   private void checkPlayerLocation(Player player, Location center) {
       Location flatPlayerLoc = new Location(player.getWorld(), 
           player.getLocation().getX(), 
           center.getY(),  
           player.getLocation().getZ());
           
       Location flatCenter = new Location(player.getWorld(), 
           center.getX(), 
           center.getY(), 
           center.getZ());
       
       double distance = flatPlayerLoc.distance(flatCenter);
       
       if (distance > RADIUS + 0.5) {
           player.damage(DAMAGE_AMOUNT);
       }
   }

   @Override
   public void onDisable() {
       getServer().getWorlds().get(0).getWorldBorder().setSize(60000000);
   }
}