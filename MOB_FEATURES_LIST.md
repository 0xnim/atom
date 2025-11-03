# Mob AI Features - Complete List

## 🚨 OVERVIEW
This is a comprehensive realistic mob AI system that was implemented. It may be "overboard" - this document lists everything so you can decide what to keep/remove.

---

## 1. CORE SYSTEMS

### Herd Management
- **HerdManager**: Central coordinator for all herds
- **Herd Formation**: Animals auto-group by species
- **Leader Election**: Based on health and age
- **Herd Roles**: Leader vs Follower behaviors
- **Size Ranges**: Species-specific (2-20 animals)
- **Cross-Chunk Persistence**: Herds saved via PDC

### Dominance Hierarchy
- **Ranks**: ALPHA, BETA, SUBORDINATE, OMEGA
- **DominanceHierarchy**: Tracking and rank challenges
- **Rank-Based Behaviors**: Different roles for different ranks

### Memory System
- **MemoryManager**: Central memory coordinator
- **AnimalMemory**: Danger location tracking
- **PlayerMemory**: Interaction history, threat levels
- **Spatial Memory**: Remembers dangerous areas

### Needs System
- **NeedsManager**: Tracks hunger, thirst, energy
- **Passive Drain**: Constant need degradation
- **Activity Costs**: Actions consume energy
- **Critical Thresholds**: Force behaviors when desperate

### Vision System
- **Vision Cones**: Directional awareness
- **Line of Sight**: Proper obstruction checking
- **Detection Chances**: Based on angle/distance
- **Behind Detection**: Reduced awareness from behind

### Environmental Context
- **Time of Day**: Day/night behavior changes
- **Weather Awareness**: Storm reactions
- **Biome Preferences**: Habitat suitability
- **Activity Patterns**: Diurnal/Nocturnal/Crepuscular

### Life Cycle
- **Age Stages**: Baby, Juvenile, Adult, Elder
- **FamilyRelationships**: Parent/sibling tracking
- **Bond Strength**: Family member closeness
- **Age-Based Stats**: Different capabilities per age

### Combat Systems
- **InjurySystem**: Damage tracking, injury states
- **FatigueSystem**: Combat exhaustion
- **MoraleSystem**: Courage/fear mechanics

### Vocalization System
- **Call Types**: ALARM, CONTACT, THREAT, DISTRESS, MATING
- **Species Sounds**: Species-specific audio
- **Herd Response**: Calls trigger group reactions

---

## 2. AI GOALS (30+ Custom Goals)

### Basic Movement & Cohesion
1. **StayNearHerdGoal** - Followers stick near leader
2. **HerdLeaderWanderGoal** - Leader-driven exploration
3. **ReunionGoal** - Separated members navigate back

### Panic & Defense
4. **HerdPanicGoal** - Coordinated fleeing with stamina
5. **AvoidPlayerWhenInjuredGoal** - Tactical retreat
6. **TerritoryDefenseGoal** - ALPHA defends from rival herds
7. **MotherProtectionGoal** - Mothers defend babies aggressively

### Needs-Based
8. **GrazingGoal** - Herbivores eat grass blocks
9. **SeekWaterGoal** - Find and drink from water
10. **HuntPreyGoal** - Carnivores hunt prey animals
11. **ScavengeGoal** - Pick up dropped food items
12. **RestWhenExhaustedGoal** - Force rest at critical energy

### Environmental
13. **SleepGoal** - Rest when tired/nighttime
14. **SeekShelterGoal** - Find shelter during storms
15. **TimeBasedActivityGoal** - Speed modifiers by time

### Combat & Hunting
16. **AcquireNearestPlayerTargetGoal** - Smart aggression targeting
17. **ChaseAndMeleeAttackGoal** - Pursuit with domestication scaling
18. **StalkPreyGoal** - Stealth approach
19. **TrackWoundedPreyGoal** - Follow injured targets
20. **FlankAndSurroundGoal** - Pack tactics

### Social Behaviors
21. **SentryBehaviorGoal** - ALPHA/BETA watch for threats
22. **PlayBehaviorGoal** - Baby animals play with siblings
23. **ShareFoodGoal** - High-rank members share with hungry family
24. **DeathEffectsGoal** - Mourning, gathering, morale impact

---

## 3. SPECIAL ABILITIES (8 Species-Specific)

### Aggressive Abilities
1. **Ram Charge** (Goat/Sheep) - Windup animation, wall collision/stun, impact damage
2. **Kick Attack** (Horse/Donkey) - Rear-up animation, rear detection, strong knockback
3. **Counter Charge** (Pig) - Desperate charge when cornered <20% HP
4. **Pounce Attack** (Fox) - Leap attack from distance
5. **Pack Hunting** (Wolf) - Coordinated targeting, sync attacks
6. **Cub Protection** (Polar Bear) - Enrage when cub attacked

### Defensive Abilities
7. **Flight Burst** (Chicken) - Vertical escape when fleeing <70% HP
8. **Stampede** (Cow) - Synchronized herd fleeing, trample damage
9. **Roll Defense** (Armadillo) - Invulnerable ball when <90% HP

---

## 4. ENHANCED EFFECTS

### Visual Effects
- **Particle Indicators**: 6 colors for different states
- **Boss Bars**: Hunger/thirst/energy display
- **Action Bars**: Active goal names
- **Dust Clouds**: Stampede/charge effects
- **Soul Particles**: Death/mourning
- **Ground Shake**: Stampede tremors

### Audio Effects
- **Species Sounds**: 24+ species-specific
- **Attack Sounds**: Impact/collision audio
- **Vocalization Calls**: 5 call types
- **Environmental**: Thunder, tremor effects

---

## 5. DEBUG SYSTEM

### Debug Levels
- OFF, MINIMAL, NORMAL, VERBOSE

### Debug Categories
- GOALS, NEEDS, MEMORY, COMBAT, SOCIAL, ENVIRONMENTAL

### Visual Debugging
- Particle trails
- Boss bars
- Action bars
- Real-time entity tracking

### Performance Monitoring
- >5ms warnings
- Thread-safe logging
- Performance stats

### Debug Commands (/mobai)
- `debug <level>` - Set debug level
- `info <entity>` - Entity stats
- `goals <entity>` - Active goals
- `needs <entity>` - Hunger/thirst/energy
- `memory <entity>` - Danger/player memories
- `herd <entity>` - Herd hierarchy
- `track <entity>` - Visual tracking
- `performance` - Performance stats
- `reset` - Clear debug data

### Herd Commands (/herd)
- `info` - Animal stats and herd membership
- `list` - Show all nearby herds

---

## 6. SPECIES COVERAGE

### 24 Species Configured
- **Farm**: Cow, Pig, Sheep, Chicken, Rabbit, Goat
- **Pack**: Horse, Donkey, Mule, Llama
- **Predators**: Wolf, Fox, Polar Bear
- **Wild**: Panda, Mooshroom, Ocelot, Cat, Parrot
- **Special**: Armadillo, Hoglin, Piglin, Strider, Turtle, Frog, Axolotl

Each has:
- Herd size ranges
- Flee/chase speeds
- Aggression chance
- Panic threshold
- Cohesion radius
- Activity pattern
- Diet type

---

## 7. INTEGRATIONS

### AnimalDomestication
- Babies join parent herds
- Domestication affects all behaviors
- Scaling aggression/fear
- Tameness influences

### Event Handling
- `onAnimalDamage` - Injury tracking, morale, vocalizations
- `onAnimalDeath` - Complete system cleanup, mourning
- Chunk load/unload handling

### Stat Enhancements
- Health boosts
- Speed modifiers
- Knockback resistance
- One-time application (prevents restart multiplication)

---

## 8. PERFORMANCE & SAFETY

### Folia Compatibility
- All async chunk operations use block coordinates
- `GlobalRegionScheduler` for periodic tasks
- `EntityScheduler` for player operations
- NO main thread assumptions

### Thread Safety
- ConcurrentHashMap for herds
- Thread-safe logging
- Concurrent collections throughout

### Null Safety
- Comprehensive null checks
- Null-safe bond updates
- Location validation
- World validity checks

---

## 📊 STATISTICS

- **Total Goals**: 30+
- **Special Abilities**: 8
- **Core Systems**: 10
- **Species Covered**: 24
- **Debug Commands**: 9
- **Files Created/Modified**: 50+
- **Lines of Code**: 5000+

---

## 🤔 WHAT TO CONSIDER REMOVING

If this is "overboard", consider removing:

### High Complexity, Low Impact
- Life cycle system (age stages, family relationships)
- Mourning/death effects
- Vocalization system
- Visual debugging particles
- Play behavior

### Resource Intensive
- Memory system (danger locations)
- Vision cone calculations
- Performance monitoring
- Environmental context polling

### Niche Features
- Sentry behavior
- Share food goal
- Territory defense
- Dominance hierarchy challenges
- Scavenging

### Can Keep Core
- Herd formation and cohesion
- Basic panic/flee
- Chase and attack
- Needs system (simplified)
- Special abilities (selective)
