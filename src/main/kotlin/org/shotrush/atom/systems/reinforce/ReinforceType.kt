package org.shotrush.atom.systems.reinforce

import org.bukkit.Material
import org.shotrush.atom.api.ItemRef

enum class ReinforceType(val itemRef: ItemRef) {
    LIGHT(ItemRef.vanilla(Material.COPPER_INGOT)),
    MEDIUM(ItemRef.vanilla(Material.IRON_INGOT)),
    HEAVY(ItemRef.custom("steel_ingot"))
}