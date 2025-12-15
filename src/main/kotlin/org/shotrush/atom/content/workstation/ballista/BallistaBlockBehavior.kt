package org.shotrush.atom.content.workstation.ballista

import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.shotrush.atom.blocks.AtomBlock
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.item.Bolts

class BallistaBlockBehavior(block: CustomBlock) : AtomBlock<BallistaBlockEntity>(
    block,
    Workstations.BALLISTA.type,
    ::BallistaBlockEntity,
    BallistaBlockEntity::tick
) {
    object Factory : BlockBehaviorFactory {
        override fun create(
            block: CustomBlock,
            arguments: Map<String?, Any?>,
        ): BlockBehavior = BallistaBlockBehavior(block)
    }

    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState,
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item as? ItemStack ?: return InteractionResult.PASS
        val pos = context.clickedPos

        val blockEntity = context.level.storageWorld().getBlockEntityAtIfLoaded(pos)
        if (blockEntity !is BallistaBlockEntity) return InteractionResult.PASS

        // If holding a bolt, try to load it (works whether mounted or not)
        if (Bolts.isBolt(item)) {
            val boltType = Bolts.getBoltType(item) ?: return InteractionResult.PASS

            if (blockEntity.loadedBolt != null) {
                ActionBarManager.send(player, "<red>Ballista already loaded!</red>")
                return InteractionResult.FAIL
            }

            if (blockEntity.loadBolt(boltType)) {
                item.amount--
                ActionBarManager.send(player, "<green>Loaded ${boltType.name.lowercase().replaceFirstChar { it.uppercase() }} bolt</green>")
                player.world.playSound(player.location, Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.5f)
                return InteractionResult.SUCCESS
            }
            return InteractionResult.FAIL
        }

        // Empty hand: mount or dismount
        if (item.isEmpty || item.type == org.bukkit.Material.AIR) {
            // If player is already mounted on THIS ballista, dismount
            if (blockEntity.mountedPlayerUUID == player.uniqueId) {
                blockEntity.dismount()
                BallistaListener.trackDismount(player)
                ActionBarManager.send(player, "<yellow>Dismounted ballista</yellow>")
                return InteractionResult.SUCCESS
            }

            // If another player is mounted
            if (blockEntity.isMounted()) {
                ActionBarManager.send(player, "<red>Ballista is in use!</red>")
                return InteractionResult.FAIL
            }

            // Mount the player
            if (blockEntity.mount(player)) {
                BallistaListener.trackMount(player, blockEntity)
                if (blockEntity.loadedBolt != null) {
                    ActionBarManager.send(player, "<green>Mounted ballista!</green> <gray>Left-click to wind and fire, sneak to dismount</gray>")
                } else {
                    ActionBarManager.send(player, "<green>Mounted ballista!</green> <gray>Right-click with bolt to load, sneak to dismount</gray>")
                }
                return InteractionResult.SUCCESS
            }
            return InteractionResult.FAIL
        }

        return InteractionResult.PASS
    }
}
