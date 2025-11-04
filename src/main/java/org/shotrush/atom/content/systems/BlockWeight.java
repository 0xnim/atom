package org.shotrush.atom.content.systems;

import org.bukkit.Material;

/**
 * Determines the fall speed of blocks based on their material type.
 * Heavier materials fall faster, lighter materials fall slower.
 */
public class BlockWeight {
    
    /**
     * Get the fall speed for a given material.
     * @param material The block material
     * @return The fall speed (higher = faster)
     */
    public static double getFallSpeed(Material material) {
        String name = material.name();
        if (name.contains("IRON") || name.contains("GOLD") || name.contains("NETHERITE")) {
            return 0.5;
        }
        if (name.contains("STONE") || name.contains("COBBLESTONE") || name.contains("BRICK") ||
            name.contains("DEEPSLATE") || name.contains("BLACKSTONE") || name.contains("OBSIDIAN") ||
            name.contains("CONCRETE") && !name.contains("POWDER")) {
            return 0.3;
        }
        if (material == Material.SAND || material == Material.RED_SAND ||
            material == Material.GRAVEL || name.contains("CONCRETE_POWDER") ||
            name.contains("DIRT") || name.contains("SOUL_SAND") || name.contains("SOUL_SOIL")) {
            return 0.2;
        }
        if (name.contains("GLASS") || name.contains("ICE")) {
            return 0.08;
        }
        if (name.contains("WOOD") || name.contains("LOG") || name.contains("PLANK") ||
            name.contains("BAMBOO")) {
            return 0.05;
        }
        if (name.contains("WOOL") || name.contains("LEAVES") || name.contains("HAY") ||
            name.contains("CARPET") || name.contains("SNOW") || name.contains("MUSHROOM") ||
            name.contains("SPONGE")) {
            return 0.02;
        }
        return 0.05;
    }
    
    /**
     * Check if a material is considered heavy.
     * @param material The block material
     * @return true if the material is heavy
     */
    public static boolean isHeavy(Material material) {
        return getFallSpeed(material) >= 0.15;
    }
    
    /**
     * Check if a material is considered light.
     * @param material The block material
     * @return true if the material is light
     */
    public static boolean isLight(Material material) {
        return getFallSpeed(material) <= 0.05;
    }
}
