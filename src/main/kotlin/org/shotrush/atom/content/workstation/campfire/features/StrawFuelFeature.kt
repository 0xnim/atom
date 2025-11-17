package org.shotrush.atom.content.workstation.campfire.features

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Campfire
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.Atom
import org.shotrush.atom.content.workstation.campfire.CampfireRegistry

class StrawFuelFeature(
    private val strawExtensionMs: Long = 2 * 60 * 1000L
) : CampfireRegistry.Listener {

    private val strawJobs = mutableMapOf<Location, Job>()
    private val strawCount = mutableMapOf<Location, Int>() // Track actual straw count
    private val atom get() = Atom.instance

    fun tryAddStrawFuel(registry: CampfireRegistry, loc: Location): Long? {
        val campfire = loc.block.state as? Campfire ?: return null

        // 1) Capacity check BEFORE timer extension
        val slot = nextEmptySlot(campfire)
        if (slot == -1) {
            // Fully fueled -> do not extend time
            return null
        }

        // 2) Extend timer now that capacity exists
        val end = registry.addFuel(loc, strawExtensionMs) ?: return null

        // 3) Place visual straw (re-check slot)
        campfire.setItem(slot, ItemStack(Material.WHEAT, 1))
        campfire.setCookTime(slot, 0)
        campfire.setCookTimeTotal(slot, Int.MAX_VALUE)
        campfire.update(true)
        
        // Track straw count
        strawCount[loc] = (strawCount[loc] ?: 0) + 1

        // 4) Schedule progressive visual burn using your scheduler (no coroutines)
        scheduleBurnVisual(loc, slot)

        return end
    }

    private fun scheduleBurnVisual(loc: Location, startingSlot: Int) {
        strawJobs[loc]?.cancel()
        val job = atom.launch(atom.regionDispatcher(loc)) {
            var current = startingSlot
            while (true) {
                delay(strawExtensionMs)
                val campfire = loc.block.state as? Campfire ?: break
                if (current != -1) {
                    campfire.setItem(current, ItemStack(Material.AIR))
                    campfire.update(true)
                    // Decrement straw count when it burns away
                    strawCount[loc] = ((strawCount[loc] ?: 1) - 1).coerceAtLeast(0)
                }
                current = nextFilledSlot(campfire)
                if (current == -1) break
            }
            strawJobs.remove(loc)
        }
        strawJobs[loc] = job
    }

    override fun onCampfireExtinguished(state: CampfireRegistry.CampfireState, reason: String) {
        // Cleanup visual fuel slots
        Atom.instance.launch(Atom.instance.regionDispatcher(state.location)) {
            val cf = state.location.block.state as? Campfire ?: return@launch
            for (i in 0 until cf.size) {
                cf.setItem(i, ItemStack(Material.AIR))
            }
            cf.update(true)
        }
        strawJobs.remove(state.location)?.cancel()
        strawCount.remove(state.location)
    }

    override fun onCampfireBroken(state: CampfireRegistry.CampfireState) {
        strawJobs.remove(state.location)?.cancel()
        
        // Drop actual straw items
        val count = strawCount.remove(state.location) ?: 0
        if (count > 0) {
            val strawItem = net.momirealms.craftengine.bukkit.api.CraftEngineItems.byId(
                net.momirealms.craftengine.core.util.Key.of("atom", "straw")
            )?.buildItemStack()?.apply { amount = count }
            
            if (strawItem != null) {
                state.location.world.dropItemNaturally(state.location, strawItem)
            }
        }
    }

    private fun nextEmptySlot(cf: Campfire): Int {
        for (i in 0 until cf.size) {
            val it = cf.getItem(i)
            if (it == null || it.type.isAir) return i
        }
        return -1
    }

    private fun nextFilledSlot(cf: Campfire): Int {
        for (i in 0 until cf.size) {
            val it = cf.getItem(i)
            if (it != null && !it.type.isAir) return i
        }
        return -1
    }

    private fun isEmpty(cf: Campfire, idx: Int): Boolean {
        if (idx !in 0 until cf.size) return false
        val it = cf.getItem(idx)
        return it == null || it.type.isAir
    }
}