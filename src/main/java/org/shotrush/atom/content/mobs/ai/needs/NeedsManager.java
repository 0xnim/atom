package org.shotrush.atom.content.mobs.ai.needs;

import org.bukkit.entity.Animals;
import org.shotrush.atom.Atom;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NeedsManager {
    
    private final Atom plugin;
    private final Map<UUID, AnimalNeeds> needs;
    
    public NeedsManager(Atom plugin) {
        this.plugin = plugin;
        this.needs = new ConcurrentHashMap<>();
        startNeedsUpdateTask();
    }
    
    public AnimalNeeds getNeeds(Animals animal) {
        return needs.computeIfAbsent(animal.getUniqueId(), id -> new AnimalNeeds());
    }
    
    public void removeNeeds(UUID animalId) {
        needs.remove(animalId);
    }
    
    public void drainFromChasing(Animals animal) {
        AnimalNeeds animalNeeds = getNeeds(animal);
        animalNeeds.eat(-0.5);
    }
    
    private void startNeedsUpdateTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (AnimalNeeds need : needs.values()) {
                need.update();
            }
        }, 20L, 20L);
    }
}
