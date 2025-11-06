package org.shotrush.atom.core.api.combat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TemperatureEffectsAPI {
    
    public static void applyHeatDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature >= 100) {
            player.setFireTicks(40);
            spawnSweatParticles(player, 3);
        } else if (temperature >= 50) {
            player.setFireTicks(20);
            spawnSweatParticles(player, 1);
        }
    }
    
    public static void applyColdDamage(Player player, double temperature, boolean hasProtection) {
        if (hasProtection) return;
        
        if (temperature <= -20) {
            player.damage(2.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            spawnColdParticles(player, 3);
        } else if (temperature <= -10) {
            player.damage(1.0);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnColdParticles(player, 1);
        }
    }
    
    public static void applyBodyTemperatureEffects(Player player, double bodyTemp) {
        if (bodyTemp >= 41.0) {
            player.damage(0.5);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA, 100, 0, false, false
            ));
            spawnSweatParticles(player, 5);
        } else if (bodyTemp >= 39.0) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnSweatParticles(player, 2);
        }
        
        else if (bodyTemp >= 38.0) {
            spawnSweatParticles(player, 1);
        }
        
        else if (bodyTemp <= 33.0) {
            player.damage(0.5);
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 2, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 10, 140));
            spawnColdParticles(player, 5);
        } else if (bodyTemp <= 34.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 1, false, false
            ));
            player.setFreezeTicks(Math.min(player.getFreezeTicks() + 5, 140));
            spawnColdParticles(player, 3);
        } else if (bodyTemp <= 35.5) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, 40, 0, false, false
            ));
            spawnColdParticles(player, 1);
        }
    }
    
    public static String getTemperatureColor(double temperature, double normalTemp) {
        double deviation = Math.abs(temperature - normalTemp);
        
        if (deviation >= 5.0) return "§c";
        if (deviation >= 2.5) return "§6";
        if (deviation >= 1.5) return "§e";
        return "§a";
    }
    
    public static String getBodyTempColor(double temp) {
        if (temp >= 40.0 || temp <= 32.0) return "§c";
        if (temp >= 38.5 || temp <= 34.0) return "§6";
        if (temp >= 38.0 || temp <= 35.5) return "§e";
        return "§a";
    }
    
    private static void spawnSweatParticles(Player player, int count) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        player.getWorld().spawnParticle(
            Particle.DRIPPING_WATER,
            loc,
            count,
            0.3, 0.3, 0.3,
            0.05
        );
    }
    
    private static void spawnColdParticles(Player player, int count) {
        
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection();
        
        
        loc.add(direction.multiply(0.4));
        
        
        player.getWorld().spawnParticle(
            Particle.CLOUD,
            loc,
            count,
            0.15, 0.08, 0.15,  
            0.01  
        );
    }
}
