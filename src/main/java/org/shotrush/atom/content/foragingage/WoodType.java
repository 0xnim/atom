package org.shotrush.atom.content.foragingage;

import org.bukkit.Material;

public enum WoodType {
    OAK("Oak", Material.OAK_LOG, Material.STRIPPED_OAK_LOG, Material.SANDSTONE_WALL, Material.OAK_FENCE),
    SPRUCE("Spruce", Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG, Material.COBBLESTONE_WALL, Material.SPRUCE_FENCE),
    BIRCH("Birch", Material.BIRCH_LOG, Material.STRIPPED_BIRCH_LOG, Material.SANDSTONE_WALL, Material.BIRCH_FENCE),
    JUNGLE("Jungle", Material.JUNGLE_LOG, Material.STRIPPED_JUNGLE_LOG, Material.MOSSY_COBBLESTONE_WALL, Material.JUNGLE_FENCE),
    ACACIA("Acacia", Material.ACACIA_LOG, Material.STRIPPED_ACACIA_LOG, Material.RED_SANDSTONE_WALL, Material.ACACIA_FENCE),
    DARK_OAK("Dark Oak", Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG, Material.COBBLESTONE_WALL, Material.DARK_OAK_FENCE),
    MANGROVE("Mangrove", Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG, Material.MUD_BRICK_WALL, Material.MANGROVE_FENCE),
    CHERRY("Cherry", Material.CHERRY_LOG, Material.STRIPPED_CHERRY_LOG, Material.SANDSTONE_WALL, Material.CHERRY_FENCE),
    CRIMSON("Crimson", Material.CRIMSON_STEM, Material.STRIPPED_CRIMSON_STEM, Material.NETHER_BRICK_WALL, Material.CRIMSON_FENCE),
    WARPED("Warped", Material.WARPED_STEM, Material.STRIPPED_WARPED_STEM, Material.NETHER_BRICK_WALL, Material.WARPED_FENCE);
    
    private final String displayName;
    private final Material log;
    private final Material strippedLog;
    private final Material wall;
    private final Material fence;
    
    WoodType(String displayName, Material log, Material strippedLog, Material wall, Material fence) {
        this.displayName = displayName;
        this.log = log;
        this.strippedLog = strippedLog;
        this.wall = wall;
        this.fence = fence;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Material getLog() {
        return log;
    }
    
    public Material getStrippedLog() {
        return strippedLog;
    }
    
    public Material getWall() {
        return wall;
    }
    
    public Material getFence() {
        return fence;
    }
    
    public static WoodType fromLog(Material material) {
        for (WoodType type : values()) {
            if (type.log == material || type.strippedLog == material) {
                return type;
            }
        }
        return null;
    }
}
