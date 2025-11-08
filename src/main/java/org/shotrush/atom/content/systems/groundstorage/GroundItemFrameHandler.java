package org.shotrush.atom.content.systems.groundstorage;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.api.annotation.RegisterSystem;


@RegisterSystem(
    id = "ground_item_frame_handler",
    priority = 15,
    description = "Allows placing items on the ground using invisible item frames",
    toggleable = true,
    enabledByDefault = true
)
public class GroundItemFrameHandler implements Listener {

    private final NamespacedKey groundItemKey;

    public GroundItemFrameHandler(Plugin plugin) {
        this.groundItemKey = new NamespacedKey(plugin, "ground_item_frame");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        
        if (event.getBlockFace() != BlockFace.UP) return;
        
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) return;
        
        
        Material blockType = block.getType();
        if (!blockType.isSolid()) return;
        if (blockType.isInteractable()) return;
        
        
        event.setCancelled(true);
        
        
        org.bukkit.Location loc = block.getLocation().add(0.5, 1.0, 0.5);
        
        
        ItemStack itemToPlace = hand.clone();
        itemToPlace.setAmount(1);
        
        
        ItemFrame frame = block.getWorld().spawn(loc, ItemFrame.class);
        frame.setFacingDirection(BlockFace.UP);
        frame.setVisible(false);
        frame.setFixed(false);
        frame.setItem(itemToPlace, false);
        frame.getPersistentDataContainer().set(groundItemKey, PersistentDataType.BYTE, (byte) 1);
        
        
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFrameBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!frame.getPersistentDataContainer().has(groundItemKey, PersistentDataType.BYTE)) return;

        if (event.getCause() == HangingBreakEvent.RemoveCause.OBSTRUCTION) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = frame.getItem();
        if (item != null && item.getType() != Material.AIR) {
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        }
        
        
        event.setCancelled(true);
        frame.remove();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!frame.getPersistentDataContainer().has(groundItemKey, PersistentDataType.BYTE)) return;
        
        
        ItemStack item = frame.getItem();
        if (item != null && item.getType() != Material.AIR) {
            frame.getWorld().dropItemNaturally(frame.getLocation(), item);
        }
        
        
        event.setCancelled(true);
        frame.remove();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (!frame.getPersistentDataContainer().has(groundItemKey, PersistentDataType.BYTE)) return;
        
        
        event.setCancelled(true);
        
        
        ItemStack item = frame.getItem();
        if (item != null && item.getType() != Material.AIR) {
            event.getPlayer().getInventory().addItem(item);
            frame.remove();
        }
    }
}
