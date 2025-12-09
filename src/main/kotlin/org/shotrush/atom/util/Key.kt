package org.shotrush.atom.util

import net.momirealms.craftengine.core.util.Key as CEKey

data class Key(val namespace: String, val key: String) {
    override fun toString(): String {
        return "$namespace:$key"
    }

    fun toCEKey(): CEKey = CEKey(namespace, key)
}

fun CEKey.asAtomKey(): Key = Key(namespace, value)