package org.shotrush.atom.content.workstation.ballista

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.shotrush.atom.Atom
import org.shotrush.atom.listener.AtomListener
import org.shotrush.atom.listener.eventDef
import org.shotrush.atom.listener.register

object BallistaProjectileListener : AtomListener {
    override val eventDefs = mapOf(
        eventDef<ProjectileHitEvent> { event ->
            Atom.instance.regionDispatcher(event.entity.location)
        }
    )

    fun register() = (this as AtomListener).register()

    // Poison cloud configuration
    private const val POISON_CLOUD_RADIUS = 4.0f
    private const val POISON_CLOUD_DURATION_TICKS = 200  // 10 seconds
    private const val POISON_EFFECT_DURATION_TICKS = 100 // 5 seconds per application
    private const val POISON_EFFECT_AMPLIFIER = 1        // Poison II

    @EventHandler(priority = EventPriority.HIGH)
    suspend fun onProjectileHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? AbstractArrow ?: return

        // Check if this is a ballista bolt
        val boltTypeId = arrow.persistentDataContainer.get(
            BallistaListener.BOLT_TYPE_KEY,
            PersistentDataType.STRING
        ) ?: return

        val boltType = BoltType.fromId(boltTypeId) ?: return

        // Use hit block location if available, otherwise arrow location
        val hitLocation = event.hitBlock?.location?.add(0.5, 0.5, 0.5) ?: arrow.location
        val world = hitLocation.world

        when (boltType) {
            BoltType.IRON -> {
                // Standard damage already applied via arrow.damage
                // No additional effect
            }

            BoltType.POISON -> {
                // Create lingering poison cloud
                val poisonColor = Color.fromRGB(67, 130, 42) // Dark green poison color
                world.spawn(hitLocation, AreaEffectCloud::class.java) { cloud ->
                    cloud.source = arrow.shooter as? Player
                    cloud.radius = POISON_CLOUD_RADIUS
                    cloud.radiusPerTick = -0.005f // Slowly shrinks
                    cloud.duration = POISON_CLOUD_DURATION_TICKS
                    cloud.durationOnUse = 0
                    cloud.radiusOnUse = 0f
                    cloud.reapplicationDelay = 20 // 1 second between re-applications
                    cloud.waitTime = 10 // Small delay before active
                    cloud.setParticle(Particle.ENTITY_EFFECT, poisonColor) // Particle with color data
                    cloud.addCustomEffect(
                        PotionEffect(
                            PotionEffectType.POISON,
                            POISON_EFFECT_DURATION_TICKS,
                            POISON_EFFECT_AMPLIFIER,
                            false, // No ambient
                            true,  // Show particles
                            true   // Show icon
                        ),
                        true
                    )
                }
            }

            BoltType.EXPLOSIVE -> {
                // Create explosion at impact point (works for both entity and block hits)
                hitLocation.world.createExplosion(
                    hitLocation,
                    boltType.explosionPower,
                    false,  // No fire
                    true    // Block damage for siege weapon effect
                )
            }
        }

        // Remove the arrow after effect
        arrow.remove()
    }
}
