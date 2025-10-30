package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.command.*;
import org.shotrush.atom.core.*;
import org.shotrush.atom.display.*;
import org.shotrush.atom.world.*;
import org.shotrush.atom.player.*;
import org.shotrush.atom.recipe.*;
import org.shotrush.atom.test.*;

public final class Atom extends JavaPlugin {
    @Getter private static Atom instance;
    @Getter private DataManager dataManager;
    @Getter private DisplayEntityManager displayManager;
    @Getter private org.shotrush.atom.model.ModelManager modelManager;
    @Getter private InteractionManager interactionManager;
    @Getter private WorldModificationManager worldManager;
    @Getter private PlayerCustomizationManager playerManager;
    @Getter private CustomRecipeManager recipeManager;
    @Getter private SchedulerManager schedulerManager;
    @Getter private SelfTestManager testManager;
    @Getter private org.shotrush.atom.listener.DisplayRotationCollisionListener rotationCollisionListener;
    @Getter private PerformanceMonitor performanceMonitor;
    @Getter private CustomItemManager itemManager;
    
    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        
        schedulerManager = new SchedulerManager(this);
        performanceMonitor = new PerformanceMonitor(this);
        dataManager = new DataManager(this);
        displayManager = new DisplayEntityManager(this);
        interactionManager = new InteractionManager(this);
        worldManager = new WorldModificationManager(this);
        playerManager = new PlayerCustomizationManager(this);
        itemManager = new CustomItemManager(this);
        recipeManager = new CustomRecipeManager(this);
        modelManager = new org.shotrush.atom.model.ModelManager(this);
        testManager = new SelfTestManager(this);
        rotationCollisionListener = new org.shotrush.atom.listener.DisplayRotationCollisionListener();
        
        worldManager.initialize();
        playerManager.initialize();
        recipeManager.initialize();
        displayManager.initialize();
        interactionManager.initialize();
        performanceMonitor.startMonitoring();
        modelManager.listModels();
        
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new ModelCommand());

        commandManager.getCommandCompletions().registerCompletion("models", c -> 
            modelManager.getModels().asMap().keySet());
        
        getServer().getPluginManager().registerEvents(new org.shotrush.atom.listener.ModelPlaceListener(), this);
        getServer().getPluginManager().registerEvents(new org.shotrush.atom.listener.DisplayCollisionListener(), this);
        getServer().getPluginManager().registerEvents(new org.shotrush.atom.listener.ModelLoadListener(), this);
        
        getCommand("atom").setExecutor(new AtomCommand());
        getCommand("atom").setTabCompleter(new AtomCommand());
        
        testManager.runTests();
        
        getLogger().info("Atom initialized in " + (System.currentTimeMillis() - start) + "ms");
    }
    
    @Override
    public void onDisable() {
        if (displayManager != null) displayManager.shutdown();
        if (worldManager != null) worldManager.shutdown();
        if (playerManager != null) playerManager.shutdown();
        if (recipeManager != null) recipeManager.shutdown();
        getLogger().info("Atom disabled");
    }
}
