package org.shotrush.atom.systems.reinforce

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.api.ItemRef

enum class ReinforceType(val displayName: String, val itemRef: ItemRef, val speedMultiplier: Double) {
    LIGHT("Light", ItemRef.vanilla(Material.COPPER_INGOT), 1.25),
    MEDIUM("Medium", ItemRef.vanilla(Material.IRON_INGOT), 1.75),
    HEAVY("Heavy", ItemRef.custom("steel_ingot"), 2.5);

    fun isHigher(type: ReinforceType?): Boolean {
        if(type == null) return true
        return ordinal > type.ordinal
    }

    companion object {
        fun byItem(item: ItemStack): ReinforceType? {
            return entries.find { it.itemRef.matches(item) }
        }
    }
}