package org.shotrush.atom.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.matches
import org.shotrush.atom.util.Key

sealed interface ItemReference {
    fun matches(stack: ItemStack): Boolean

    companion object {
        fun vanilla(material: Material) = MaterialReference(material)
        fun custom(key: Key) = CEItemReference(key)
        fun custom(key: String) = custom(Key("atom", key))
    }

    data class MaterialReference(val material: Material) : ItemReference {
        override fun matches(stack: ItemStack): Boolean {
            return if (stack.isCustomItem()) false else stack.type == material
        }
    }

    data class CEItemReference(val key: Key) : ItemReference {
        override fun matches(stack: ItemStack): Boolean {
            return if (!stack.isCustomItem()) false else stack.matches(key)
        }
    }
}