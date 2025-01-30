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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Strider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;

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

        // Combat & PVE Tasks
        availableTasks.add(new SimonTask("Perfect Block",
                player -> player.isBlocking() && player.getNearbyEntities(3, 3, 3).stream()
                        .anyMatch(e -> e instanceof Monster),
                "Block an attack from a monster with perfect timing"));

        availableTasks.add(new SimonTask("Arrow Catch",
                player -> {
                    return player.getStatistic(Statistic.PICKUP, Material.ARROW) > 0;
                },
                "Catch an arrow mid-flight"));
        // Parkour Tasks
        availableTasks.add(new SimonTask("Wall Run",
                player -> {
                    Location loc = player.getLocation();
                    return !player.isOnGround() && loc.getBlock().getRelative(BlockFace.NORTH).getType().isSolid();
                },
                "Run along a wall without touching the ground"));

        availableTasks.add(new SimonTask("Precise Landing",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getBlock().getType() == Material.GOLD_BLOCK &&
                            !player.getLocation().add(0, -1, 0).getBlock().getType().isSolid();
                },
                "Land exactly on a gold block"));

        // Environmental Interaction
        availableTasks.add(new SimonTask("Water Walk",
                player -> {
                    Location loc = player.getLocation();
                    return loc.add(0, -1, 0).getBlock().getType() == Material.WATER &&
                            player.getVelocity().getY() >= 0;
                },
                "Walk on water using lily pads or frost walker"));

        availableTasks.add(new SimonTask("Torch Placer",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld().getBlockAt(loc).getLightLevel() > 10;
                },
                "Place torches to achieve maximum brightness"));

        // Building Tasks
        availableTasks.add(new SimonTask("Quick Builder",
                player -> {
                    Location loc = player.getLocation();
                    int blockCount = 0;
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (loc.clone().add(x, -1, z).getBlock().getType().isSolid()) {
                                blockCount++;
                            }
                        }
                    }
                    return blockCount >= 9;
                },
                "Build a 3x3 platform in under 5 seconds"));

        // Inventory Management
        availableTasks.add(new SimonTask("Color Coordinator",
                player -> {
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    if (armor[0] == null || armor[1] == null || armor[2] == null || armor[3] == null) return false;
                    Material color = armor[0].getType();
                    for (ItemStack item : armor) {
                        if (item.getType().name().contains(color.name())) return false;
                    }
                    return true;
                },
                "Wear a complete set of matching colored armor"));

        // Advanced Movement
        availableTasks.add(new SimonTask("360 No Scope",
                player -> {
                    float yaw = player.getLocation().getYaw();
                    return !player.isOnGround() && Math.abs(yaw - player.getLocation().getYaw()) >= 360;
                },
                "Perform a 360-degree spin while in the air"));

        availableTasks.add(new SimonTask("Dolphin Dive",
                player -> {
                    Location loc = player.getLocation();
                    return player.isSwimming() &&
                            loc.getPitch() < -30 &&
                            loc.getBlock().getType() == Material.WATER;
                },
                "Dive into water like a dolphin"));

        // Tool Usage
        availableTasks.add(new SimonTask("Tool Master",
                player -> {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    return item != null &&
                            item.getType().name().endsWith("_PICKAXE") &&
                            item.getEnchantments().size() >= 3;
                },
                "Use a pickaxe with at least 3 enchantments"));

        // Weather Related
        availableTasks.add(new SimonTask("Lightning Rod",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld().hasStorm() &&
                            loc.getBlock().getLightFromSky() == 15 &&
                            player.getInventory().getHelmet() != null &&
                            player.getInventory().getHelmet().getType() == Material.LIGHTNING_ROD;
                },
                "Stand in the rain with a lightning rod on your head"));

        // Crafting Tasks
        availableTasks.add(new SimonTask("Speed Crafter",
                player -> {
                    // This would need additional implementation to track crafting speed
                    return player.getStatistic(Statistic.CRAFT_ITEM) > 0;
                },
                "Craft 3 different items within 10 seconds"));

        // Pet Interaction
        availableTasks.add(new SimonTask("Pet Parade",
                player -> {
                    int tamedAnimals = (int) player.getNearbyEntities(5, 5, 5).stream()
                            .filter(e -> e instanceof Tameable && ((Tameable) e).isTamed() &&
                                    ((Tameable) e).getOwner().equals(player))
                            .count();
                    return tamedAnimals >= 3;
                },
                "Have 3 tamed animals following you simultaneously"));

        // Redstone Engineering
        availableTasks.add(new SimonTask("Circuit Builder",
                player -> {
                    Location loc = player.getLocation();
                    int redstoneCount = 0;
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            if (loc.clone().add(x, 0, z).getBlock().getType() == Material.REDSTONE_WIRE) {
                                redstoneCount++;
                            }
                        }
                    }
                    return redstoneCount >= 5;
                },
                "Create a working redstone circuit"));

        // Social Tasks
        availableTasks.add(new SimonTask("Trade Master",
                player -> {
                    return player.getNearbyEntities(5, 5, 5).stream()
                            .anyMatch(e -> e instanceof Villager &&
                                    ((Villager) e).getProfession() != Villager.Profession.NONE);
                },
                "Successfully trade with a villager"));

        // Farming Tasks
        availableTasks.add(new SimonTask("Crop Harvester",
        player -> {
            Location loc = player.getLocation();
            int cropCount = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = loc.clone().add(x, 0, z).getBlock();
                    if (block.getType() == Material.WHEAT && 
                        block.getBlockData() instanceof Ageable) {
                        Ageable crop = (Ageable) block.getBlockData();
                        if (crop.getAge() == crop.getMaximumAge()) {
                            cropCount++;
                        }
                    }
                }
            }
            return cropCount >= 3;
        },
        "Harvest 3 fully grown wheat crops"));

        // Mining Tasks
        availableTasks.add(new SimonTask("Deep Diver",
                player -> player.getLocation().getY() < 0 &&
                        player.getLocation().getBlock().getLightLevel() == 0,
                "Reach bedrock level in complete darkness"));

        // Survival Tasks
        availableTasks.add(new SimonTask("Fire Walker",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getBlock().getType() == Material.MAGMA_BLOCK &&
                            !player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
                },
                "Walk on magma blocks without fire resistance"));

        // Enchanting Tasks
        availableTasks.add(new SimonTask("Enchantment Master",
                player -> {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    return item != null && item.getEnchantments().size() >= 5;
                },
                "Hold an item with 5 or more enchantments"));

        // Brewing Tasks
        availableTasks.add(new SimonTask("Potion Mixer",
                player -> {
                    return player.getActivePotionEffects().size() >= 3;
                },
                "Have 3 potion effects active simultaneously"));

        // Transportation Tasks
        availableTasks.add(new SimonTask("Elytra Expert",
                player -> {
                    return player.isGliding() &&
                            player.getVelocity().length() > 1.5 &&
                            player.getLocation().getPitch() < -45;
                },
                "Perform a steep dive while gliding with elytra"));

        // Combat Tricks
        availableTasks.add(new SimonTask("Shield Master",
                player -> {
                    return player.isBlocking() &&
                            player.getLocation().getPitch() < -60 &&
                            player.isSneaking();
                },
                "Block while looking up and sneaking"));

        // Resource Collection
        availableTasks.add(new SimonTask("Resource Gatherer",
                player -> {
                    Inventory inv = player.getInventory();
                    return inv.contains(Material.OAK_LOG, 16) &&
                            inv.contains(Material.COBBLESTONE, 16) &&
                            inv.contains(Material.IRON_ORE, 4);
                },
                "Collect 16 logs, 16 cobblestone, and 4 iron ore"));

        // Navigation Tasks
        availableTasks.add(new SimonTask("Explorer",
                player -> {
                    Location loc = player.getLocation();
                    return Math.abs(loc.getX()) > 1000 || Math.abs(loc.getZ()) > 1000;
                },
                "Travel 1000 blocks from spawn in any direction"));

        // Food Tasks
        availableTasks.add(new SimonTask("Gourmet Chef",
                player -> {
                    return player.getFoodLevel() == 20 &&
                            player.getSaturation() > 15;
                },
                "Achieve full hunger and saturation bars"));

        // Advanced Building
        availableTasks.add(new SimonTask("Scaffold Builder",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getY() > player.getWorld().getHighestBlockYAt(loc) + 20 &&
                            loc.getBlock().getRelative(BlockFace.DOWN).getType().isSolid();
                },
                "Build and stand on a pillar 20 blocks above the highest point"));

        // Weather Challenges
        availableTasks.add(new SimonTask("Storm Chaser",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld().hasStorm() &&
                            loc.getBlock().getLightFromSky() == 15 &&
                            player.getInventory().getHelmet() == null;
                },
                "Stand in an open area during a thunderstorm without armor"));

        // Technical Tasks
        availableTasks.add(new SimonTask("Wireless Engineer",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getBlock().getType() == Material.OBSERVER &&
                            loc.getBlock().getRelative(BlockFace.UP).getType() == Material.REDSTONE_LAMP &&
                            loc.getBlock().getRelative(BlockFace.UP).isBlockPowered();
                },
                "Create a wireless redstone signal using observers"));

        availableTasks.add(new SimonTask("Sheep Rainbow",
                player -> {
                    return player.getNearbyEntities(10, 3, 10).stream()
                            .filter(e -> e instanceof Sheep)
                            .map(e -> (Sheep) e)
                            .map(Sheep::getColor)
                            .distinct()
                            .count() >= 5;
                },
                "Gather 5 differently colored sheep in one area"));

        // Advanced Combat
        availableTasks.add(new SimonTask("Trident Master",
                player -> {
                    return player.isGliding() &&
                            player.getInventory().getItemInMainHand().getType() == Material.TRIDENT &&
                            player.getLocation().getBlock().isLiquid();
                },
                "Throw a trident while gliding over water"));

        availableTasks.add(new SimonTask("TNT Jumper",
                player -> {
                    return !player.isOnGround() &&
                            player.getNearbyEntities(3, 3, 3).stream()
                                    .anyMatch(e -> e instanceof TNTPrimed);
                },
                "Jump using TNT explosion (without dying)"));

        // Music & Sound
        availableTasks.add(new SimonTask("Music Maker",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getBlock().getType() == Material.NOTE_BLOCK &&
                            loc.getBlock().getBlockPower() > 0;
                },
                "Play a note block melody"));

        // Advanced Building
        availableTasks.add(new SimonTask("Pixel Artist",
                player -> {
                    Location loc = player.getLocation();
                    Set<Material> colors = new HashSet<>();
                    for (int x = -2; x <= 2; x++) {
                        for (int y = 0; y <= 4; y++) {
                            Material type = loc.clone().add(x, y, 0).getBlock().getType();
                            if (type.name().contains("WOOL")) colors.add(type);
                        }
                    }
                    return colors.size() >= 6;
                },
                "Create a wool pixel art using 6 different colors"));

        // Ocean Tasks
        availableTasks.add(new SimonTask("Coral Collector",
                player -> {
                    return Stream.of(Material.BRAIN_CORAL, Material.BUBBLE_CORAL,
                                    Material.FIRE_CORAL, Material.HORN_CORAL,
                                    Material.TUBE_CORAL)
                            .allMatch(m -> player.getInventory().contains(m));
                },
                "Collect all 5 types of coral"));

        // Nether Challenges
        availableTasks.add(new SimonTask("Strider Racer",
                player -> {
                    return player.getVehicle() instanceof Strider &&
                            player.getLocation().getBlock().getType() == Material.LAVA;
                },
                "Ride a Strider across a lava lake"));

        // End Challenges
        availableTasks.add(new SimonTask("Dragon Breath Collector",
                player -> {
                    return player.getInventory().contains(Material.DRAGON_BREATH) &&
                            player.getLocation().getWorld().getEnvironment() == World.Environment.THE_END;
                },
                "Collect Dragon's Breath in The End"));

        // Village Tasks
        availableTasks.add(new SimonTask("Village Hero",
                player -> {
                    return player.getNearbyEntities(20, 10, 20).stream()
                            .filter(e -> e instanceof Villager)
                            .count() >= 5 &&
                            player.getStatistic(Statistic.RAID_WIN) > 0;
                },
                "Win a raid while protecting at least 5 villagers"));

        // Redstone Engineering
        availableTasks.add(new SimonTask("Logic Master",
                player -> {
                    Location loc = player.getLocation();
                    int comparators = 0;
                    int repeaters = 0;
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -3; z <= 3; z++) {
                            Block block = loc.clone().add(x, 0, z).getBlock();
                            if (block.getType() == Material.COMPARATOR) comparators++;
                            if (block.getType() == Material.REPEATER) repeaters++;
                        }
                    }
                    return comparators >= 2 && repeaters >= 2;
                },
                "Build a working logic circuit with comparators and repeaters"));

        // Farming Advanced
        availableTasks.add(new SimonTask("Bee Keeper",
                player -> {
                    return player.getNearbyEntities(10, 10, 10).stream()
                            .filter(e -> e instanceof Bee)
                            .count() >= 3 &&
                            player.getLocation().getBlock().getType() == Material.BEEHIVE;
                },
                "Maintain a beehive with at least 3 bees"));

        // Transportation Advanced
        availableTasks.add(new SimonTask("Rail Engineer",
                player -> {
                    Location loc = player.getLocation();
                    int railCount = 0;
                    for (int x = -5; x <= 5; x++) {
                        for (int z = -5; z <= 5; z++) {
                            Block block = loc.clone().add(x, 0, z).getBlock();
                            if (block.getType().name().contains("RAIL")) railCount++;
                        }
                    }
                    return railCount >= 10 &&
                            loc.getBlock().getType() == Material.POWERED_RAIL;
                },
                "Build a powered rail system with at least 10 tracks"));

        // Potion Making
        availableTasks.add(new SimonTask("Alchemist",
                player -> {
                    return player.getActivePotionEffects().stream()
                            .filter(effect -> effect.getType().equals(PotionEffectType.INVISIBILITY))
                            .findFirst()
                            .map(effect -> effect.getAmplifier() >= 1)
                            .orElse(false);
                },
                "Create and drink an Invisibility II potion"));

        // Advanced Movement
        availableTasks.add(new SimonTask("Parkour Master",
                player -> {
                    Location loc = player.getLocation();
                    return !player.isOnGround() &&
                            loc.getY() > loc.getWorld().getHighestBlockYAt(loc) + 10 &&
                            player.getInventory().getBoots() != null &&
                            player.getInventory().getBoots().getEnchantments().containsKey(Enchantment.FEATHER_FALLING);
                },
                "Perform a high jump with Feather Falling boots"));

        // Advanced Combat
        availableTasks.add(new SimonTask("Archery Challenge",
                player -> {
                    return player.getStatistic(Statistic.MOB_KILLS) >
                            player.getStatistic(Statistic.MOB_KILLS, EntityType.SKELETON) &&
                            player.getInventory().getItemInMainHand().getType() == Material.BOW;
                },
                "Defeat a Skeleton using only their own arrows"));

        // Weather Mastery
        availableTasks.add(new SimonTask("Lightning Hunter",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getWorld().hasStorm() &&
                            loc.getWorld().getHighestBlockYAt(loc) == loc.getBlockY() &&
                            player.getInventory().getHelmet() != null &&
                            player.getInventory().getHelmet().getEnchantments().containsKey(Enchantment.PROTECTION);
                },
                "Stand at the highest point during a thunderstorm with Protection armor"));

        // Food and Farming
        availableTasks.add(new SimonTask("Master Chef",
                player -> {
                    return player.getInventory().contains(Material.GOLDEN_CARROT) &&
                            player.getInventory().contains(Material.GOLDEN_APPLE) &&
                            player.getFoodLevel() == 20;
                },
                "Create golden food items and maintain full hunger"));

        // Advanced Building
        availableTasks.add(new SimonTask("Statue Maker",
                player -> {
                    Location loc = player.getLocation();
                    int height = 0;
                    Material baseMaterial = loc.getBlock().getType();
                    while (height < 10 &&
                            loc.clone().add(0, height, 0).getBlock().getType() == baseMaterial) {
                        height++;
                    }
                    return height >= 5 && baseMaterial.isSolid();
                },
                "Build a statue at least 5 blocks tall"));

        // Enchanting Mastery
        availableTasks.add(new SimonTask("Enchanted Warriors",
                player -> {
                    ItemStack[] armor = player.getInventory().getArmorContents();
                    return Arrays.stream(armor)
                            .filter(Objects::nonNull)
                            .allMatch(item -> !item.getEnchantments().isEmpty());
                },
                "Wear a full set of enchanted armor"));

        // Advanced Redstone
        availableTasks.add(new SimonTask("Hidden Door",
                player -> {
                    Location loc = player.getLocation();
                    return loc.getBlock().getType() == Material.PISTON &&
                            loc.getBlock().getBlockPower() > 0 &&
                            loc.clone().add(0, 1, 0).getBlock().getType().isOccluding();
                },
                "Create a hidden piston door"));
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