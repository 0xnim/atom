package org.shotrush.atom.systems.room

import com.github.shynixn.mccoroutine.folia.ticks
import kotlinx.coroutines.delay
import org.bukkit.World
import org.joml.Vector3i
import kotlin.random.Random

object RoomScanner {
    suspend fun scanAt(world: World, start: Vector3i, maxVolume: Int = 800, retries: Int = 0): Room? {
        repeat(retries + 1) { attempt ->
            val scan = RoomScan(world, start, maxVolume = maxVolume)
            val ok = scan.scan()
            if (ok) {
                val room = scan.toRoomOrNull() ?: return null
                // Dedup on commit to avoid duplicates if another scan completed meanwhile
                return if (RoomRegistry.tryRegisterDedup(room)) {
                    room
                } else {
                    null
                }
            } else if (attempt < retries) {
                delay((4 + Random.nextInt(5)).ticks) // 4–8 ticks backoff
            }
        }
        return null
    }
}