package org.shotrush.atom.core.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;

import java.util.*;

public class ShapelessRecipe implements Recipe {
    
    private final String id;
    private final ItemStack result;
    private final List<RecipeIngredient> ingredients;
    
    public ShapelessRecipe(String id, ItemStack result, List<RecipeIngredient> ingredients) {
        this.id = id;
        this.result = result;
        this.ingredients = ingredients;
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
        if (items.size() != ingredients.size()) {
            return false;
        }
        
        List<ItemStack> itemsCopy = new ArrayList<>(items);
        List<RecipeIngredient> ingredientsCopy = new ArrayList<>(ingredients);
        
        for (RecipeIngredient ingredient : ingredientsCopy) {
            boolean found = false;
            
            for (int i = 0; i < itemsCopy.size(); i++) {
                ItemStack item = itemsCopy.get(i);
                if (ingredient.matches(item)) {
                    itemsCopy.remove(i);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return false;
            }
        }
        
        return itemsCopy.isEmpty();
    }
    
    @Override
    public RecipeType getType() {
        return RecipeType.SHAPELESS;
    }
    
    public static class Builder {
        private String id;
        private ItemStack result;
        private List<RecipeIngredient> ingredients = new ArrayList<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }
        
        public Builder addIngredient(Material material) {
            ingredients.add(new RecipeIngredient(material));
            return this;
        }
        
        public Builder addIngredient(String customItemId) {
            ingredients.add(new RecipeIngredient(customItemId));
            return this;
        }
        
        public Builder addIngredient(RecipeIngredient ingredient) {
            ingredients.add(ingredient);
            return this;
        }
        
        public ShapelessRecipe build() {
            if (id == null || result == null || ingredients.isEmpty()) {
                throw new IllegalStateException("Recipe must have id, result, and at least one ingredient");
            }
            return new ShapelessRecipe(id, result, ingredients);
        }
    }
}
