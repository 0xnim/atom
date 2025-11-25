package org.shotrush.atom.content.systems;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.shotrush.atom.core.api.annotation.RegisterSystem;

@RegisterSystem(
        id = "combat_listener",
        priority = 5,
        toggleable = true,
        description = "Handles loss of hunger in combat"
)
public class CombatListener implements Listener {
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            int currentFoodLevel = player.getFoodLevel();
            if (currentFoodLevel > 0) {
                player.setFoodLevel(currentFoodLevel - 1);
            }
        }
    }
}
