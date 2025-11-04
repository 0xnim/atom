package org.shotrush.atom.content.mobs.ai.debug;

import org.bukkit.entity.Mob;

public final class DebugManager {
    
    private static DebugLevel globalLevel = DebugLevel.OFF;
    
    public static void setGlobalLevel(DebugLevel level) {
        globalLevel = level;
    }
    
    public static void setCategoryLevel(DebugCategory category, DebugLevel level) {
    }
    
    public static void log(String message) {
        if (globalLevel != DebugLevel.OFF) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    public static void resetPerformanceMetrics() {
    }
}
