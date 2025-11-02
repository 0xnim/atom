package org.shotrush.atom.content.systems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoRegisterSystem(priority = 1)
public class SimpleBlockCarrying implements Listener {
    
    private final Atom plugin;
    private final Map<UUID, ItemDisplay> carriedBlocks = new HashMap<>();
    private static final NamespacedKey CARRIED_KEY = new NamespacedKey("atom", "carried");
    
    public SimpleBlockCarrying(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking()) return;
        
        Player player = event.getPlayer();
        boolean isCarrying = carriedBlocks.containsKey(player.getUniqueId());
        
        if (isCarrying) {
            dropBlock(player);
            event.setCancelled(true);
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            org.bukkit.block.Block block = event.getClickedBlock();
            Material blockType = block.getType();
            
            if (blockType.isBlock() && blockType.isSolid() && blockType != Material.BEDROCK) {
                pickupBlock(player, blockType);
                block.setType(Material.AIR);
                event.setCancelled(true);
            }
        }
    }
    
    private void pickupBlock(Player player, Material blockType) {
        org.bukkit.Location spawnLoc = player.getEyeLocation().add(0, 0.5, 0);
        spawnLoc.setPitch(0);
        spawnLoc.setYaw(0);
        
        ItemDisplay display = (ItemDisplay) spawnLoc.getWorld().spawnEntity(
            spawnLoc,
            org.bukkit.entity.EntityType.ITEM_DISPLAY
        );
        
        display.setItemStack(new ItemStack(blockType));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
        display.setInterpolationDuration(2);
        display.setInterpolationDelay(0);
        display.setGravity(false);
        
        Transformation transformation = new Transformation(
            new Vector3f(0, 0.4f, 0),
            new AxisAngle4f(0, 0, 0, 1),
            new Vector3f(1.2f, 1.2f, 1.2f),
            new AxisAngle4f(0, 0, 0, 1)
        );
        display.setTransformation(transformation);
        
        carriedBlocks.put(player.getUniqueId(), display);
        player.getPersistentDataContainer().set(CARRIED_KEY, PersistentDataType.STRING, blockType.name());
        
        player.getScheduler().runDelayed(plugin, task -> {
            if (display.isValid() && player.isOnline()) {
                player.addPassenger(display);
            }
        }, null, 1L);
    }
    
    private void dropBlock(Player player) {
        ItemDisplay display = carriedBlocks.remove(player.getUniqueId());
        if (display == null) return;
        
        ItemStack item = display.getItemStack();
        player.removePassenger(display);
        display.remove();
        player.getPersistentDataContainer().remove(CARRIED_KEY);
        
        if (item != null && item.getType().isBlock()) {
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            
            if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                org.bukkit.block.BlockFace face = player.getTargetBlockFace(5);
                if (face != null) {
                    org.bukkit.block.Block placeBlock = targetBlock.getRelative(face);
                    if (placeBlock.getType() == Material.AIR) {
                        placeBlock.setType(item.getType());
                        return;
                    }
                }
            }
            
            player.getWorld().dropItemNaturally(
                player.getLocation().add(player.getLocation().getDirection().multiply(2)),
                item
            );
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dropBlock(event.getPlayer());
    }
}
