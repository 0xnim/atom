package org.shotrush.atom.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.matches
import org.shotrush.atom.util.Key

sealed interface ItemRef {
    fun matches(stack: ItemStack): Boolean

    companion object {
        fun vanilla(material: Material) = MaterialRef(material)
        fun custom(key: Key) = CEItemRef(key)
        fun custom(key: String) = custom(Key("atom", key))
    }

    data class MaterialRef(val material: Material) : ItemRef {
        override fun matches(stack: ItemStack): Boolean {
            return if (stack.isCustomItem()) false else stack.type == material
        }
    }

    data class CEItemRef(val key: Key) : ItemRef {
        override fun matches(stack: ItemStack): Boolean {
            return if (!stack.isCustomItem()) false else stack.matches(key)
        }
    }
}