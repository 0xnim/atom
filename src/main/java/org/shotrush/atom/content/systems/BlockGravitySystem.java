package org.shotrush.atom.content.systems;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.HashSet;
import java.util.Set;

@AutoRegisterSystem(priority = 1)
public class BlockGravitySystem implements Listener {
    
    private final Atom plugin;
    private static final NamespacedKey SPLIT_PLANK_KEY = new NamespacedKey("atom", "split_plank");
    private final Set<Location> splitPlankLocations = new HashSet<>();
    
    public BlockGravitySystem(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        
        Block landingBlock = event.getBlock();
        
        if (landingBlock.getType() != Material.AIR) {
            event.setCancelled(true);
            return;
        }
        
        boolean isFelled = fallingBlock.getPersistentDataContainer().has(
            new NamespacedKey("atom", "felled"),
            PersistentDataType.BYTE
        );
        
        Block blockBelow = landingBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
        
        if (blockBelow.getType() == Material.POINTED_DRIPSTONE) {
            Material logType = fallingBlock.getBlockData().getMaterial();
            
            if (isWoodLog(logType)) {
                event.setCancelled(true);
                fallingBlock.remove();
                
                Material plankType = getPlankFromLog(logType);
                Location spawnLoc = landingBlock.getLocation().add(0.5, 0.5, 0.5);
                
                for (int i = 0; i < 4; i++) {
                    FallingBlock plankBlock = landingBlock.getWorld().spawnFallingBlock(
                        spawnLoc,
                        plankType.createBlockData()
                    );
                    plankBlock.setDropItem(false);
                    plankBlock.setHurtEntities(false);
                    plankBlock.getPersistentDataContainer().set(SPLIT_PLANK_KEY, PersistentDataType.BYTE, (byte) 1);
                    
                    org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(
                        (Math.random() - 0.5) * 0.3,
                        0.2 + Math.random() * 0.1,
                        (Math.random() - 0.5) * 0.3
                    );
                    plankBlock.setVelocity(velocity);
                }
            }
        }
        
        if (fallingBlock.getPersistentDataContainer().has(SPLIT_PLANK_KEY, PersistentDataType.BYTE)) {
            splitPlankLocations.add(landingBlock.getLocation());
        }
        
        if (isFelled && landingBlock.getState() instanceof org.bukkit.block.TileState tileState) {
            tileState.getPersistentDataContainer().set(
                new NamespacedKey("atom", "felled"),
                PersistentDataType.BYTE,
                (byte) 1
            );
            tileState.update();
        }
    }
    
    @EventHandler
    public void onPlankBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (splitPlankLocations.contains(block.getLocation()) && isPlankBlock(block.getType())) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(
                block.getLocation().add(0.5, 0.5, 0.5),
                new ItemStack(block.getType(), 1)
            );
            splitPlankLocations.remove(block.getLocation());
        }
    }
    
    private boolean isWoodLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM");
    }
    
    private boolean isPlankBlock(Material material) {
        String name = material.name();
        return name.endsWith("_PLANKS");
    }
    
    private Material getPlankFromLog(Material log) {
        String logName = log.name();
        
        if (logName.contains("STRIPPED_")) {
            logName = logName.replace("STRIPPED_", "");
        }
        
        String plankName = logName.replace("_LOG", "_PLANKS").replace("_STEM", "_PLANKS");
        
        try {
            return Material.valueOf(plankName);
        } catch (IllegalArgumentException e) {
            return Material.OAK_PLANKS;
        }
    }
}
