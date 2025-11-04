package org.shotrush.atom.content.mobs.ai.memory;

import org.bukkit.entity.Animals;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryManager {
    
    private final Map<UUID, AnimalMemory> memories = new ConcurrentHashMap<>();
    
    public AnimalMemory getMemory(Animals animal) {
        return memories.computeIfAbsent(animal.getUniqueId(), k -> new AnimalMemory());
    }
}
