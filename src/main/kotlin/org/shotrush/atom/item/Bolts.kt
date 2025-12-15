package org.shotrush.atom.item

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.item.CustomItem
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.content.workstation.ballista.BoltType
import org.shotrush.atom.getNamespacedKey

object Bolts {
    val IRON_BOLT by item("atom:iron_bolt")
    val POISON_BOLT by item("atom:poison_bolt")
    val EXPLOSIVE_BOLT by item("atom:explosive_bolt")

    fun isBolt(item: ItemStack): Boolean {
        val key = item.getNamespacedKey()
        return BoltType.entries.any { it.itemKey == key }
    }

    fun getBoltType(item: ItemStack): BoltType? {
        val key = item.getNamespacedKey()
        return BoltType.fromItemKey(key)
    }
}
