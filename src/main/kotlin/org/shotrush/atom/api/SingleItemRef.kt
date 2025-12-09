package org.shotrush.atom.api

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.isCustomItem
import org.shotrush.atom.matches
import org.shotrush.atom.util.Key

sealed class SingleItemRef : ItemFilter {
    abstract fun createStack(amount: Int = 1): ItemStack

    companion object {
        fun vanilla(material: Material) = MaterialRef(material)
        fun custom(key: Key) = CEItemRef(key)
        fun custom(key: String) = custom(Key("atom", key))
    }

    data class MaterialRef(val material: Material) : SingleItemRef() {
        override fun matches(stack: ItemStack): Boolean {
            return if (stack.isCustomItem()) false else stack.type == material
        }

        override fun createStack(amount: Int): ItemStack {
            return ItemStack(material, amount)
        }
    }

    data class CEItemRef(val key: Key) : SingleItemRef() {
        override fun matches(stack: ItemStack): Boolean {
            return if (!stack.isCustomItem()) false else stack.matches(key)
        }

        override fun createStack(amount: Int): ItemStack {
            return CraftEngineItems.byId(key.toCEKey())?.buildItemStack(amount) ?: error("Item not found!")
        }
    }
}