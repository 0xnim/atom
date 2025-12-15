package org.shotrush.atom.content.workstation.ballista

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.entity.Billboard
import net.momirealms.craftengine.core.entity.ItemDisplayContext
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.util.QuaternionUtils
import org.bukkit.inventory.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f
import org.shotrush.atom.blocks.AtomBlockEntityRenderer

class BallistaDynamicRenderer(val entity: BallistaBlockEntity) : AtomBlockEntityRenderer({
    origin(entity.pos())

    // Turret element - rotates with player aim
    item("turret") {
        position(0.5, 0.5, 0.5) // Center of block

        // Dynamic rotation based on entity aim
        rotation {
            // Convert yaw/pitch to radians - match projectile direction math
            // Minecraft yaw: 0=south, 90=west, 180=north, -90=east
            // Model assumed to face south by default
            val yawRad = Math.toRadians(entity.aimYaw.toDouble())
            val pitchRad = Math.toRadians(-entity.aimPitch.toDouble()) // Negate pitch for correct visual

            // Create rotation quaternion: yaw around Y axis, pitch around X axis
            val yawQuat = QuaternionUtils.toQuaternionf(0.0, yawRad, 0.0)
            val pitchQuat = QuaternionUtils.toQuaternionf(pitchRad, 0.0, 0.0)

            // Combine: apply yaw first, then pitch in rotated frame
            Quaternionf(yawQuat).mul(pitchQuat)
        }

        // Display the turret model (uses crossbow as placeholder)
        displayedItem {
            CraftEngineItems.byId(Key.of("atom:ballista_turret"))?.buildItemStack()
                ?: ItemStack(org.bukkit.Material.CROSSBOW)
        }

        shadow(0.3f, 0.8f)
        viewRange(64f)
        displayContext(ItemDisplayContext.NONE)
        billboard(Billboard.FIXED)
        scale(Vector3f(1.5f, 1.5f, 1.5f))

        // Update at 20 UPS when a player is mounted, 2 UPS otherwise
        updatesPerSecond {
            if (entity.isMounted()) 20f else 2f
        }

        visible(true)
    }

    // Loaded bolt indicator
    item("loadedBolt") {
        position(0.5, 0.7, 0.5) // Slightly above turret

        rotation {
            val yawRad = Math.toRadians(entity.aimYaw.toDouble())
            val pitchRad = Math.toRadians(-entity.aimPitch.toDouble())
            val yawQuat = QuaternionUtils.toQuaternionf(0.0, yawRad, 0.0)
            val pitchQuat = QuaternionUtils.toQuaternionf(pitchRad, 0.0, 0.0)
            Quaternionf(yawQuat).mul(pitchQuat)
        }

        displayedItem {
            entity.loadedBolt?.let { bolt ->
                CraftEngineItems.byId(Key.of(bolt.itemKey))?.buildItemStack()
            } ?: ItemStack.empty()
        }

        shadow(0f, 0f)
        viewRange(32f)
        displayContext(ItemDisplayContext.FIXED)
        billboard(Billboard.FIXED)
        scale(Vector3f(0.6f, 0.6f, 0.6f))

        updatesPerSecond {
            if (entity.isMounted()) 20f else 2f
        }

        autoVisibleFromItem()
    }
})
