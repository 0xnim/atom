package org.shotrush.atom.systems.reinforce

import org.bukkit.Material
import org.shotrush.atom.item.ItemReference

enum class ReinforceType(itemRef: ItemReference) {
    LIGHT(ItemReference.vanilla(Material.COPPER_INGOT)),
    MEDIUM(ItemReference.vanilla(Material.IRON_INGOT)),
    HEAVY(ItemReference.custom("steel_ingot"))
}