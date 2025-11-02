package org.shotrush.atom.content.foragingage.throwing;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.projectiles.CustomProjectile;

public class SpearProjectileListener implements Listener {
    
    private final Atom plugin;
    
    public SpearProjectileListener(Atom plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        
        ItemStack item = trident.getItemStack();

        CustomItem spearItem = plugin.getItemRegistry().getItem("wood_spear");
        if (spearItem == null || !spearItem.isCustomItem(item)) return;
        
        Player shooter = (Player) trident.getShooter();
        if (shooter == null) return;
        
        ItemStack thrownItem = item.clone();
        
        org.shotrush.atom.core.items.ItemQuality quality = org.shotrush.atom.core.api.ItemQualityAPI.getQuality(thrownItem);
        org.shotrush.atom.core.util.DurabilityUtil.applyQualityBasedDamage(thrownItem, quality);
        
        if (thrownItem.getType() == Material.AIR || thrownItem.getAmount() <= 0) {
            event.setCancelled(true);
            trident.remove();
            return;
        }
        
        ItemStack itemInHand = shooter.getInventory().getItemInMainHand();
        if (itemInHand.isSimilar(item)) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            ItemStack offHand = shooter.getInventory().getItemInOffHand();
            if (offHand.isSimilar(item)) {
                offHand.setAmount(offHand.getAmount() - 1);
            }
        }
        
        trident.setDamage(8.0);
        
        trident.getScheduler().runAtFixedRate(plugin, task -> {
            if (!trident.isValid() || trident.isInBlock() || trident.isOnGround()) {
                Location dropLoc = trident.getLocation();
                trident.getWorld().dropItemNaturally(dropLoc, thrownItem);
                trident.remove();
                task.cancel();
            }
        }, null, 1L, 1L);
    }
}
