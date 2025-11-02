package org.shotrush.atom.content.foragingage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

import java.util.*;

@AutoRegisterSystem(priority = 1)
public class WoodFeller implements Listener {
    
    private final Atom plugin;
    private static final NamespacedKey STRIP_STAGE_KEY = new NamespacedKey("atom", "strip_stage");
    private final Map<Location, Integer> blockStages = new HashMap<>();
    private final Map<Location, WoodType> blockWoodTypes = new HashMap<>();
    
    public WoodFeller(Plugin plugin) {
        this.plugin = (Atom) plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material blockType = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        if (isWoodBlock(blockType) || isStrippedBlock(blockType)) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            
            if (isSharpenedFlint(tool)) {
                handleWoodStripping(block, player);
            }
            event.setCancelled(true);
            return;
        }
        
        if (isLeafBlock(blockType)) {
            event.setDropItems(false);
            
            if (Math.random() < 0.15) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.VINE, 1));
            }
        }
    }
    
    private boolean isStrippedBlock(Material material) {
        return material.name().contains("_WALL") || 
               material.name().contains("_FENCE") ||
               material.name().contains("_PANE");
    }
    
    private void handleWoodStripping(Block block, Player player) {
        int stage = getStripStage(block);
        WoodType woodType = blockWoodTypes.get(block.getLocation());
        
        if (woodType == null) {
            woodType = WoodType.fromLog(block.getType());
            if (woodType == null) return;
            blockWoodTypes.put(block.getLocation(), woodType);
        }
        
        if (stage == 0) {
            block.getWorld().dropItemNaturally(block.getLocation(), createBark(woodType));
            block.setType(woodType.getStrippedLog());
            setStripStage(block, 1);
        } else if (stage == 1) {
            block.setType(woodType.getWall());
            setStripStage(block, 2);
        } else if (stage == 2) {
            block.setType(woodType.getFence());
            setStripStage(block, 3);
        } else if (stage >= 3) {
            dropTreeSideways(block, woodType, player);
            block.setType(Material.AIR);
            removeStripStage(block);
            blockWoodTypes.remove(block.getLocation());
        }
    }
    
    private void dropTreeSideways(Block startBlock, WoodType woodType, Player player) {
        Set<Block> treeBlocks = new HashSet<>();
        Queue<Block> toCheck = new LinkedList<>();
        toCheck.add(startBlock.getRelative(BlockFace.UP));
        
        while (!toCheck.isEmpty() && treeBlocks.size() < 500) {
            Block current = toCheck.poll();
            
            if (treeBlocks.contains(current)) continue;
            if (!isWoodBlock(current.getType()) && !isLeafBlock(current.getType())) continue;
            
            treeBlocks.add(current);
            
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, 
                                                   BlockFace.EAST, BlockFace.WEST}) {
                toCheck.add(current.getRelative(face));
            }
        }
        
        org.bukkit.util.Vector direction = player.getLocation().getDirection().setY(0).normalize();
        BlockFace fallDirection = getClosestBlockFace(direction);
        
        Map<Block, Location> dropLocations = new HashMap<>();
        for (Block block : treeBlocks) {
            int heightAboveBase = block.getY() - startBlock.getY();
            
            Location dropLoc = startBlock.getLocation().clone()
                .add(0.5, 0.5, 0.5)
                .add(direction.clone().multiply(heightAboveBase * 0.7));
            
            dropLoc.setY(startBlock.getY() + Math.random() * 2);
            
            dropLocations.put(block, dropLoc);
        }
        
        for (Block block : treeBlocks) {
            Material blockType = block.getType();
            Location blockLoc = block.getLocation().add(0.5, 0, 0.5);
            
            if (isWoodBlock(blockType)) {
                org.bukkit.entity.FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(
                    blockLoc, 
                    block.getBlockData()
                );
                fallingBlock.setDropItem(false);
                fallingBlock.setCancelDrop(false);
                fallingBlock.setHurtEntities(false);
                fallingBlock.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey("atom", "felled"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1
                );
                
                org.bukkit.util.Vector velocity = direction.clone().multiply(0.3);
                velocity.setY(0.1 + Math.random() * 0.2);
                fallingBlock.setVelocity(velocity);
                
            } else if (isLeafBlock(blockType)) {
                org.bukkit.entity.FallingBlock fallingLeaf = block.getWorld().spawnFallingBlock(
                    blockLoc,
                    block.getBlockData()
                );
                fallingLeaf.setDropItem(false);
                fallingLeaf.setCancelDrop(false);
                fallingLeaf.setHurtEntities(false);
                
                org.bukkit.util.Vector velocity = direction.clone().multiply(0.2);
                velocity.setY(0.05 + Math.random() * 0.1);
                fallingLeaf.setVelocity(velocity);
            }
            
            block.setType(Material.AIR);
            blockWoodTypes.remove(block.getLocation());
            blockStages.remove(block.getLocation());
        }
    }
    
    private BlockFace getClosestBlockFace(org.bukkit.util.Vector direction) {
        double x = direction.getX();
        double z = direction.getZ();
        
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }
    
    private int getStripStage(Block block) {
        return blockStages.getOrDefault(block.getLocation(), 0);
    }
    
    private void setStripStage(Block block, int stage) {
        blockStages.put(block.getLocation(), stage);
    }
    
    private void removeStripStage(Block block) {
        blockStages.remove(block.getLocation());
    }
    
    private boolean isSharpenedFlint(ItemStack item) {
        if (item == null) return false;
        return plugin.getItemRegistry().getItem("sharpened_flint") != null &&
               plugin.getItemRegistry().getItem("sharpened_flint").isCustomItem(item);
    }
    
    private ItemStack createBark(WoodType woodType) {
        ItemStack bark = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta meta = bark.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("§6" + woodType.getDisplayName() + " Bark"));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("§7Stripped from a tree"),
            net.kyori.adventure.text.Component.text("§8[Foraging Age Material]")
        ));
        bark.setItemMeta(meta);
        return bark;
    }
    
    private boolean isWoodBlock(Material material) {
        return material == Material.OAK_LOG ||
               material == Material.SPRUCE_LOG ||
               material == Material.BIRCH_LOG ||
               material == Material.JUNGLE_LOG ||
               material == Material.ACACIA_LOG ||
               material == Material.DARK_OAK_LOG ||
               material == Material.MANGROVE_LOG ||
               material == Material.CHERRY_LOG ||
               material == Material.CRIMSON_STEM ||
               material == Material.WARPED_STEM ||
               material == Material.STRIPPED_OAK_LOG ||
               material == Material.STRIPPED_SPRUCE_LOG ||
               material == Material.STRIPPED_BIRCH_LOG ||
               material == Material.STRIPPED_JUNGLE_LOG ||
               material == Material.STRIPPED_ACACIA_LOG ||
               material == Material.STRIPPED_DARK_OAK_LOG ||
               material == Material.STRIPPED_MANGROVE_LOG ||
               material == Material.STRIPPED_CHERRY_LOG ||
               material == Material.STRIPPED_CRIMSON_STEM ||
               material == Material.STRIPPED_WARPED_STEM ||
               material == Material.OAK_WOOD ||
               material == Material.SPRUCE_WOOD ||
               material == Material.BIRCH_WOOD ||
               material == Material.JUNGLE_WOOD ||
               material == Material.ACACIA_WOOD ||
               material == Material.DARK_OAK_WOOD ||
               material == Material.MANGROVE_WOOD ||
               material == Material.CHERRY_WOOD ||
               material == Material.CRIMSON_HYPHAE ||
               material == Material.WARPED_HYPHAE ||
               material == Material.STRIPPED_OAK_WOOD ||
               material == Material.STRIPPED_SPRUCE_WOOD ||
               material == Material.STRIPPED_BIRCH_WOOD ||
               material == Material.STRIPPED_JUNGLE_WOOD ||
               material == Material.STRIPPED_ACACIA_WOOD ||
               material == Material.STRIPPED_DARK_OAK_WOOD ||
               material == Material.STRIPPED_MANGROVE_WOOD ||
               material == Material.STRIPPED_CHERRY_WOOD ||
               material == Material.STRIPPED_CRIMSON_HYPHAE ||
               material == Material.STRIPPED_WARPED_HYPHAE;
    }
    
    private boolean isLeafBlock(Material material) {
        return material == Material.OAK_LEAVES ||
               material == Material.SPRUCE_LEAVES ||
               material == Material.BIRCH_LEAVES ||
               material == Material.JUNGLE_LEAVES ||
               material == Material.ACACIA_LEAVES ||
               material == Material.DARK_OAK_LEAVES ||
               material == Material.MANGROVE_LEAVES ||
               material == Material.CHERRY_LEAVES ||
               material == Material.AZALEA_LEAVES ||
               material == Material.FLOWERING_AZALEA_LEAVES;
    }
}
