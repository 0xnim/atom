# 🌍 Chunk Loading & Realistic Animal Movement - Analysis & Solutions

## 🔴 The Problem

**Current Situation:**
- Animals in **unloaded chunks** don't tick/update
- Needs system drains only when chunks are loaded
- Animals "freeze in time" when players leave the area
- Herds can't migrate realistically across the world
- Animals won't starve/die even after days of being unloaded

**Why This Breaks Realism:**
```
Player visits cow herd → cows start grazing, needs drain
Player leaves (chunks unload) → cows FREEZE for 10 hours
Player returns → cows resume from exact same state
Result: Cows never died, never moved, needs barely changed
```

---

## 💡 Possible Solutions

### ❌ **Option 1: Force-Load Chunks**
**Concept:** Keep all animal chunks loaded permanently

```java
// Keep chunks with animals loaded
chunk.addPluginChunkTicket(plugin);
```

**Pros:**
- ✅ Animals behave naturally 24/7
- ✅ No special handling needed

**Cons:**
- ❌ **CATASTROPHIC performance impact**
- ❌ 100 animals = 100+ loaded chunks = server crash
- ❌ Defeats Minecraft's chunk optimization
- ❌ Not scalable beyond ~20 animals

**Verdict:** ❌ **NOT VIABLE** for realistic scale

---

### ⚠️ **Option 2: Pause When Unloaded**
**Concept:** Drastically slow/pause needs drain for unloaded animals

```java
// In AnimalNeeds.update()
if (!chunk.isLoaded()) {
    drainMultiplier = 0.01; // 1% drain rate
}
```

**Pros:**
- ✅ Simple to implement
- ✅ Prevents unfair death

**Cons:**
- ⚠️ Not realistic (animals "freeze")
- ⚠️ World feels static when not observed
- ⚠️ Players can "abuse" by staying away

**Verdict:** ⚠️ **PARTIAL SOLUTION** - better than nothing

---

### ✅ **Option 3: Time-Skip Simulation (RECOMMENDED)**
**Concept:** Calculate what SHOULD have happened during unloaded time

```java
// When chunks load
long unloadedTime = now - lastUpdate;
simulateBehavior(unloadedTime);
```

**Implementation:**
1. Track `lastChunkUpdate` timestamp
2. On chunk load, calculate elapsed time
3. Apply probabilistic state changes:
   - Needs drain at reduced rate (25% of normal)
   - Random movement/migration
   - Death if critically low for too long
   - Herd position shifts

**Pros:**
- ✅ Realistic outcomes without performance cost
- ✅ Animals can die/move while unloaded
- ✅ Scalable to thousands of animals
- ✅ Feels dynamic and alive

**Cons:**
- ⚠️ Not "real-time" (discrete updates on load)
- ⚠️ Requires careful probability tuning
- ⚠️ More complex implementation

**Verdict:** ✅ **BEST APPROACH** for realistic survival

---

## 🎯 Recommended Implementation

### **Hybrid Time-Skip System**

**Core Principles:**
1. **When loaded:** Normal AI behavior (current system)
2. **When unloaded:** Track timestamp, pause active behavior
3. **On reload:** Simulate catch-up based on elapsed time

**What to Simulate:**

#### **A. Needs Drain (Automatic)**
```java
// Reduced drain rate when unloaded
double unloadedDrainMultiplier = 0.25; // 25% of normal rate

// On chunk load
long elapsedMs = now - lastUpdate;
double elapsedSeconds = elapsedMs / 1000.0;

hunger -= HUNGER_DRAIN_PER_SECOND * elapsedSeconds * unloadedDrainMultiplier;
thirst -= THIRST_DRAIN_PER_SECOND * elapsedSeconds * unloadedDrainMultiplier;
energy -= ENERGY_DRAIN_PER_SECOND * elapsedSeconds * unloadedDrainMultiplier;
```

**Why 25%?** Animals rest/idle most of the time when not being chased/hunting.

#### **B. Death Check (Probabilistic)**
```java
// If critically low for too long, chance of death
if (hunger < HUNGER_CRITICAL && elapsedHours > 24) {
    double deathChance = 0.5; // 50% chance per day
    if (Math.random() < deathChance) {
        animal.damage(1000); // Kill
    }
}
```

#### **C. Movement/Migration (Probabilistic)**
```java
// Chance to relocate based on time
double migrationChance = Math.min(0.5, elapsedHours / 48.0); // 50% after 2 days

if (Math.random() < migrationChance) {
    // Move animal to nearby location within loaded chunks
    Location newLoc = findNearbyLocation(animal.getLocation(), 100);
    animal.teleport(newLoc);
}
```

#### **D. Herd Cohesion (Group movement)**
```java
// If herd leader moved, move followers proportionally
if (isLeader && migrated) {
    moveHerdMembers(herdId, newLocation);
}
```

---

## 📝 Implementation Plan

### **Phase 1: Add Timestamp Tracking** ✅ ALREADY EXISTS!

```java
// In AnimalNeeds.java - ALREADY THERE
private long lastUpdate;
```

### **Phase 2: Create UnloadedBehaviorSimulator**

```java
public class UnloadedBehaviorSimulator {
    
    public static void simulateCatchup(Animals animal, AnimalNeeds needs, 
                                       long lastUpdate, SpeciesBehavior behavior) {
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastUpdate;
        double elapsedHours = elapsedMs / (1000.0 * 60 * 60);
        
        if (elapsedHours < 0.1) return; // Less than 6 minutes, skip
        
        // Simulate needs with reduced drain
        simulateNeedsDrain(needs, elapsedMs);
        
        // Check for death
        if (shouldDieFromNeglect(needs, elapsedHours)) {
            animal.damage(1000);
            return;
        }
        
        // Simulate movement
        if (shouldMigrate(elapsedHours, behavior)) {
            attemptMigration(animal, elapsedHours);
        }
    }
    
    private static void simulateNeedsDrain(AnimalNeeds needs, long elapsedMs) {
        double elapsedSeconds = elapsedMs / 1000.0;
        double multiplier = 0.25; // 25% drain when unloaded
        
        needs.drainFromActivity(
            HUNGER_DRAIN_PER_SECOND * elapsedSeconds * multiplier,
            THIRST_DRAIN_PER_SECOND * elapsedSeconds * multiplier,
            ENERGY_DRAIN_PER_SECOND * elapsedSeconds * multiplier
        );
    }
    
    private static boolean shouldDieFromNeglect(AnimalNeeds needs, double hours) {
        if (needs.isStarving() && hours > 48) return Math.random() < 0.5;
        if (needs.isDehydrated() && hours > 24) return Math.random() < 0.7;
        return false;
    }
    
    private static boolean shouldMigrate(double hours, SpeciesBehavior behavior) {
        // Herbivores migrate to find food, carnivores to hunt
        double baseChance = hours / 72.0; // 100% after 3 days
        return Math.random() < Math.min(0.5, baseChance);
    }
}
```

### **Phase 3: Hook into EntitiesLoadEvent**

```java
@EventHandler
public void onEntitiesLoad(EntitiesLoadEvent event) {
    for (Entity entity : event.getEntities()) {
        if (entity instanceof Animals animal) {
            AnimalNeeds needs = needsManager.getNeeds(animal);
            SpeciesBehavior behavior = SpeciesBehavior.get(animal.getType());
            
            // Simulate what happened while unloaded
            UnloadedBehaviorSimulator.simulateCatchup(
                animal, needs, needs.getLastUpdate(), behavior
            );
        }
    }
}
```

---

## ⚙️ Tuning Parameters

### **Drain Rates When Unloaded**
```java
UNLOADED_DRAIN_MULTIPLIER = 0.25  // 25% of normal
```
- Too high: Animals die unfairly
- Too low: World feels frozen
- **Sweet spot:** 20-30%

### **Death Thresholds**
```java
STARVING_DEATH_TIME = 48 hours
DEHYDRATED_DEATH_TIME = 24 hours
DEATH_CHANCE_PER_DAY = 0.5  // 50%
```

### **Migration Chances**
```java
MIGRATION_CHANCE_BASE = hours / 72.0  // Linear increase
MAX_MIGRATION_DISTANCE = 100 blocks
HERD_FOLLOWS_LEADER = true
```

---

## 🎮 Expected Player Experience

### **Before (Current System)**
```
Day 1: Player finds cow herd, leaves
Day 7: Player returns, cows exactly where they were, full health
Result: Static, unrealistic
```

### **After (With Time-Skip)**
```
Day 1: Player finds cow herd, leaves
Day 7: Player returns, herd has:
  - Moved 50 blocks away (migration)
  - 1 cow died from starvation (was already low)
  - Needs are 60% instead of 100%
  - Herd still functional and realistic
Result: Dynamic, living world
```

---

## 📊 Performance Impact

### **Memory Cost**
- Added: 1 `long` timestamp per animal (8 bytes)
- Total overhead: **Negligible** (8 bytes × 1000 animals = 8KB)

### **CPU Cost**
- Time-skip calculation: ~0.1ms per animal on chunk load
- 100 animals loading: ~10ms one-time cost
- **Impact:** None (happens on chunk load anyway)

### **Scalability**
- ✅ Works with 10,000+ animals
- ✅ No background threading needed
- ✅ No chunk tickets required
- ✅ O(1) per animal on load

---

## 🚀 Quick Start (Minimal Version)

If you want to start simple, just implement **reduced drain**:

```java
// In NeedsManager.startNeedsUpdateTask()
for (Map.Entry<UUID, AnimalNeeds> entry : needs.entrySet()) {
    Animals animal = (Animals) plugin.getServer().getEntity(entry.getKey());
    
    // If animal not loaded, skip update (frozen in time)
    if (animal == null || !animal.isValid()) {
        continue;
    }
    
    entry.getValue().update();
}
```

This prevents drain when unloaded (Option 2), which is better than animals dying unfairly.

---

## 🎯 Recommendation

**For maximum realism:** Implement **Option 3 (Time-Skip Simulation)**

**For quick fix:** Use **Option 2 (Pause when unloaded)**

**Never use:** Option 1 (Force-loading) - it will crash your server

The time-skip approach gives you a living, breathing world where animals persist and behave realistically even when players aren't watching, without the catastrophic performance cost of force-loading chunks.

Would you like me to implement the full UnloadedBehaviorSimulator system?
