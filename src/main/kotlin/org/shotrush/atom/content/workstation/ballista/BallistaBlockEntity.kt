package org.shotrush.atom.content.workstation.ballista

import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.properties.Property
import net.momirealms.craftengine.core.util.HorizontalDirection
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.shotrush.atom.blocks.AtomBlockEntity
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.core.util.ActionBarManager
import java.util.*

class BallistaBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState,
) : AtomBlockEntity(Workstations.BALLISTA, pos, blockState) {

    companion object {
        const val CHARGE_TICKS_REQUIRED = 40  // 2 seconds to fully wind up
        const val MAX_PITCH_UP = -30f   // How far up can aim (negative = looking up)
        const val MAX_PITCH_DOWN = 45f  // How far down can aim
        const val BOLT_SPEED = 3.0      // Projectile velocity multiplier
    }

    // Placement direction from block state
    val placementDirection: HorizontalDirection
        get() = try {
            blockState().get(blockState().properties.first() as Property<HorizontalDirection>)
        } catch (e: Exception) {
            HorizontalDirection.NORTH
        }

    // State
    var mountedPlayerUUID: UUID? = null
        private set

    var aimYaw: Float = 0f
        private set

    var aimPitch: Float = 0f
        private set

    var loadedBolt: BoltType? = null
        private set

    // Charging state
    var isCharging: Boolean = false
        private set

    var chargeTicks: Int = 0
        private set

    init {
        blockEntityRenderer = BallistaDynamicRenderer(this)
        // Initialize yaw based on placement direction
        aimYaw = when (placementDirection) {
            HorizontalDirection.NORTH -> 180f
            HorizontalDirection.SOUTH -> 0f
            HorizontalDirection.EAST -> -90f
            HorizontalDirection.WEST -> 90f
        }
    }

    override fun loadCustomData(tag: CompoundTag) {
        if (tag.containsKey("aimYaw")) aimYaw = tag.getFloat("aimYaw")
        if (tag.containsKey("aimPitch")) aimPitch = tag.getFloat("aimPitch")
        if (tag.containsKey("loadedBolt")) {
            loadedBolt = BoltType.fromId(tag.getString("loadedBolt"))
        }
        // Charging state and mountedPlayerUUID are not persisted
    }

    override fun saveCustomData(tag: CompoundTag) {
        tag.putFloat("aimYaw", aimYaw)
        tag.putFloat("aimPitch", aimPitch)
        loadedBolt?.let { tag.putString("loadedBolt", it.id) }
    }

    val mountedPlayer: Player?
        get() = mountedPlayerUUID?.let { Bukkit.getPlayer(it) }

    fun isMounted(): Boolean = mountedPlayerUUID != null

    fun canMount(player: Player): Boolean = !isMounted()

    fun mount(player: Player): Boolean {
        if (!canMount(player)) return false

        mountedPlayerUUID = player.uniqueId

        // Initialize aim to player's current look direction
        aimYaw = player.location.yaw
        aimPitch = player.location.pitch.coerceIn(MAX_PITCH_UP, MAX_PITCH_DOWN)

        markDirty(updateRenderer = true)
        return true
    }

    fun dismount(): Boolean {
        cancelCharging()
        mountedPlayerUUID = null
        markDirty(updateRenderer = true)
        return true
    }

    fun updateAim(yaw: Float, pitch: Float) {
        // Minecraft pitch: negative = looking up, positive = looking down
        // MAX_PITCH_UP = -30 (looking up limit), MAX_PITCH_DOWN = 45 (looking down limit)
        val newPitch = pitch.coerceIn(MAX_PITCH_UP, MAX_PITCH_DOWN)
        if (this.aimYaw != yaw || this.aimPitch != newPitch) {
            this.aimYaw = yaw
            this.aimPitch = newPitch
            markDirty(updateRenderer = true)
        }
    }

    fun loadBolt(bolt: BoltType): Boolean {
        if (loadedBolt != null) return false
        loadedBolt = bolt
        markDirty(updateRenderer = true)
        return true
    }

    fun canStartCharging(): Boolean = loadedBolt != null && !isCharging && isMounted()

    fun startCharging(): Boolean {
        if (!canStartCharging()) return false
        isCharging = true
        chargeTicks = 0
        markDirty(updateRenderer = true)

        // Play winding sound
        val player = mountedPlayer
        if (player != null) {
            bukkitWorld.playSound(location, Sound.ITEM_CROSSBOW_LOADING_START, 1.0f, 0.8f)
        }
        return true
    }

    fun cancelCharging() {
        if (isCharging) {
            isCharging = false
            chargeTicks = 0
            markDirty(updateRenderer = true)
        }
    }

    fun getChargeProgress(): Float {
        return (chargeTicks.toFloat() / CHARGE_TICKS_REQUIRED).coerceIn(0f, 1f)
    }

    fun isFullyCharged(): Boolean = chargeTicks >= CHARGE_TICKS_REQUIRED

    /**
     * Called when fully charged - consumes bolt and returns it for firing
     */
    fun fire(): BoltType? {
        val bolt = loadedBolt ?: return null

        loadedBolt = null
        isCharging = false
        chargeTicks = 0
        markDirty(updateRenderer = true)

        return bolt
    }

    fun tick() {
        val player = mountedPlayer

        // Handle charging
        if (isCharging && player != null && player.isOnline) {
            chargeTicks++

            // Play winding tick sound every 10 ticks
            if (chargeTicks % 10 == 0 && chargeTicks < CHARGE_TICKS_REQUIRED) {
                val pitch = 0.8f + (getChargeProgress() * 0.6f) // Pitch increases as it charges
                bukkitWorld.playSound(location, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.5f, pitch)
            }

            // Show progress bar using sendStatus for continuous updates
            val progress = getChargeProgress()
            val filledBars = (progress * 20).toInt()
            val emptyBars = 20 - filledBars
            val progressBar = "<green>${"|".repeat(filledBars)}</green><dark_gray>${"|".repeat(emptyBars)}</dark_gray>"
            val percent = (progress * 100).toInt()
            ActionBarManager.sendStatus(player, "<yellow>Winding</yellow> [$progressBar] <white>$percent%</white>")

            // Check if fully charged
            if (isFullyCharged()) {
                // Fire automatically
                val bolt = fire()
                if (bolt != null) {
                    BallistaListener.fireBoltFromEntity(player, this, bolt)
                }
            }
        }

        // Update aim from mounted player
        if (player != null && player.isOnline) {
            updateAim(player.location.yaw, player.location.pitch)

            // Auto-dismount if player moves too far from ballista
            val dist = player.location.distanceSquared(location)
            if (dist > 25.0) { // 5 blocks
                dismount()
                BallistaListener.trackDismount(player)
                ActionBarManager.send(player, "<yellow>Dismounted ballista (moved too far)</yellow>")
            }
        } else if (mountedPlayerUUID != null) {
            // Player disconnected while mounted
            cancelCharging()
            mountedPlayerUUID = null
            markDirty(updateRenderer = true)
        }
    }

    override fun preRemove() {
        // Dismount player if mounted when block is broken
        val player = mountedPlayer
        if (player != null) {
            BallistaListener.trackDismount(player)
            mountedPlayerUUID = null
        }
    }
}
