package org.shotrush.atom.systems.blockbreak

import org.bukkit.block.Block
import org.shotrush.atom.api.BlockRef
import org.shotrush.atom.listener.AtomListener
import org.shotrush.atom.listener.EventClass
import org.shotrush.atom.listener.EventRunner
import org.shotrush.atom.asReference

object BlockBreakSystem : AtomListener {
    override val eventDefs: Map<EventClass, EventRunner>
        get() = TODO("Not yet implemented")


    val globalSpeed = 4.0
    const val MODIFIER_KEY: String = "block_break_speed"
    val blockMultipliers = HashMap<BlockRef, Double>()

    fun getMultiplier(block: Block): Double {
        var multiplier = globalSpeed
        val ref = block.asReference()
        if (blockMultipliers.containsKey(ref)) multiplier *= blockMultipliers[ref]!!
        return multiplier
    }
}