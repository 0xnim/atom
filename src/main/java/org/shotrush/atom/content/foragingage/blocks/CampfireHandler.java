package org.shotrush.atom.content.foragingage.blocks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.util.ActionBarManager;
import org.shotrush.atom.core.workstations.WorkstationHandler;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

import java.util.concurrent.ThreadLocalRandom;


@RegisterSystem(
    id = "campfire_handler",
    priority = 5,
    toggleable = true,
    description = "Handles campfire lighting mechanics"
)
public class CampfireHandler extends WorkstationHandler<CampfireHandler.LightingProgress> {
    
    
    private static final int MIN_STROKES = 8;
    private static final int MAX_STROKES = 12;
    private static final double SPARK_PARTICLE_CHANCE = 0.3;
    
    private static CampfireHandler instance;
    
    public CampfireHandler(Plugin plugin) {
        super();
        instance = this;
    }
    
    @Override
    public boolean isValidTool(ItemStack item) {
        return org.shotrush.atom.UtilKt.matches(item, "atom:pebble");
    }
    
    @Override
    protected Sound getStrokeSound() {
        return Sound.BLOCK_STONE_HIT;
    }
    
    @Override
    protected void spawnStrokeParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location particleLoc = location.clone().add(0, 0.5, 0);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 3, 0.1, 0.1, 0.1, 0.01);
        
        
        if (ThreadLocalRandom.current().nextDouble() < SPARK_PARTICLE_CHANCE) {
            world.spawnParticle(Particle.LAVA, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
    }
    
    @Override
    protected String getStatusMessage() {
        return "§7Striking flint... Creating sparks";
    }
    
    @Override
    public org.joml.AxisAngle4f getItemRotation() {
        return new org.joml.AxisAngle4f(0, 0, 0, 1);
    }
    
    @Override
    public org.joml.Vector3f getItemScale() {
        return new org.joml.Vector3f(1, 1, 1);
    }
    
    @Override
    public boolean handleInteraction(PlayerInteractEvent event, Block block, Player player, ItemStack hand, org.shotrush.atom.core.workstations.WorkstationData data) {
        return false; 
    }
    
    @Override
    public boolean canPlaceItem(ItemStack item) {
        return false;
    }
    
    @Override
    public org.joml.Vector3f getPlacementPosition() {
        return new org.joml.Vector3f(0, 0, 0);
    }
    
    @Override
    public int getMaxItems() {
        return 0;
    }
    
    @Override
    public String getFullMessage() {
        return "";
    }
    
    @Override
    public String getEmptyMessage() {
        return "";
    }
    
    
    static class LightingProgress extends WorkstationHandler.WorkProgress {
        final Campfire campfire;
        
        LightingProgress(long startTime, Location location, Campfire campfire) {
            super(startTime, ThreadLocalRandom.current().nextInt(MIN_STROKES, MAX_STROKES + 1), location);
            this.campfire = campfire;
        }
    }
    
    
    public static void startLighting(Player player, Campfire campfire) {
        if (instance == null || instance.isProcessing(player)) return;
        
        Location campfireLocation = campfire.getSpawnLocation();
        LightingProgress progress = new LightingProgress(
            System.currentTimeMillis(), 
            campfireLocation, 
            campfire
        );
        
        instance.startProcessing(
            player, 
            campfireLocation, 
            progress, 
            () -> finishLighting(player, campfire),
            "§7Striking flint... Creating sparks"
        );
    }
    
    
    public static boolean isLighting(Player player) {
        return instance != null && instance.isProcessing(player);
    }
    
    
    private static void finishLighting(Player player, Campfire campfire) {
        
        if (ThreadLocalRandom.current().nextDouble() < Campfire.getLightingFailureChance()) {
            handleLightingFailure(player);
            return;
        }
        
        
        campfire.light();
        handleLightingSuccess(player, campfire);
    }
    
    
    private static void handleLightingFailure(Player player) {
        ActionBarManager.send(player, "§cThe sparks didn't catch! Try again.");
        ActionBarManager.clearStatus(player);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
    }
    
    
    private static void handleLightingSuccess(Player player, Campfire campfire) {
        ActionBarManager.send(player, "§aThe campfire ignites!");
        ActionBarManager.clearStatus(player);
        
        
        Location playerLoc = player.getLocation();
        player.playSound(playerLoc, Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.0f);
        player.playSound(playerLoc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
        
        
        World world = campfire.getSpawnLocation().getWorld();
        if (world != null) {
            Location particleLoc = campfire.getSpawnLocation().clone().add(0, 0.5, 0);
            world.spawnParticle(Particle.FLAME, particleLoc, 20, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticle(Particle.LAVA, particleLoc, 5, 0.2, 0.2, 0.2, 0);
        }
    }
}
