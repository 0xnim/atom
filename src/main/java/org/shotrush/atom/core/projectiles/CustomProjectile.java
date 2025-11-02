package org.shotrush.atom.core.projectiles;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;

import java.util.UUID;
import java.util.function.Consumer;

public class CustomProjectile {
    
    private static final double DEFAULT_GRAVITY = 0.03;
    private static final double DEFAULT_AIR_DRAG = 0.99;
    private static final int DEFAULT_MAX_LIFETIME = 200;
    
    private final Atom plugin;
    private final ItemDisplay display;
    private final Vector velocity;
    private final ItemStack projectileItem;
    private final Player shooter;
    private final ProjectileConfig config;
    
    private int ticksAlive = 0;
    private boolean isActive = true;
    
    private Consumer<RayTraceResult> onBlockHit;
    private Consumer<LivingEntity> onEntityHit;
    private Runnable onExpire;
    
    public CustomProjectile(Atom plugin, Location startLocation, Vector initialVelocity, 
                           ItemStack displayItem, ItemStack projectileItem, Player shooter, 
                           ProjectileConfig config) {
        this.plugin = plugin;
        this.velocity = initialVelocity.clone();
        this.projectileItem = projectileItem;
        this.shooter = shooter;
        this.config = config;
        
        this.display = (ItemDisplay) startLocation.getWorld().spawnEntity(startLocation, org.bukkit.entity.EntityType.ITEM_DISPLAY);
        display.setItemStack(displayItem);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        display.setBillboard(Display.Billboard.FIXED);
        display.setViewRange(config.viewRange);
        display.setShadowRadius(0.0f);
        display.setShadowStrength(0.0f);
        display.setInterpolationDuration(config.interpolationDuration);
        display.setInterpolationDelay(config.interpolationDelay);
        
        if (config.baseRotation != null) {
            AxisAngle4f baseAxis = new AxisAngle4f();
            config.baseRotation.get(baseAxis);
            display.setTransformation(new Transformation(
                config.translation,
                baseAxis,
                config.scale,
                new AxisAngle4f(0, 0, 0, 1)
            ));
        }
    }
    
    public void start() {
        display.getScheduler().runAtFixedRate(plugin, task -> {
            if (!update()) {
                task.cancel();
            }
        }, null, 1L, 1L);
    }
    
    private boolean update() {
        if (!display.isValid() || !isActive) {
            cleanup();
            return false;
        }
        
        ticksAlive++;
        if (ticksAlive >= config.maxLifetime) {
            if (onExpire != null) {
                onExpire.run();
            } else {
                display.getLocation().getWorld().dropItemNaturally(display.getLocation(), projectileItem);
            }
            cleanup();
            return false;
        }
        
        Location currentLoc = display.getLocation();
        
        RayTraceResult blockHit = currentLoc.getWorld().rayTraceBlocks(
            currentLoc, 
            velocity.clone().normalize(), 
            velocity.length() + 0.3,
            org.bukkit.FluidCollisionMode.NEVER,
            true
        );
        
        if (blockHit != null && blockHit.getHitBlock() != null) {
            if (onBlockHit != null) {
                onBlockHit.accept(blockHit);
            } else {
                Location hitLoc = blockHit.getHitPosition().toLocation(currentLoc.getWorld());
                hitLoc.getWorld().dropItemNaturally(hitLoc, projectileItem);
            }
            cleanup();
            return false;
        }
        
        RayTraceResult entityHit = currentLoc.getWorld().rayTraceEntities(
            currentLoc, 
            velocity.clone().normalize(), 
            velocity.length() + 0.3,
            0.3,
            entity -> entity instanceof LivingEntity && entity != shooter && entity != display
        );
        
        if (entityHit != null && entityHit.getHitEntity() != null) {
            LivingEntity hitEntity = (LivingEntity) entityHit.getHitEntity();
            if (onEntityHit != null) {
                onEntityHit.accept(hitEntity);
            } else {
                hitEntity.damage(config.damage, shooter);
                Location hitLoc = entityHit.getHitPosition().toLocation(currentLoc.getWorld());
                hitLoc.getWorld().dropItemNaturally(hitLoc, projectileItem);
            }
            cleanup();
            return false;
        }
        
        Location nextLoc = currentLoc.clone().add(velocity);
        
        double speed = velocity.length();
        int dynamicInterpolation = (int) Math.ceil(speed * 3.0);
        dynamicInterpolation = Math.max(1, Math.min(10, dynamicInterpolation));
        
        display.setInterpolationDuration(dynamicInterpolation);
        display.setInterpolationDelay(0);
        display.teleportAsync(nextLoc);
        
        velocity.multiply(config.airDrag);
        velocity.setY(velocity.getY() - config.gravity);
        
        updateRotation();
        
        return true;
    }
    
    private void updateRotation() {
        if (velocity.lengthSquared() < 0.0001) return;
        
        Vector normalized = velocity.clone().normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(-normalized.getX(), normalized.getZ()));
        float pitch = (float) Math.toDegrees(Math.asin(-normalized.getY()));
        
        Location loc = display.getLocation();
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        display.teleportAsync(loc);
    }
    
    private void cleanup() {
        isActive = false;
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
    
    public CustomProjectile onBlockHit(Consumer<RayTraceResult> handler) {
        this.onBlockHit = handler;
        return this;
    }
    
    public CustomProjectile onEntityHit(Consumer<LivingEntity> handler) {
        this.onEntityHit = handler;
        return this;
    }
    
    public CustomProjectile onExpire(Runnable handler) {
        this.onExpire = handler;
        return this;
    }
    
    public UUID getDisplayId() {
        return display.getUniqueId();
    }
    
    public ItemDisplay getDisplay() {
        return display;
    }
    
    public Vector getVelocity() {
        return velocity.clone();
    }
    
    public Player getShooter() {
        return shooter;
    }
    
    public static class ProjectileConfig {
        private double gravity = DEFAULT_GRAVITY;
        private double airDrag = DEFAULT_AIR_DRAG;
        private int maxLifetime = DEFAULT_MAX_LIFETIME;
        private double damage = 8.0;
        private float viewRange = 128.0f;
        private int interpolationDuration = 5;
        private int interpolationDelay = -1;
        private org.joml.Quaternionf baseRotation = null;
        private Vector3f translation = new Vector3f(0, 0, 0);
        private Vector3f scale = new Vector3f(1, 1, 1);
        
        public ProjectileConfig gravity(double gravity) {
            this.gravity = gravity;
            return this;
        }
        
        public ProjectileConfig airDrag(double airDrag) {
            this.airDrag = airDrag;
            return this;
        }
        
        public ProjectileConfig maxLifetime(int ticks) {
            this.maxLifetime = ticks;
            return this;
        }
        
        public ProjectileConfig damage(double damage) {
            this.damage = damage;
            return this;
        }
        
        public ProjectileConfig viewRange(float range) {
            this.viewRange = range;
            return this;
        }
        
        public ProjectileConfig interpolation(int duration, int delay) {
            this.interpolationDuration = duration;
            this.interpolationDelay = delay;
            return this;
        }
        
        public ProjectileConfig baseRotation(org.joml.Quaternionf rotation) {
            this.baseRotation = rotation;
            return this;
        }
        
        public ProjectileConfig translation(float x, float y, float z) {
            this.translation = new Vector3f(x, y, z);
            return this;
        }
        
        public ProjectileConfig scale(float scale) {
            this.scale = new Vector3f(scale, scale, scale);
            return this;
        }
        
        public ProjectileConfig scale(float x, float y, float z) {
            this.scale = new Vector3f(x, y, z);
            return this;
        }
    }
}
