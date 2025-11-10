package org.shotrush.atom.content.foragingage.workstations.leatherbed;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.shotrush.atom.Atom;
import org.shotrush.atom.content.foraging.items.SharpenedFlint;
import org.shotrush.atom.core.workstations.WorkstationData;
import org.shotrush.atom.core.util.ActionBarManager;
import org.shotrush.atom.core.workstations.WorkstationHandler;
import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

@RegisterSystem(
    id = "leather_bed_handler",
    priority = 5,
    toggleable = true,
    description = "Handles leather curing and brushing mechanics"
)
public class LeatherBedHandler extends WorkstationHandler<LeatherBedHandler.BrushingProgress> {
    
    private static LeatherBedHandler instance;
    
    public LeatherBedHandler(Plugin plugin) {
        super();
        instance = this;
        WorkstationData.registerHandler("leather_bed", this);
    }
    
    @Override
    public AxisAngle4f getItemRotation() {
        return new AxisAngle4f((float) Math.toRadians(0), 1, 0, 0);
    }
    
    @Override
    public Vector3f getItemScale() {
        return new Vector3f(1.2f, 1.2f, 0.5f);
    }
    
    @Override
    public boolean handleInteraction(PlayerInteractEvent event, Block block, Player player, ItemStack hand, WorkstationData data) {
        
        if (org.shotrush.atom.UtilKt.matches(hand, "atom:sharpened_flint") || 
            org.shotrush.atom.UtilKt.matches(hand, "atom:knife")) {
            if (data.getPlacedItems().isEmpty()) {
                ActionBarManager.send(player, getEmptyMessage());
                event.setCancelled(true);
                return true;
            }
            if (!isBrushing(player)) {
                startBrushing(player, data, hand);
            }
            return true; 
        }
        
        
        if (canPlaceItem(hand)) {
            if (data.getPlacedItems().size() >= getMaxItems()) {
                ActionBarManager.send(player, getFullMessage());
                return false;
            }
            event.setCancelled(true);
            if (data.placeItem(hand, getPlacementPosition(), 0)) {
                hand.setAmount(hand.getAmount() - 1);
                player.swingMainHand();
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean canPlaceItem(ItemStack item) {
        return org.shotrush.atom.UtilKt.matches(item, "atom:uncured_leather");
    }
    
    @Override
    public Vector3f getPlacementPosition() {
        return new Vector3f(-0.05f, 0.25f, 0.10f);
    }
    
    @Override
    public int getMaxItems() {
        return 1;
    }
    
    @Override
    public String getFullMessage() {
        return "§cLeather bed is full!";
    }
    
    @Override
    public String getEmptyMessage() {
        return "§cPlace uncured leather first!";
    }
    
    @Override
    public boolean isValidTool(ItemStack item) {
        return org.shotrush.atom.UtilKt.matches(item, "atom:sharpened_flint") || 
               org.shotrush.atom.UtilKt.matches(item, "atom:knife");
    }
    
    @Override
    protected Sound getStrokeSound() {
        return Sound.ITEM_BRUSH_BRUSHING_GENERIC;
    }
    
    @Override
    protected void spawnStrokeParticles(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location particleLoc = location.clone().add(0, 1, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(
            org.bukkit.Color.fromRGB(139, 69, 19), 
            1.0f
        );
        world.spawnParticle(Particle.DUST, particleLoc, 10, 0.2, 0.2, 0.2, 0, dustOptions);
    }
    
    @Override
    protected String getStatusMessage() {
        return "§7Scraping leather... Use the tool carefully";
    }
    
    static class BrushingProgress extends WorkstationHandler.WorkProgress {
        ItemStack tool;
        WorkstationData workstation;
        
        BrushingProgress(long startTime, Location location, ItemStack tool, WorkstationData workstation) {
            super(startTime, 20 + (int)(Math.random() * 11), location);
            this.tool = tool;
            this.workstation = workstation;
        }
    }
    
    public static void startBrushing(Player player, WorkstationData workstation, ItemStack tool) {
        if (instance == null) return;
        if (instance.isProcessing(player)) return;
        
        Location location = workstation.getBlockLocation().clone().add(0.5, 0.5, 0.5);
        BrushingProgress progress = new BrushingProgress(System.currentTimeMillis(), location, tool, workstation);
        
        instance.startProcessing(player, location, progress, () -> {
            finishBrushing(player, workstation, tool);
        }, "§7Scraping leather... Use the tool carefully");
    }
    
    public static boolean isBrushing(Player player) {
        return instance != null && instance.isProcessing(player);
    }
    
    private static void finishBrushing(Player player, WorkstationData workstation, ItemStack tool) {
        Location location = workstation.getBlockLocation().clone().add(0.5, 0.5, 0.5);
        
        player.sendMessage("§eDEBUG: finishBrushing called at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        
        
        workstation.removeLastItem();
        
        
        player.sendMessage("§eDEBUG: Creating raw_meat via CraftEngine...");
        CustomItem<ItemStack> rawMeatItem = CraftEngineItems.byId(Key.of("atom:raw_meat"));
        if (rawMeatItem != null) {
            ItemStack rawMeat = rawMeatItem.buildItemStack();
            if (rawMeat != null) {
                player.sendMessage("§eDEBUG: Dropping raw_meat...");
                player.getWorld().dropItemNaturally(location, rawMeat);
            }
        } else {
            player.sendMessage("§cDEBUG: raw_meat CustomItem is NULL!");
        }
        
        player.sendMessage("§eDEBUG: Creating stabilized_leather via CraftEngine...");
        CustomItem<ItemStack> leatherItem = CraftEngineItems.byId(Key.of("atom:stabilized_leather"));
        if (leatherItem != null) {
            ItemStack stabilizedLeather = leatherItem.buildItemStack();
            if (stabilizedLeather != null) {
                player.sendMessage("§eDEBUG: Dropping stabilized_leather...");
                player.getWorld().dropItemNaturally(location, stabilizedLeather);
            }
        } else {
            player.sendMessage("§cDEBUG: stabilized_leather CustomItem is NULL!");
        }
        
        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3);
        }
        
        player.playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f);
        ActionBarManager.send(player, "§aScraped the leather successfully!");
        ActionBarManager.clearStatus(player);
    }
}
