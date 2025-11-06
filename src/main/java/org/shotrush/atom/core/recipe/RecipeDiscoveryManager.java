package org.shotrush.atom.core.recipe;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.reflections.Reflections;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.recipe.annotation.AllowedRecipe;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.shotrush.atom.core.api.annotation.RegisterSystem;


@RegisterSystem(
    id = "recipe_discovery_manager",
    priority = 10,
    toggleable = false,
    description = "Manages vanilla recipe blocking and discovery"
)
public class RecipeDiscoveryManager implements Listener {
    
    private static final Set<NamespacedKey> allowedRecipes = new HashSet<>();
    private static RecipeDiscoveryManager instance;
    
    public RecipeDiscoveryManager(Plugin plugin) {
        instance = this;
        scanForAllowedRecipes();
        
        Atom.getInstance().getLogger().info("Recipe blocking enabled - only allowed recipes can be crafted");
    }
    
    
    private void scanForAllowedRecipes() {
        try {
            Reflections reflections = new Reflections("org.shotrush.atom");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(AllowedRecipe.class);
            
            for (Class<?> clazz : annotatedClasses) {
                AllowedRecipe annotation = clazz.getAnnotation(AllowedRecipe.class);
                if (annotation != null) {
                    for (String recipeKey : annotation.recipes()) {
                        NamespacedKey key = new NamespacedKey("minecraft", recipeKey);
                        allowedRecipes.add(key);
                        Atom.getInstance().getLogger().info("Allowed vanilla recipe: " + recipeKey);
                    }
                }
            }
            
            Atom.getInstance().getLogger().info("Loaded " + allowedRecipes.size() + " allowed vanilla recipes");
        } catch (Exception e) {
            Atom.getInstance().getLogger().warning("Failed to scan for allowed recipes: " + e.getMessage());
        }
    }
    
    
    public static void allowRecipe(NamespacedKey key) {
        allowedRecipes.add(key);
    }
    
    
    public static void disallowRecipe(NamespacedKey key) {
        allowedRecipes.remove(key);
    }
    
    
    public static boolean isRecipeAllowed(NamespacedKey key) {
        
        if (!key.getNamespace().equals("minecraft")) {
            return true;
        }
        
        return allowedRecipes.contains(key);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        
        undiscoverAllVanillaRecipes(player);
    }
    
    
    public static void undiscoverAllVanillaRecipes(Player player) {
        
        Set<NamespacedKey> discoveredRecipes = player.getDiscoveredRecipes();
        
        
        Set<NamespacedKey> toUndiscover = discoveredRecipes.stream()
                .filter(key -> key.getNamespace().equals("minecraft"))
                .filter(key -> !isRecipeAllowed(key))
                .collect(Collectors.toSet());
        
        
        if (!toUndiscover.isEmpty()) {
            player.undiscoverRecipes(toUndiscover);
        }
    }
    
    
    public static void undiscoverAllVanillaRecipesForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            undiscoverAllVanillaRecipes(player);
        }
    }
    
    
    public static void discoverAllowedRecipes(Player player) {
        if (!allowedRecipes.isEmpty()) {
            player.discoverRecipes(allowedRecipes);
        }
    }
    
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        
        
        if (!(recipe instanceof org.bukkit.Keyed keyedRecipe)) {
            return;
        }
        
        NamespacedKey key = keyedRecipe.getKey();
        
        
        if (key.getNamespace().equals("minecraft") && !isRecipeAllowed(key)) {
            CraftingInventory inventory = event.getInventory();
            inventory.setResult(null);
            
            
            if (event.getView().getPlayer() instanceof Player player) {
                
            }
        }
    }
    
    
    public static Set<NamespacedKey> getAllowedRecipes() {
        return new HashSet<>(allowedRecipes);
    }
}
