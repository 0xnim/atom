package org.shotrush.atom.core.blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.shotrush.atom.Atom;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BarrierBreakHandler implements Listener {
    
    private final Atom plugin;
    private final CustomBlockManager blockManager;
    private final Map<Location, UUID> breakingBlocks = new HashMap<>();
    
    public BarrierBreakHandler(Atom plugin, CustomBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        setupProtocolListener();
    }
    
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getBlock().getType() != Material.BARRIER) return;
        
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();
        Player player = event.getPlayer();
        
        CustomBlock customBlock = blockManager.getBlockAt(blockLoc);
        if (customBlock == null) return;
        
        if (customBlock.getDisplayUUID() == null) {
            return;
        }
        
        if (breakingBlocks.containsKey(blockLoc)) {
            return;
        }
        
        BlockType blockType = blockManager.getRegistry().getBlockType(customBlock.getBlockType());
        if (blockType == null) return;
        
        double baseBreakTime = blockType.getBreakTime();
        
        ItemStack tool = event.getItemInHand();
        
        double breakSpeed = 1.0;
        if (player.getAttribute(Attribute.BLOCK_BREAK_SPEED) != null) {
            breakSpeed = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_BREAK_SPEED)).getValue();
        }
        
        int efficiencyLevel = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        breakSpeed *= (1.0 + efficiencyLevel * 0.3);
        
        long breakTicks = Math.max(1, (long) (baseBreakTime / breakSpeed));
        
        UUID taskId = UUID.randomUUID();
        breakingBlocks.put(blockLoc, taskId);
        
        final int maxStages = 10;
        final long ticksPerStage = Math.max(1, breakTicks / maxStages);
        
        class BreakTask {
            int stage = 0;
            
            void run() {
                if (!breakingBlocks.containsKey(blockLoc) || !breakingBlocks.get(blockLoc).equals(taskId)) {
                    return;
                }
                
                if (block.getType() != Material.BARRIER) {
                    breakingBlocks.remove(blockLoc);
                    return;
                }
                
                if (stage < maxStages) {
                    sendBlockBreakAnimation(player, blockLoc, stage);
                    stage++;
                    
                    Bukkit.getRegionScheduler().runDelayed(plugin, blockLoc, task -> run(), ticksPerStage);
                } else {
                    breakingBlocks.remove(blockLoc);
                    sendBlockBreakAnimation(player, blockLoc, -1);
                    breakCustomBlock(block, player);
                }
            }
        }
        
        new BreakTask().run();
    }
    
    private void sendBlockBreakAnimation(Player player, Location location, int stage) {
        try {
            int breakerId = player.getEntityId();
            int animationStage = stage < 0 ? 255 : Math.min(stage, 9);
            
            java.util.Collection<Player> nearbyPlayers = location.getNearbyPlayers(64);
            if (!nearbyPlayers.contains(player)) {
                nearbyPlayers = new java.util.ArrayList<>(nearbyPlayers);
                nearbyPlayers.add(player);
            }
            
            for (Player nearbyPlayer : nearbyPlayers) {
                PacketContainer packet = ProtocolLibrary.getProtocolManager()
                    .createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                
                packet.getIntegers()
                    .write(0, breakerId)
                    .write(1, animationStage);
                
                packet.getBlockPositionModifier()
                    .write(0, new com.comphenix.protocol.wrappers.BlockPosition(
                        location.getBlockX(), 
                        location.getBlockY(), 
                        location.getBlockZ()
                    ));
                
                ProtocolLibrary.getProtocolManager().sendServerPacket(nearbyPlayer, packet);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send block break animation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void breakCustomBlock(Block block, Player player) {
        Location brokenLoc = block.getLocation();
        CustomBlock customBlock = blockManager.getBlockAt(brokenLoc);
        
        if (customBlock != null) {
            BlockType blockType = blockManager.getRegistry().getBlockType(customBlock.getBlockType());
            if (blockType != null) {
                ItemStack dropItem = blockType.getDropItem();
                if (dropItem != null) {
                    brokenLoc.getWorld().dropItemNaturally(brokenLoc, dropItem);
                }
            }
            
            block.setType(Material.AIR);
            blockManager.removeBlock(customBlock);
            player.sendMessage("§aCustom block removed");
        }
    }
    
    private void setupProtocolListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    PacketContainer packet = event.getPacket();
                    EnumWrappers.PlayerDigType digType = packet.getPlayerDigTypes().read(0);
                    
                    if (digType == EnumWrappers.PlayerDigType.ABORT_DESTROY_BLOCK || 
                        digType == EnumWrappers.PlayerDigType.STOP_DESTROY_BLOCK) {
                        
                        com.comphenix.protocol.wrappers.BlockPosition pos = packet.getBlockPositionModifier().read(0);
                        Location loc = new Location(
                            event.getPlayer().getWorld(),
                            pos.getX(),
                            pos.getY(),
                            pos.getZ()
                        );

                        breakingBlocks.remove(loc);
                    }
                }
            }
        );
    }
}
