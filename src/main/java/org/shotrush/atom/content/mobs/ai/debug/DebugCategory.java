package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.text.format.NamedTextColor;

public enum DebugCategory {
    GOALS("Goals", NamedTextColor.GOLD),
    NEEDS("Needs", NamedTextColor.GREEN),
    MEMORY("Memory", NamedTextColor.BLUE),
    COMBAT("Combat", NamedTextColor.RED),
    SOCIAL("Social", NamedTextColor.AQUA),
    ENVIRONMENTAL("Environmental", NamedTextColor.YELLOW);
    
    private final String displayName;
    private final NamedTextColor color;
    
    DebugCategory(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public NamedTextColor getColor() {
        return color;
    }
}
