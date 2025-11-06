package org.shotrush.atom.content.foragingage.workstations.grounditems;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.api.annotation.RegisterSystem;
import org.shotrush.atom.core.blocks.CustomBlock;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.util.ActionBarManager;


@RegisterSystem(
    id = "ground_item_handler",
    priority = 6,
    toggleable = true,
    description = "Allows placing items on the ground"
)
public class GroundItemHandler implements Listener {
    
    private final Atom plugin;
    
    public GroundItemHandler(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getClickedBlock() == null) return;
        if (event.getBlockFace() != BlockFace.UP) return;
        
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        
        
        if (!clicked.getType().isSolid()) return;
        
        
        Location surfaceLoc = clicked.getLocation().add(0, 1, 0);
        Block surfaceBlock = surfaceLoc.getBlock();
        
        
        if (!surfaceBlock.getType().isAir()) return;
        
        
        CustomBlock existing = plugin.getBlockManager().getBlockAt(surfaceLoc);
        
        if (existing instanceof GroundItemSurface) {
            
            GroundItemSurface surface = (GroundItemSurface) existing;
            
            
            if (surface.onWrenchInteract(player, false)) {
                event.setCancelled(true);
                
                
                if (!surface.hasItems()) {
                    surface.remove();
                    surface.onRemoved();
                    plugin.getBlockManager().getBlocks().remove(existing);
                }
            }
            return;
        }
        
        
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return;
        
        
        if (plugin.getItemRegistry().getItem("wrench") != null &&
            plugin.getItemRegistry().getItem("wrench").isCustomItem(hand)) {
            return;
        }
        
        
        Location spawnLoc = surfaceLoc.clone().add(0.5, 0, 0.5);
        GroundItemSurface surface = new GroundItemSurface(spawnLoc, BlockFace.UP);
        
        
        plugin.getBlockManager().getBlocks().add(surface);
        
        
        surface.spawn(plugin);
        
        
        if (surface.onWrenchInteract(player, false)) {
            event.setCancelled(true);
        } else {
            
            surface.remove();
            plugin.getBlockManager().getBlocks().remove(surface);
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        
        if (!(event.getEntity() instanceof ItemDisplay)) return;
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        Entity clicked = event.getEntity();
        
        
        for (CustomBlock block : plugin.getBlockManager().getBlocks()) {
            if (!(block instanceof GroundItemSurface)) continue;
            
            GroundItemSurface surface = (GroundItemSurface) block;
            
            
            if (surface.removeItemByUUID(clicked.getUniqueId())) {
                ActionBarManager.send(player, "§aItem picked up");
                event.setCancelled(true);
                
                
                if (!surface.hasItems()) {
                    surface.remove();
                    plugin.getBlockManager().getBlocks().remove(surface);
                }
                
                return;
            }
        }
    }
}
