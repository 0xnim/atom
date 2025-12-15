package org.shotrush.atom.content.workstation.ballista

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

enum class BoltType(
    val id: String,
    val baseDamage: Double,
    val itemKey: String,
    val explosionPower: Float = 0f,
    val poisonEffect: PotionEffect? = null
) {
    IRON(
        id = "iron",
        baseDamage = 20.0,
        itemKey = "atom:iron_bolt"
    ),
    POISON(
        id = "poison",
        baseDamage = 15.0,
        itemKey = "atom:poison_bolt",
        poisonEffect = PotionEffect(PotionEffectType.POISON, 200, 1) // 10 seconds, level 2
    ),
    EXPLOSIVE(
        id = "explosive",
        baseDamage = 10.0,
        itemKey = "atom:explosive_bolt",
        explosionPower = 2.0f // Smaller than TNT (4.0)
    );

    companion object {
        fun fromId(id: String): BoltType? = entries.find { it.id == id }
        fun fromItemKey(key: String): BoltType? = entries.find { it.itemKey == key }
    }
}
