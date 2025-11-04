package org.shotrush.atom.content.mobs.ai.memory;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AnimalMemory {
    
    public enum PlayerThreatLevel {
        FRIENDLY,
        NEUTRAL,
        CAUTIOUS,
        HOSTILE,
        MORTAL_ENEMY
    }
    
    public Optional<Location> getRecentThreat() {
        return Optional.empty();
    }
    
    public List<Location> getNearbyDangerZones(Location location, double radius) {
        return Collections.emptyList();
    }
    
    public PlayerThreatLevel getThreatLevel(Player player) {
        return PlayerThreatLevel.NEUTRAL;
    }
}
