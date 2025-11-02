package org.shotrush.atom.core.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface Recipe {
    
    String getId();
    
    ItemStack getResult();
    
    boolean matches(List<ItemStack> items);
    
    RecipeType getType();
    
    enum RecipeType {
        SHAPED,
        SHAPELESS
    }
}
