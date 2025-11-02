package org.shotrush.atom.core.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapedRecipe implements Recipe {
    
    private final String id;
    private final ItemStack result;
    private final String[] pattern;
    private final Map<Character, RecipeIngredient> ingredients;
    private final int width;
    private final int height;
    
    public ShapedRecipe(String id, ItemStack result, String[] pattern, Map<Character, RecipeIngredient> ingredients) {
        this.id = id;
        this.result = result;
        this.pattern = pattern;
        this.ingredients = ingredients;
        this.height = pattern.length;
        this.width = pattern.length > 0 ? pattern[0].length() : 0;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public ItemStack getResult() {
        return result.clone();
    }
    
    @Override
    public boolean matches(List<ItemStack> items) {
        int totalSlots = width * height;
        if (items.size() != totalSlots) {
            return false;
        }
        
        for (int i = 0; i < totalSlots; i++) {
            int row = i / width;
            int col = i % width;
            
            if (row >= height || col >= width) {
                return false;
            }
            
            char key = pattern[row].charAt(col);
            ItemStack item = items.get(i);
            
            if (key == ' ') {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    return false;
                }
            } else {
                RecipeIngredient ingredient = ingredients.get(key);
                if (ingredient == null || !ingredient.matches(item)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public RecipeType getType() {
        return RecipeType.SHAPED;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public static class Builder {
        private String id;
        private ItemStack result;
        private String[] pattern;
        private Map<Character, RecipeIngredient> ingredients = new HashMap<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }
        
        public Builder pattern(String... pattern) {
            this.pattern = pattern;
            return this;
        }
        
        public Builder setIngredient(char key, RecipeIngredient ingredient) {
            ingredients.put(key, ingredient);
            return this;
        }
        
        public ShapedRecipe build() {
            if (id == null || result == null || pattern == null || pattern.length == 0) {
                throw new IllegalStateException("Recipe must have id, result, and pattern");
            }
            
            int width = pattern[0].length();
            for (String row : pattern) {
                if (row.length() != width) {
                    throw new IllegalStateException("All pattern rows must have the same width");
                }
            }
            
            return new ShapedRecipe(id, result, pattern, ingredients);
        }
    }
}
