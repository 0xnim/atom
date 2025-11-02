package org.shotrush.atom.content.systems;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.potion.PotionEffectType;
import org.shotrush.atom.core.systems.annotation.AutoRegisterSystem;

@AutoRegisterSystem(priority = 4)
public class PlayerTemperatureSystem implements Listener {
    
    @Getter
    private static PlayerTemperatureSystem instance;
    
    private final Plugin plugin;
    private final Map<UUID, Double> playerTemperatures = new HashMap<>();
    
    private static final double NORMAL_TEMP = 37.0;
    private static final double MAX_TEMP = 42.0;
    private static final double MIN_TEMP = 32.0;
    private static final double COMFORTABLE_RANGE = 2.0;
    private static final double WARNING_RANGE = 3.0;
    
    public PlayerTemperatureSystem(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.shotrush.atom.Atom.getInstance().getDataStorage().getPlayerData(playerId);
        double savedTemp = config.getDouble("temperature.body", NORMAL_TEMP);
        
        playerTemperatures.put(playerId, savedTemp);
        startTemperatureTickForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        double temp = playerTemperatures.getOrDefault(playerId, NORMAL_TEMP);
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.shotrush.atom.Atom.getInstance().getDataStorage().getPlayerData(playerId);
        config.set("temperature.body", temp);
        org.shotrush.atom.Atom.getInstance().getDataStorage().savePlayerData(playerId, config);
        
        playerTemperatures.remove(playerId);
    }
    
    private void startTemperatureTickForPlayer(Player player) {
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }
            updatePlayerTemperature(player);
        }, null, 1L, 20L);
    }
    
    private void updatePlayerTemperature(Player player) {
        UUID playerId = player.getUniqueId();
        double currentTemp = playerTemperatures.getOrDefault(playerId, NORMAL_TEMP);
        org.bukkit.Location loc = player.getLocation();
        
        double envChange = org.shotrush.atom.core.api.EnvironmentalFactorAPI
            .calculateEnvironmentalTemperatureChange(player, loc, 0.01);
        
        double armorInsulation = org.shotrush.atom.core.api.ArmorProtectionAPI.getInsulationValue(player);
        envChange *= (1.0 - armorInsulation * 0.7);
        
        double naturalRegulation = 0.0;
        double tempDiff = currentTemp - NORMAL_TEMP;
        double deviation = Math.abs(tempDiff);
        
        if (deviation > COMFORTABLE_RANGE) {
            naturalRegulation = -tempDiff * 0.12;
            
            double regulationCost = deviation * 0.015;
            
            if (Math.random() < regulationCost) {
                int currentHunger = player.getFoodLevel();
                if (currentHunger > 0) {
                    player.setFoodLevel(Math.max(0, currentHunger - 1));
                }
                
                ThirstSystem thirstSystem = ThirstSystem.getInstance();
                if (thirstSystem != null) {
                    int currentThirst = thirstSystem.getThirst(player);
                    if (currentThirst > 0) {
                        thirstSystem.addThirst(player, -1);
                    }
                }
            }
        }
        
        double totalChange = envChange + naturalRegulation;
        totalChange = Math.max(-0.2, Math.min(0.2, totalChange));
        
        double newTemp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, currentTemp + totalChange));
        
        playerTemperatures.put(playerId, newTemp);
        
        applyTemperatureEffects(player, newTemp);
    }
    
    private void applyTemperatureEffects(Player player, double temp) {
        org.shotrush.atom.core.api.TemperatureEffectsAPI.applyBodyTemperatureEffects(player, temp);
        
        double deviation = Math.abs(temp - NORMAL_TEMP);
        if (deviation > WARNING_RANGE) {
            String tempDisplay = String.format("%.1f°C", temp);
            String color = org.shotrush.atom.core.api.TemperatureEffectsAPI.getBodyTempColor(temp);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§7Body Temperature: " + color + tempDisplay));
        }
    }
    
    public double getPlayerTemperature(Player player) {
        return playerTemperatures.getOrDefault(player.getUniqueId(), NORMAL_TEMP);
    }
}
