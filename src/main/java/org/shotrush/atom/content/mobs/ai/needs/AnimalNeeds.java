package org.shotrush.atom.content.mobs.ai.needs;

public class AnimalNeeds {
    
    private double hunger;
    private double thirst;
    private double energy;
    private long lastUpdate;
    
    private static final double MAX_HUNGER = 100.0;
    private static final double MAX_THIRST = 100.0;
    private static final double MAX_ENERGY = 100.0;
    private static final double HUNGER_DRAIN_PER_SECOND = 0.02;
    private static final double THIRST_DRAIN_PER_SECOND = 0.03;
    private static final double ENERGY_DRAIN_PER_SECOND = 0.01;
    private static final double HUNGER_CRITICAL = 20.0;
    private static final double THIRST_CRITICAL = 20.0;
    private static final double ENERGY_CRITICAL = 20.0;
    
    public AnimalNeeds() {
        this.hunger = MAX_HUNGER;
        this.thirst = MAX_THIRST;
        this.energy = MAX_ENERGY;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public void update() {
        long now = System.currentTimeMillis();
        double deltaSeconds = (now - lastUpdate) / 1000.0;
        lastUpdate = now;
        
        hunger = Math.max(0, hunger - (HUNGER_DRAIN_PER_SECOND * deltaSeconds));
        thirst = Math.max(0, thirst - (THIRST_DRAIN_PER_SECOND * deltaSeconds));
        energy = Math.max(0, energy - (ENERGY_DRAIN_PER_SECOND * deltaSeconds));
    }
    
    public void eat(double amount) {
        hunger = Math.min(MAX_HUNGER, hunger + amount);
    }
    
    public double getHunger() {
        return hunger;
    }
    
    public double getHungerPercent() {
        return hunger / MAX_HUNGER;
    }
    
    public boolean isHungry() {
        return hunger < MAX_HUNGER * 0.6;
    }
    
    public boolean isStarving() {
        return hunger < HUNGER_CRITICAL;
    }
    
    public double getThirst() {
        return thirst;
    }
    
    public double getEnergy() {
        return energy;
    }
    
    public boolean isThirsty() {
        return thirst < MAX_THIRST * 0.6;
    }
    
    public boolean isDehydrated() {
        return thirst < THIRST_CRITICAL;
    }
    
    public boolean isTired() {
        return energy < MAX_ENERGY * 0.6;
    }
    
    public boolean isExhausted() {
        return energy < ENERGY_CRITICAL;
    }
}
