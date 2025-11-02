package org.shotrush.atom.core.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;

import java.util.List;

public class RecipeIngredient {
    
    private final Material material;
    private final String customItemId;
    private final String customModelData;
    
    public RecipeIngredient(Material material) {
        this.material = material;
        this.customItemId = null;
        this.customModelData = null;
    }
    
    public RecipeIngredient(String customItemId) {
        this.material = null;
        this.customItemId = customItemId;
        this.customModelData = null;
    }
    
    public RecipeIngredient(Material material, String customModelData) {
        this.material = material;
        this.customItemId = null;
        this.customModelData = customModelData;
    }
    
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        if (customItemId != null) {
            CustomItem customItem = Atom.getInstance().getItemRegistry().getItem(customItemId);
            return customItem != null && customItem.isCustomItem(item);
        }
        
        if (material != null && item.getType() != material) {
            return false;
        }
        
        if (customModelData != null) {
            if (!item.hasItemMeta()) {
                return false;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) {
                return false;
            }
            List<String> strings = meta.getCustomModelDataComponent().getStrings();
            return strings.contains(customModelData);
        }
        
        return material != null;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getCustomItemId() {
        return customItemId;
    }
    
    public String getCustomModelData() {
        return customModelData;
    }
}
