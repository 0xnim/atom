package org.shotrush.atom.core.recipe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;


public class CustomItemChoice extends RecipeChoice.ExactChoice {
    
    private final ItemStack template;
    
    public CustomItemChoice(@NotNull ItemStack template) {
        super(removeHeatFromItem(template.clone()));
        this.template = removeHeatFromItem(template.clone());
    }
    
    @Override
    public @NotNull ItemStack getItemStack() {
        return template.clone();
    }
    
    @Override
    public @NotNull ExactChoice clone() {
        return new CustomItemChoice(template);
    }
    
    @Override
    public boolean test(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != template.getType()) return false;
        
        
        ItemStack testItem = removeHeatFromItem(itemStack.clone());
        ItemStack templateItem = template.clone();
        
        
        return testItem.isSimilar(templateItem);
    }
    
    private static ItemStack removeHeatFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        
        org.shotrush.atom.core.data.PersistentData.remove(meta, "item_heat");
        item.setItemMeta(meta);
        return item;
    }
}
