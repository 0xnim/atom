package org.shotrush.atom.content.workstation.ballista

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import org.shotrush.atom.Atom
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.listener.AtomListener
import org.shotrush.atom.listener.eventDef
import org.shotrush.atom.listener.register
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object BallistaListener : AtomListener {
    override val eventDefs = mapOf(
        eventDef<PlayerInteractEvent> { Atom.instance.entityDispatcher(it.player) },
        eventDef<PlayerToggleSneakEvent> { Atom.instance.entityDispatcher(it.player) },
        eventDef<PlayerQuitEvent> { Atom.instance.entityDispatcher(it.player) },
    )

    fun register() = (this as AtomListener).register()

    // Track which players are mounted on ballistae
    private val mountedPlayers = mutableMapOf<UUID, BallistaBlockEntity>()

    val BOLT_TYPE_KEY = NamespacedKey(Atom.instance, "bolt_type")

    fun trackMount(player: Player, entity: BallistaBlockEntity) {
        mountedPlayers[player.uniqueId] = entity
    }

    fun trackDismount(player: Player) {
        mountedPlayers.remove(player.uniqueId)
    }

    fun getMountedBallista(player: Player): BallistaBlockEntity? {
        return mountedPlayers[player.uniqueId]
    }

    @EventHandler(priority = EventPriority.HIGH)
    suspend fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val ballista = getMountedBallista(player) ?: return

        // LEFT CLICK while mounted: Start winding
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {
            event.isCancelled = true

            if (ballista.isCharging) {
                // Already charging - show progress
                val progress = ballista.getChargeProgress()
                val percent = (progress * 100).toInt()
                ActionBarManager.send(player, "<yellow>Winding... $percent%</yellow>")
                return
            }

            if (ballista.loadedBolt == null) {
                ActionBarManager.send(player, "<red>No bolt loaded!</red> <gray>Right-click with a bolt to load</gray>")
                return
            }

            if (ballista.startCharging()) {
                ActionBarManager.send(player, "<yellow>Winding ballista...</yellow>")
            }
            return
        }

        // RIGHT CLICK while mounted: Cancel charging or show status
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            // Don't cancel the event for right-click - let block behavior handle bolt loading
            // But if charging, cancel it
            if (ballista.isCharging) {
                event.isCancelled = true
                ballista.cancelCharging()
                ActionBarManager.send(player, "<red>Cancelled winding</red>")
                return
            }

            // If they right-clicked with empty hand while mounted, show status
            val item = player.inventory.itemInMainHand
            if (item.isEmpty || item.type == org.bukkit.Material.AIR) {
                event.isCancelled = true
                if (ballista.loadedBolt == null) {
                    ActionBarManager.send(player, "<gray>No bolt loaded. Right-click with a bolt to load, then left-click to fire.</gray>")
                } else {
                    ActionBarManager.send(player, "<green>Bolt loaded!</green> <gray>Left-click to start winding and fire.</gray>")
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    suspend fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (!event.isSneaking) return

        val player = event.player
        val ballista = getMountedBallista(player) ?: return

        // Cancel charging if active
        if (ballista.isCharging) {
            ballista.cancelCharging()
        }

        // Dismount
        ballista.dismount()
        trackDismount(player)
        ActionBarManager.send(player, "<yellow>Dismounted ballista</yellow>")
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val ballista = getMountedBallista(event.player) ?: return
        ballista.dismount()
        trackDismount(event.player)
    }

    /**
     * Called from BallistaBlockEntity when charging completes
     */
    fun fireBoltFromEntity(player: Player, ballista: BallistaBlockEntity, bolt: BoltType) {
        Atom.instance.launch(Atom.instance.regionDispatcher(ballista.location)) {
            fireBolt(player, ballista, bolt)
        }
    }

    private fun fireBolt(player: Player, ballista: BallistaBlockEntity, bolt: BoltType) {
        val world = ballista.bukkitWorld

        // Calculate direction from yaw/pitch (Minecraft coordinate system)
        // Yaw: 0=south(+Z), 90=west(-X), 180=north(-Z), -90=east(+X)
        val yawRad = Math.toRadians(ballista.aimYaw.toDouble())
        val pitchRad = Math.toRadians(ballista.aimPitch.toDouble())

        val direction = Vector(
            -sin(yawRad) * cos(pitchRad),
            -sin(pitchRad),
            cos(yawRad) * cos(pitchRad)
        ).normalize()

        // Spawn bolt slightly in front of the ballista in aim direction
        val spawnLoc = ballista.location.clone().add(0.5, 1.0, 0.5)
            .add(direction.clone().multiply(0.8)) // Offset forward by 0.8 blocks

        // Spawn arrow
        world.spawn(spawnLoc, Arrow::class.java) { arr ->
            arr.velocity = direction.multiply(BallistaBlockEntity.BOLT_SPEED)
            arr.shooter = player
            arr.damage = bolt.baseDamage
            arr.pickupStatus = AbstractArrow.PickupStatus.DISALLOWED
            arr.isCritical = true
            @Suppress("DEPRECATION")
            arr.knockbackStrength = 2

            // Store bolt type in PDC for hit handling
            arr.persistentDataContainer.set(
                BOLT_TYPE_KEY,
                PersistentDataType.STRING,
                bolt.id
            )
        }

        // Effects
        world.playSound(spawnLoc, Sound.ENTITY_ARROW_SHOOT, 2.0f, 0.5f)
        world.playSound(spawnLoc, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.8f)
        world.playSound(spawnLoc, Sound.ITEM_CROSSBOW_SHOOT, 1.5f, 0.6f)
        world.spawnParticle(Particle.SMOKE, spawnLoc, 10, 0.2, 0.2, 0.2, 0.05)

        ActionBarManager.send(player, "<green>Fired!</green>")
    }
}
