package org.shotrush.atom.core.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.ItemQuality;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BukkitRecipeBuilder {
    
    private final Plugin plugin;
    private final String recipeKey;
    private final boolean isShaped;
    
    private ItemStack result;
    private List<RecipeChoice> shapelessIngredients = new ArrayList<>();
    private String[] pattern;
    private Map<Character, RecipeChoice> shapedIngredients = new HashMap<>();
    
    private BukkitRecipeBuilder(Plugin plugin, String recipeKey, boolean isShaped) {
        this.plugin = plugin;
        this.recipeKey = recipeKey;
        this.isShaped = isShaped;
    }
    
    
    public static BukkitRecipeBuilder shapeless(String recipeKey) {
        return new BukkitRecipeBuilder(org.shotrush.atom.Atom.getInstance(), recipeKey, false);
    }
    
    
    public static BukkitRecipeBuilder shaped(String recipeKey) {
        return new BukkitRecipeBuilder(org.shotrush.atom.Atom.getInstance(), recipeKey, true);
    }
    
    
    public BukkitRecipeBuilder result(ItemStack result) {
        this.result = result;
        return this;
    }
    
    
    public BukkitRecipeBuilder result(String customItemId) {
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        this.result = item;
        return this;
    }
    
    
    public BukkitRecipeBuilder result(String customItemId, int count) {
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        item.setAmount(count);
        this.result = item;
        return this;
    }
    
    
    
    
    public BukkitRecipeBuilder ingredient(ItemStack customItem) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        shapelessIngredients.add(new CustomItemChoice(customItem));
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredient(String customItemId) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        shapelessIngredients.add(new CustomItemChoice(item));
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredient(String customItemId, ItemQuality quality) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        
        
        if (item.hasItemMeta()) {
            org.shotrush.atom.core.data.PersistentData.set(item.getItemMeta(), "quality", quality.name());
        }
        
        shapelessIngredients.add(new CustomItemChoice(item));
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredient(Material material) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        shapelessIngredients.add(new RecipeChoice.MaterialChoice(material));
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredient(Material material, int count) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        for (int i = 0; i < count; i++) {
            shapelessIngredients.add(new RecipeChoice.MaterialChoice(material));
        }
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredientChoice(ItemStack... choices) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        shapelessIngredients.add(new RecipeChoice.ExactChoice(List.of(choices)));
        return this;
    }
    
    
    public BukkitRecipeBuilder ingredientChoice(Material... choices) {
        if (isShaped) throw new IllegalStateException("Use shape() and setIngredient() for shaped recipes");
        shapelessIngredients.add(new RecipeChoice.MaterialChoice(choices));
        return this;
    }
    
    
    
    
    public BukkitRecipeBuilder shape(String... pattern) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        if (pattern.length > 3) throw new IllegalArgumentException("Pattern can have at most 3 rows");
        this.pattern = pattern;
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredient(char key, ItemStack customItem) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        shapedIngredients.put(key, new CustomItemChoice(customItem));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredient(char key, String customItemId) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        shapedIngredients.put(key, new CustomItemChoice(item));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredient(char key, String customItemId, ItemQuality quality) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        ItemStack item = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (item == null) {
            throw new IllegalArgumentException("Custom item not found: " + customItemId);
        }
        
        
        if (item.hasItemMeta()) {
            org.shotrush.atom.core.data.PersistentData.set(item.getItemMeta(), "quality", quality.name());
        }
        
        shapedIngredients.put(key, new CustomItemChoice(item));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredient(char key, Material material) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        shapedIngredients.put(key, new RecipeChoice.MaterialChoice(material));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredientChoice(char key, ItemStack... choices) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        shapedIngredients.put(key, new RecipeChoice.ExactChoice(List.of(choices)));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredientChoice(char key, Material... choices) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        shapedIngredients.put(key, new RecipeChoice.MaterialChoice(choices));
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredientChoice(char key, String customItemId, Material material) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        ItemStack customItem = Atom.getInstance().getItemRegistry().createItem(customItemId);
        if (customItem != null) {
            shapedIngredients.put(key, new RecipeChoice.ExactChoice(List.of(customItem, new ItemStack(material))));
        } else {
            shapedIngredients.put(key, new RecipeChoice.MaterialChoice(material));
        }
        return this;
    }
    
    
    public BukkitRecipeBuilder setIngredientChoice(char key, String customItemId1, String customItemId2) {
        if (!isShaped) throw new IllegalStateException("Use ingredient() for shapeless recipes");
        List<ItemStack> choices = new ArrayList<>();
        
        ItemStack item1 = Atom.getInstance().getItemRegistry().createItem(customItemId1);
        if (item1 != null) choices.add(item1);
        
        ItemStack item2 = Atom.getInstance().getItemRegistry().createItem(customItemId2);
        if (item2 != null) choices.add(item2);
        
        if (!choices.isEmpty()) {
            shapedIngredients.put(key, new RecipeChoice.ExactChoice(choices));
        }
        return this;
    }
    
    
    
    
    public void register() {
        if (result == null) {
            throw new IllegalStateException("Result item must be set");
        }
        
        NamespacedKey key = new NamespacedKey(plugin, recipeKey);
        
        if (isShaped) {
            if (pattern == null || pattern.length == 0) {
                throw new IllegalStateException("Pattern must be set for shaped recipes");
            }
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(pattern);
            
            for (Map.Entry<Character, RecipeChoice> entry : shapedIngredients.entrySet()) {
                recipe.setIngredient(entry.getKey(), entry.getValue());
            }
            
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Registered shaped recipe: " + recipeKey);
        } else {
            if (shapelessIngredients.isEmpty()) {
                throw new IllegalStateException("At least one ingredient must be added for shapeless recipes");
            }
            
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            
            for (RecipeChoice ingredient : shapelessIngredients) {
                recipe.addIngredient(ingredient);
            }
            
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Registered shapeless recipe: " + recipeKey);
        }
    }
    
    
    public org.bukkit.inventory.Recipe build() {
        if (result == null) {
            throw new IllegalStateException("Result item must be set");
        }
        
        NamespacedKey key = new NamespacedKey(plugin, recipeKey);
        
        if (isShaped) {
            if (pattern == null || pattern.length == 0) {
                throw new IllegalStateException("Pattern must be set for shaped recipes");
            }
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(pattern);
            
            for (Map.Entry<Character, RecipeChoice> entry : shapedIngredients.entrySet()) {
                recipe.setIngredient(entry.getKey(), entry.getValue());
            }
            
            return recipe;
        } else {
            if (shapelessIngredients.isEmpty()) {
                throw new IllegalStateException("At least one ingredient must be added for shapeless recipes");
            }
            
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            
            for (RecipeChoice ingredient : shapelessIngredients) {
                recipe.addIngredient(ingredient);
            }
            
            return recipe;
        }
    }
}
