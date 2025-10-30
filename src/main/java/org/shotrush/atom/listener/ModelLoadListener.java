package org.shotrush.atom.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shotrush.atom.Atom;

public class ModelLoadListener implements Listener {
    
    private static boolean modelsLoaded = false;
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!modelsLoaded) {
            modelsLoaded = true;
            Atom.getInstance().getModelManager().loadPlacedModelsDelayed();
        }
    }
}
