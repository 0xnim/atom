package org.shotrush.atom.content.systems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.core.data.PersistentData;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;
@AutoRegisterSystem(priority = 3)
public class ItemHeatSystem implements Listener {
    private final Atom plugin;
    private static final NamespacedKey HEAT_MODIFIER_KEY = new NamespacedKey("atom", "heat_modifier");
    
    public ItemHeatSystem(org.bukkit.plugin.Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startHeatTickForPlayer(player);
    }
    
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && item.getType() != Material.AIR) {
            applyHeatEffect(player, item);
        } else {
            removeHeatEffect(player);
        }
    }
    
    private void startHeatTickForPlayer(Player player) {
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem != null && heldItem.getType() != Material.AIR) {
                updateItemHeatFromEnvironment(player, heldItem);
                applyHeatEffect(player, heldItem);
                displayHeatActionBar(player, heldItem);
            }
        }, null, 1L, 20L);
    }
    
    private void displayHeatActionBar(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        
        org.shotrush.atom.core.ui.ActionBarManager manager = org.shotrush.atom.core.ui.ActionBarManager.getInstance();
        if (manager == null) return;
        
        if (Math.abs(heat) < 1.0) {
            manager.removeMessage(player, "item_heat");
            return;
        }
        
        String color;
        if (heat > 100) {
            color = "§c";
        } else if (heat > 50) {
            color = "§6";
        } else if (heat > 0) {
            color = "§e";
        } else if (heat < -50) {
            color = "§b";
        } else {
            color = "§3";
        }
        
        String message = "§7Item: " + color + (int)heat + "°C";
        manager.setMessage(player, "item_heat", message);
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Item droppedItem = event.getItemDrop();
        ItemStack item = droppedItem.getItemStack();
        
        double heat = getItemHeat(item);
        
        
        if (heat >= 50) {
            ItemStack chestplate = player.getInventory().getChestplate();
            boolean hasProtection = chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE;
            
            if (!hasProtection) {
                player.setFireTicks(40); 
            }
        }
        
        startDroppedItemHeatTracking(droppedItem);
    }
    
    private void startDroppedItemHeatTracking(Item droppedItem) {
        droppedItem.getScheduler().runAtFixedRate(plugin, task -> {
            if (droppedItem.isDead() || !droppedItem.isValid()) {
                task.cancel();
                return;
            }
            
            ItemStack itemStack = droppedItem.getItemStack();
            double currentHeat = getItemHeat(itemStack);
            org.bukkit.Location loc = droppedItem.getLocation();
            
            double heatChange = org.shotrush.atom.core.api.EnvironmentalFactorAPI
                .getNearbyHeatSources(loc, 6);
            
            double ambientTemp = 20.0;
            if (currentHeat > ambientTemp) {
                heatChange -= 0.5;
            }
            
            double newHeat = Math.max(0, currentHeat + heatChange * 0.1);
            setItemHeat(itemStack, newHeat);
            droppedItem.setItemStack(itemStack);
            
            if (newHeat >= 200) {
                double fireChance = Math.min(0.5, (newHeat - 200) / 600);
                
                if (Math.random() < fireChance) {
                    org.bukkit.block.Block below = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                    if (below.getType().isBurnable() || below.getType() == Material.AIR) {
                        loc.getBlock().setType(Material.FIRE);
                    }
                }
            }
        }, null, 20L, 20L);
    }
    
    private void updateItemHeatFromEnvironment(Player player, ItemStack item) {
        double currentHeat = getItemHeat(item);
        org.bukkit.Location loc = player.getLocation();
        
        double heatChange = org.shotrush.atom.core.api.EnvironmentalFactorAPI
            .calculateEnvironmentalTemperatureChange(player, loc, 0.1);
        
        double ambientTemp = 20.0;
        if (currentHeat > ambientTemp) {
            heatChange -= 0.5;
        } else if (currentHeat < ambientTemp) {
            heatChange += 0.3;
        }
        
        double newHeat = Math.max(-100, Math.min(1000, currentHeat + heatChange));
        
        if (Math.abs(newHeat - currentHeat) > 5.0) {
            setItemHeat(item, newHeat);
        }
    }
    
    private void applyHeatEffect(Player player, ItemStack item) {
        double heat = getItemHeat(item);
        boolean hasProtection = org.shotrush.atom.core.api.ArmorProtectionAPI.hasLeatherChestplate(player);
        
        if (heat != 0) {
            double speedModifier = -Math.abs(heat) * 0.001;
            org.shotrush.atom.core.api.AttributeModifierAPI.applyModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY,
                speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            );
        } else {
            org.shotrush.atom.core.api.AttributeModifierAPI.removeModifier(
                player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
            );
        }
        
        org.shotrush.atom.core.api.TemperatureEffectsAPI.applyHeatDamage(player, heat, hasProtection);
        org.shotrush.atom.core.api.TemperatureEffectsAPI.applyColdDamage(player, heat, hasProtection);
    }
    
    private void removeHeatEffect(Player player) {
        org.shotrush.atom.core.api.AttributeModifierAPI.removeModifier(
            player, Attribute.MOVEMENT_SPEED, HEAT_MODIFIER_KEY
        );
    }
    
    public static double getItemHeat(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        
        return PersistentData.getDouble(item.getItemMeta(), "item_heat", 0.0);
    }
    
    public static void setItemHeat(ItemStack item, double heat) {
        if (item == null || item.getType() == Material.AIR) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentData.set(meta, "item_heat", heat);
        
        item.setItemMeta(meta);
    }
}
