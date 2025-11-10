package org.shotrush.atom.content.workstation

import net.momirealms.craftengine.core.block.behavior.BlockBehaviors
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityTypes
import net.momirealms.craftengine.core.util.Key
import org.shotrush.atom.content.workstation.knapping.KnappingBlockBehavior

object Workstations {
    val KNAPPING_STATION_KEY = Key.of("atom:knapping_station")
    val KNAPPING_STATION_BEHAVIOR = BlockBehaviors.register(KNAPPING_STATION_KEY, KnappingBlockBehavior.Factory)
    val KNAPPING_STATION_ENTITY_TYPE = BlockEntityTypes.register<BlockEntity>(KNAPPING_STATION_KEY)

    fun init() = Unit
}