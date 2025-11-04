package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class PerformanceMonitor {
    
    public static void displayStats(Player player) {
        player.sendMessage(Component.text("=== Performance Stats ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Performance monitoring is disabled.", NamedTextColor.GRAY));
    }
    
    public static void reset() {
    }
}
