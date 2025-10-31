package org.shotrush.atom;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.shotrush.atom.core.age.AgeManager;
import org.shotrush.atom.cog.CogListener;
import org.shotrush.atom.commands.AgeCommand;
import org.shotrush.atom.commands.CogCommand;
import org.shotrush.atom.core.storage.DataStorage;

public final class Atom extends JavaPlugin {

    @Getter
    private static Atom instance;

    @Getter
    private DataStorage dataStorage;

    @Getter
    private AgeManager ageManager;

    @Getter
    private CogListener cogListener;

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        dataStorage = new DataStorage(this);
        ageManager = new AgeManager(this, dataStorage);
        cogListener = new CogListener(this);

        getServer().getPluginManager().registerEvents(cogListener, this);

        setupCommands();

        getLogger().info("Atom plugin has been enabled!");
    }

    private void setupCommands() {
        commandManager = new PaperCommandManager(this);

        commandManager.getCommandCompletions().registerCompletion("ages", context ->
                ageManager.getAllAges().stream().map(age -> age.getId()).toList()
        );

        commandManager.registerCommand(new AgeCommand(this));
        commandManager.registerCommand(new CogCommand());
    }

    @Override
    public void onDisable() {
        getLogger().info("Atom plugin has been disabled!");
    }
}
