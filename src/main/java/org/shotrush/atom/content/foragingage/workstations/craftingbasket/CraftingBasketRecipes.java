package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.recipe.Recipe;
import org.shotrush.atom.core.recipe.RecipeProvider;
import org.shotrush.atom.core.recipe.ShapelessRecipe;
import org.shotrush.atom.core.recipe.annotation.AutoRegister;
import org.shotrush.atom.core.util.ItemUtil;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 1)
public class CraftingBasketRecipes implements RecipeProvider {
    
    @Override
    public List<Recipe> getRecipes() {
        return Arrays.asList(
            createSpearRecipe()
        );
    }
    
    private Recipe createSpearRecipe() {
        ItemStack spear = Atom.getInstance().getItemRegistry().createItem("wood_spear");
        if (spear == null) {
            System.out.println("[DEBUG] wood_spear item not found in registry, using fallback");
            spear = ItemUtil.createItemWithCustomModel(Material.TRIDENT, "spear");
        }
        
        System.out.println("[DEBUG] Creating spear recipe with result: " + spear);
        
        return new ShapelessRecipe.Builder()
            .id("spear")
            .result(spear)
            .addIngredient("sharpened_flint")
            .addIngredient(Material.STICK)
            .addIngredient(Material.VINE)
            .build();
    }
}
