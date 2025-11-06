package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.items.ItemQuality;
import org.shotrush.atom.core.recipe.BukkitRecipeBuilder;

import org.shotrush.atom.core.api.annotation.RegisterSystem;


@RegisterSystem(
    id = "crafting_basket_recipes",
    priority = 1,
    toggleable = false,
    description = "Registers crafting basket recipes"
)
public class CraftingBasketRecipes {
    
    public CraftingBasketRecipes(Plugin plugin) {
        
        registerBukkitRecipes();
    }
    
    
    private void registerBukkitRecipes() {
        registerSpearBukkitRecipe();
        registerPressureFlakerBukkitRecipe();
        registerKnifeBukkitRecipe();
        registerWaterskinBukkitRecipe();
    }
    
    private void registerSpearBukkitRecipe() {
        
        
        
        
        BukkitRecipeBuilder.shaped("spear")
            .result("wood_spear")
            .shape(
                "  F",
                " SV",
                "S  "
            )
            .setIngredient('F', "sharpened_flint")
            .setIngredient('S', Material.STICK)
            .setIngredientChoice('V', "stabilized_leather", Material.VINE)  
            .register();
    }
    
    private void registerPressureFlakerBukkitRecipe() {
        BukkitRecipeBuilder.shapeless("pressure_flaker")
            .result("pressure_flaker")
            .ingredient("bone")
            .register();
    }
    
    private void registerKnifeBukkitRecipe() {
        
        
        
        BukkitRecipeBuilder.shaped("knife")
            .result("knife")
            .shape(
                "  F",
                " SL"
            )
            .setIngredient('F', "sharpened_flint", ItemQuality.HIGH)
            .setIngredient('S', Material.STICK)
            .setIngredient('L', "stabilized_leather")
            .register();
    }
    
    private void registerWaterskinBukkitRecipe() {
        
        
        
        BukkitRecipeBuilder.shaped("waterskin")
            .result("waterskin")
            .shape(
                "L L",
                " L "
            )
            .setIngredient('L', "stabilized_leather")
            .register();
    }
}
