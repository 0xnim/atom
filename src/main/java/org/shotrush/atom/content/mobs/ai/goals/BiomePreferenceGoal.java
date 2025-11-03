package org.shotrush.atom.content.mobs.ai.goals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.content.mobs.ai.environment.EnvironmentalContext;

import java.util.EnumSet;

public class BiomePreferenceGoal implements Goal<Mob> {
    
    private final GoalKey<Mob> key;
    private final Mob mob;
    private final Plugin plugin;
    private Location targetLocation;
    private int repathTimer;
    private static final int REPATH_INTERVAL = 60;
    private static final int SEARCH_RADIUS = 32;
    private static final double MOVE_SPEED = 1.0;
    
    public BiomePreferenceGoal(Mob mob, Plugin plugin) {
        this.mob = mob;
        this.plugin = plugin;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "biome_preference"));
        this.repathTimer = 0;
    }
    
    @Override
    public boolean shouldActivate() {
        if (!(mob instanceof Animals animal)) return false;
        if (!mob.isValid() || mob.isDead()) return false;
        
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        
        Biome currentBiome = loc.getBlock().getBiome();
        EnvironmentalContext.BiomeType preferredBiome = getPreferredBiome(mob.getType());
        EnvironmentalContext.BiomePreference preference = EnvironmentalContext.getBiomePreference(currentBiome, preferredBiome);
        
        return preference == EnvironmentalContext.BiomePreference.UNCOMFORTABLE || 
               preference == EnvironmentalContext.BiomePreference.HOSTILE;
    }
    
    @Override
    public boolean shouldStayActive() {
        if (!mob.isValid() || mob.isDead()) return false;
        if (mob.getTarget() != null) return false;
        
        Location loc = mob.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        
        Biome currentBiome = loc.getBlock().getBiome();
        EnvironmentalContext.BiomeType preferredBiome = getPreferredBiome(mob.getType());
        EnvironmentalContext.BiomePreference preference = EnvironmentalContext.getBiomePreference(currentBiome, preferredBiome);
        
        return preference == EnvironmentalContext.BiomePreference.UNCOMFORTABLE || 
               preference == EnvironmentalContext.BiomePreference.HOSTILE;
    }
    
    @Override
    public void start() {
        targetLocation = null;
        repathTimer = 0;
        findPreferredBiome();
    }
    
    @Override
    public void stop() {
        targetLocation = null;
        mob.getPathfinder().stopPathfinding();
    }
    
    @Override
    public void tick() {
        repathTimer++;
        
        if (targetLocation == null || repathTimer >= REPATH_INTERVAL) {
            repathTimer = 0;
            findPreferredBiome();
        }
        
        if (targetLocation != null && targetLocation.getWorld() != null) {
            mob.getPathfinder().moveTo(targetLocation, MOVE_SPEED);
        }
    }
    
    private void findPreferredBiome() {
        Location mobLoc = mob.getLocation();
        if (mobLoc == null || mobLoc.getWorld() == null) {
            targetLocation = null;
            return;
        }
        
        EnvironmentalContext.BiomeType preferredBiome = getPreferredBiome(mob.getType());
        Location bestLocation = null;
        EnvironmentalContext.BiomePreference bestPreference = EnvironmentalContext.BiomePreference.HOSTILE;
        
        for (int angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int x = (int) (Math.cos(radians) * SEARCH_RADIUS);
            int z = (int) (Math.sin(radians) * SEARCH_RADIUS);
            
            Location checkLoc = mobLoc.clone().add(x, 0, z);
            checkLoc.setY(checkLoc.getWorld().getHighestBlockYAt(checkLoc));
            
            Biome biome = checkLoc.getBlock().getBiome();
            EnvironmentalContext.BiomePreference preference = EnvironmentalContext.getBiomePreference(biome, preferredBiome);
            
            if (preference == EnvironmentalContext.BiomePreference.PREFERRED) {
                bestLocation = checkLoc;
                break;
            }
            
            if (preference == EnvironmentalContext.BiomePreference.NEUTRAL && 
                (bestPreference == EnvironmentalContext.BiomePreference.UNCOMFORTABLE || 
                 bestPreference == EnvironmentalContext.BiomePreference.HOSTILE)) {
                bestLocation = checkLoc;
                bestPreference = preference;
            }
        }
        
        targetLocation = bestLocation;
    }
    
    private EnvironmentalContext.BiomeType getPreferredBiome(EntityType type) {
        return switch (type) {
            case COW, SHEEP, HORSE, DONKEY, MULE -> EnvironmentalContext.BiomeType.PLAINS;
            case PIG, CHICKEN, RABBIT -> EnvironmentalContext.BiomeType.FOREST;
            case WOLF, FOX -> EnvironmentalContext.BiomeType.TAIGA;
            case LLAMA, GOAT -> EnvironmentalContext.BiomeType.MOUNTAIN;
            case POLAR_BEAR -> EnvironmentalContext.BiomeType.TUNDRA;
            case CAMEL -> EnvironmentalContext.BiomeType.DESERT;
            case CAT, OCELOT -> EnvironmentalContext.BiomeType.FOREST;
            case PANDA -> EnvironmentalContext.BiomeType.FOREST;
            case SNIFFER -> EnvironmentalContext.BiomeType.PLAINS;
            case ARMADILLO -> EnvironmentalContext.BiomeType.SAVANNA;
            case TURTLE -> EnvironmentalContext.BiomeType.AQUATIC;
            case AXOLOTL, FROG -> EnvironmentalContext.BiomeType.SWAMP;
            case STRIDER -> EnvironmentalContext.BiomeType.NETHER;
            default -> EnvironmentalContext.BiomeType.PLAINS;
        };
    }
    
    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }
    
    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
