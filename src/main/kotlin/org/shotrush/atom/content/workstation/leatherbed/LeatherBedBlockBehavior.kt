package org.shotrush.atom.content.workstation.leatherbed

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.block.BlockBehavior
import net.momirealms.craftengine.core.block.CustomBlock
import net.momirealms.craftengine.core.block.ImmutableBlockState
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory
import net.momirealms.craftengine.core.block.entity.BlockEntity
import net.momirealms.craftengine.core.block.entity.BlockEntityType
import net.momirealms.craftengine.core.entity.player.InteractionResult
import net.momirealms.craftengine.core.item.context.UseOnContext
import net.momirealms.craftengine.core.util.Key
import net.momirealms.craftengine.core.world.BlockPos
import net.momirealms.craftengine.libraries.nbt.CompoundTag
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.joml.AxisAngle4f
import org.joml.Vector3f
import org.shotrush.atom.Atom
import org.shotrush.atom.content.foraging.items.SharpenedFlint
import org.shotrush.atom.content.workstation.Workstations
import org.shotrush.atom.content.workstation.core.InteractiveSurface
import org.shotrush.atom.core.api.player.PlayerDataAPI
import org.shotrush.atom.core.util.ActionBarManager
import org.shotrush.atom.content.workstation.core.PlacedItem
import org.shotrush.atom.content.workstation.core.WorkstationDataManager
import org.shotrush.atom.matches
import kotlinx.coroutines.*
import kotlin.random.Random


class LeatherBedBlockBehavior(
    block: CustomBlock
) : InteractiveSurface(block) {
    
    companion object {
        private val activeProcessing = mutableMapOf<Player, Job>()
        internal val stabilizingLeather = mutableMapOf<BlockPos, Job>()
        internal val curingStartTimes = mutableMapOf<BlockPos, Long>()
        internal var CURING_TIME_MS = 10 * 60 * 1000L
        
        object Factory : BlockBehaviorFactory {
            override fun create(
                block: CustomBlock,
                arguments: Map<String?, Any?>
            ): BlockBehavior = LeatherBedBlockBehavior(block)
        }
    }
    
    override fun <T : BlockEntity> blockEntityType(state: ImmutableBlockState): BlockEntityType<T> =
        @Suppress("UNCHECKED_CAST")
        Workstations.LEATHER_BED_ENTITY_TYPE as BlockEntityType<T>
    
    override fun createBlockEntity(
        pos: BlockPos,
        state: ImmutableBlockState
    ): BlockEntity = LeatherBedBlockEntity(pos, state)
    
    override fun getMaxItems(): Int = 1
    
    override fun canPlaceItem(item: ItemStack): Boolean {
        val itemId = CraftEngineItems.getCustomItemId(item)
        val isRawLeather = itemId != null && itemId.value().startsWith("animal_leather_raw_")
        val isCuredLeather = itemId != null && itemId.value().startsWith("animal_leather_cured_")
        
        return isRawLeather || isCuredLeather || item.type == Material.LEATHER
    }
    
    override fun calculatePlacement(player: Player, itemCount: Int): Vector3f {
        return Vector3f(-0.05f, 0.75f, 0.60f)
    }
    
    override fun getFullMessage(): String = "§cLeather bed is full!"
    
    override fun getEmptyMessage(): String = "§cPlace leather first!"
    
    override fun getItemDisplayRotation(item: PlacedItem): AxisAngle4f {
        
        return AxisAngle4f((Math.PI / 2).toFloat(), 0f, 0f, 0f)
    }
    
    override fun getItemDisplayScale(item: PlacedItem): Vector3f {
        
        return Vector3f(1f, 1f, 1f)
    }
    
    override fun useOnBlock(
        context: UseOnContext,
        state: ImmutableBlockState
    ): InteractionResult {
        val player = context.player?.platformPlayer() as? Player ?: return InteractionResult.PASS
        val item = context.item.item as? ItemStack ?: return InteractionResult.PASS
        
        
        val targetBlock = player.getTargetBlockExact(5)
        if (targetBlock == null) {
            Atom.instance?.logger?.warning("No target block found for LeatherBed")
            return InteractionResult.PASS
        }
        
        val pos = BlockPos(targetBlock.x, targetBlock.y, targetBlock.z)
        this.blockPos = pos
        
        
        val workstationData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
        
        
        placedItems.clear()
        placedItems.addAll(workstationData.placedItems)
        
        
        
        val location = player.getTargetBlockExact(5)?.location?.add(0.5, 0.5, 0.5)
        location?.let { loc ->
            loc.world?.getNearbyEntities(loc, 1.5, 1.5, 1.5)?.forEach { entity ->
                if (entity is org.bukkit.entity.ItemDisplay) {
                    
                    val belongsToUs = placedItems.any { it.displayUUID == entity.uniqueId }
                    if (!belongsToUs) {
                        
                        entity.remove()
                    }
                }
            }
        }
        
        
        placedItems.forEach { placedItem ->
            if (placedItem.displayUUID != null) {
                
                val entity = Bukkit.getEntity(placedItem.displayUUID!!)
                if (entity == null) {
                    
                    placedItem.displayUUID = null
                    spawnItemDisplay(placedItem)
                }
            } else {
                
                spawnItemDisplay(placedItem)
            }
        }
        
        
        if (isScrapingTool(item)) {
            if (placedItems.isEmpty()) {
                ActionBarManager.send(player, getEmptyMessage())
                return InteractionResult.SUCCESS
            }
            
            
            if (!isProcessing(player)) {
                startScraping(player, item)
            }
            return InteractionResult.SUCCESS
        }
        
        
        
        val itemTypeBeforePlacement = item.type
        val result = super.useOnBlock(context, state)
        
        
        if (result == InteractionResult.SUCCESS && blockPos != null) {
            WorkstationDataManager.updatePlacedItems(blockPos!!, placedItems)
            WorkstationDataManager.saveData() 
            
            
            val placedItem = placedItems.lastOrNull()?.item
            Atom.instance?.logger?.info("Item placed on leather bed: original=${itemTypeBeforePlacement}, placed=${placedItem?.type}, is vanilla leather: ${placedItem?.type == Material.LEATHER}")
            
            
            if (placedItem?.type == Material.LEATHER) {
                Atom.instance?.logger?.info("Vanilla leather detected, starting stabilization at $pos")
                startStabilization(pos)
            }
        }
        
        return result
    }
    
    
    override fun onCrouchRightClick(player: Player): Boolean {
        
        if (placedItems.isNotEmpty()) {
            val items = placedItems.toList()
            placedItems.clear()
            
            Atom.instance?.logger?.info("Dropping ${items.size} items from leather bed")
            
            items.forEach { placedItem ->
                removeItemDisplay(placedItem)
                blockPos?.let { pos ->
                    val location = Location(
                        player.world,
                        pos.x().toDouble() + 0.5,
                        pos.y().toDouble() + 0.5,
                        pos.z().toDouble() + 0.5
                    )
                    
                    
                    val customId = CraftEngineItems.getCustomItemId(placedItem.item)
                    Atom.instance?.logger?.info("Item type: ${placedItem.item.type}, customId: ${customId?.value()}")
                    
                    val itemToDrop = if (placedItem.item.type == Material.LEATHER && customId != null) {
                        if (customId.value().contains("animal_leather_cured")) {
                            
                            val rebuilt = CraftEngineItems.byId(customId)?.buildItemStack()
                            Atom.instance?.logger?.info("Rebuilding cured leather: ${customId.value()}, success: ${rebuilt != null}")
                            rebuilt ?: placedItem.item
                        } else {
                            placedItem.item
                        }
                    } else {
                        placedItem.item
                    }
                    
                    player.world.dropItemNaturally(location, itemToDrop)
                    Atom.instance?.logger?.info("Dropped item: ${itemToDrop.type}, amount: ${itemToDrop.amount}")
                }
            }
        } else {
            ActionBarManager.send(player, getEmptyMessage())
        }
        return true
    }
    
    private fun isScrapingTool(item: ItemStack): Boolean {
        return item.matches("atom:sharpened_flint") || item.matches("atom:knife")
    }
    
    private fun isProcessing(player: Player): Boolean {
        return activeProcessing.containsKey(player)
    }
    
    private fun startScraping(player: Player, tool: ItemStack) {
        
        activeProcessing[player]?.cancel()
        
        
        val job = GlobalScope.launch {
            val strokeCount = 20 + Random.nextInt(11)
            var currentStroke = 0
            
            ActionBarManager.sendStatus(player, "§7Scraping leather... Use the tool carefully")
            
            while (currentStroke < strokeCount && isActive) {
                delay(250) 
                
                
                if (!player.isHandRaised || !isScrapingTool(player.activeItem)) {
                    ActionBarManager.sendStatus(player, "§cScraping cancelled - tool lowered")
                    delay(1000) 
                    break
                }
                
                if (!player.isOnline || player.location.distance(getBlockLocation()) > 5.0) {
                    break
                }
                
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    playScrapingEffects(player)
                }, null)
                
                currentStroke++
                
                
                val progress = (currentStroke.toFloat() / strokeCount * 100).toInt()
                ActionBarManager.sendStatus(player, "§7Scraping leather... §e$progress%")
            }
            
            if (currentStroke >= strokeCount) {
                
                player.scheduler.run(Atom.instance!!, { _ ->
                    finishScraping(player, tool)
                }, null)
            }
            
            activeProcessing.remove(player)
            ActionBarManager.clearStatus(player)
        }
        
        activeProcessing[player] = job
    }
    
    private fun playScrapingEffects(player: Player) {
        val location = getBlockLocation().add(0.5, 1.0, 0.5)
        
        
        location.world?.playSound(location, Sound.ITEM_BRUSH_BRUSHING_GENERIC, 1.0f, 1.0f)
        
        
        val dustOptions = Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.0f)
        location.world?.spawnParticle(
            Particle.DUST,
            location,
            10,
            0.2,
            0.2,
            0.2,
            0.0,
            dustOptions
        )
    }
    
    private fun finishScraping(player: Player, tool: ItemStack) {
        val location = getBlockLocation().add(0.5, 0.5, 0.5)
        
        
        val scrapedItem = placedItems.lastOrNull()?.item
        val itemId = scrapedItem?.let { CraftEngineItems.getCustomItemId(it) }
        
        
        removeLastItem()
        
        blockPos?.let { WorkstationDataManager.updatePlacedItems(it, placedItems) }
        
        
        if (itemId != null && itemId.value().startsWith("animal_leather_raw_")) {
            
            val animalType = itemId.value().removePrefix("animal_leather_raw_")
            
            
            val meatId = "atom:animal_meat_raw_$animalType"
            CraftEngineItems.byId(Key.of(meatId))?.let { rawMeatItem ->
                val rawMeat = rawMeatItem.buildItemStack()
                location.world?.dropItemNaturally(location, rawMeat)
            }
            
            
            val leather = ItemStack(Material.LEATHER)
            location.world?.dropItemNaturally(location, leather)
        } else if (itemId != null && itemId.value().startsWith("animal_leather_cured_")) {
            
            location.world?.dropItemNaturally(location, scrapedItem)
        } else if (scrapedItem?.type == Material.LEATHER) {
            
            location.world?.dropItemNaturally(location, scrapedItem)
        }
        
        
        if (SharpenedFlint.isSharpenedFlint(tool)) {
            SharpenedFlint.damageItem(tool, player, 0.3)
        }
        
        
        player.playSound(player.location, Sound.BLOCK_WOOL_BREAK, 1.0f, 1.0f)
        ActionBarManager.send(player, "§aScraped the leather successfully!")
        
        
        PlayerDataAPI.incrementInt(player, "leather_scraping.count", 0)
    }
    
    private fun getBlockLocation(): Location {
        return blockPos?.let { pos ->
            Location(
                Bukkit.getWorld("world"), 
                pos.x().toDouble(),
                pos.y().toDouble(),
                pos.z().toDouble()
            )
        } ?: Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)
    }
    
    override fun onRemoved() {
        
        activeProcessing.values.forEach { it.cancel() }
        activeProcessing.clear()
        
        
        blockPos?.let { pos ->
            stabilizingLeather[pos]?.cancel()
            stabilizingLeather.remove(pos)
            curingStartTimes.remove(pos)
            
            
        }
        
        super.onRemoved()
    }
    
    private fun startStabilization(pos: BlockPos) {
        
        stabilizingLeather[pos]?.cancel()
        
        
        val workstationData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
        val currentItem = workstationData.placedItems.lastOrNull()
        
        
        if (currentItem == null || currentItem.item.type != Material.LEATHER) {
            Atom.instance?.logger?.info("StartStabilization: No vanilla leather found at $pos")
            return
        }
        
        Atom.instance?.logger?.info("Starting leather stabilization at $pos")
        
        
        val startTime = System.currentTimeMillis()
        curingStartTimes[pos] = startTime
        workstationData.curingStartTime = startTime
        WorkstationDataManager.saveData()
        
        
        val job = GlobalScope.launch {
            delay(CURING_TIME_MS)
            
            
            val updatedData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
            val oldLeatherItem = updatedData.placedItems.lastOrNull()
            
            if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                Atom.instance?.logger?.info("Completing leather stabilization at $pos")
                
                
                val animals = listOf("cow", "pig", "sheep", "chicken", "rabbit", "horse", "donkey", "mule", "llama", "goat", "cat", "wolf", "fox", "panda", "polar_bear", "ocelot", "camel")
                val randomAnimal = animals.random()
                val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"
                
                CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->
                    
                    val curedLeather = curedItem.buildItemStack()
                    
                    
                    updatedData.placedItems.clear()
                    val newPlacedItem = PlacedItem(
                        item = curedLeather,
                        position = oldLeatherItem.position,
                        yaw = oldLeatherItem.yaw,
                        displayUUID = oldLeatherItem.displayUUID
                    )
                    updatedData.placedItems.add(newPlacedItem)
                    
                    
                    newPlacedItem.displayUUID?.let { uuid ->
                        (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->
                            
                            Bukkit.getScheduler().runTask(Atom.instance!!, Runnable {
                                display.setItemStack(curedLeather)
                                
                                display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                                display.location.world?.spawnParticle(Particle.HAPPY_VILLAGER, display.location, 10, 0.3, 0.3, 0.3, 0.0)
                            })
                        }
                    }
                    
                    
                    WorkstationDataManager.updatePlacedItems(pos, updatedData.placedItems)
                    WorkstationDataManager.saveData()
                    
                    Atom.instance?.logger?.info("Leather cured successfully at $pos to $curedLeatherId")
                } ?: Atom.instance?.logger?.warning("Failed to find cured leather item: $curedLeatherId")
            } else {
                Atom.instance?.logger?.info("Leather no longer present at $pos, cancelling stabilization")
            }
            
            stabilizingLeather.remove(pos)
            curingStartTimes.remove(pos)
            
            
            val data = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
            data.curingStartTime = null
            WorkstationDataManager.saveData()
        }
        
        stabilizingLeather[pos] = job
    }
}


fun LeatherBedBlockBehavior.Companion.accelerateCuring(pos: BlockPos): Boolean {
    val job = stabilizingLeather[pos]
    if (job == null) {
        Atom.instance?.logger?.info("No stabilization job found for $pos")
        return false
    }
    
    
    job.cancel()
    stabilizingLeather.remove(pos)
    curingStartTimes.remove(pos)
    
    
    val workstationData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
    val oldLeatherItem = workstationData.placedItems.lastOrNull()
    
    Atom.instance?.logger?.info("Accelerating cure at $pos, item type: ${oldLeatherItem?.item?.type}")
    
    if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
        
        val animals = listOf("cow", "pig", "sheep", "chicken", "rabbit", "horse", "donkey", "mule", "llama", "goat", "cat", "wolf", "fox", "panda", "polar_bear", "ocelot", "camel")
        val randomAnimal = animals.random()
        val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"
        
        CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->
            val curedLeather = curedItem.buildItemStack()
            
            
            workstationData.placedItems.clear()
            val newPlacedItem = PlacedItem(
                item = curedLeather,
                position = oldLeatherItem.position,
                yaw = oldLeatherItem.yaw,
                displayUUID = oldLeatherItem.displayUUID
            )
            workstationData.placedItems.add(newPlacedItem)
            
            
            newPlacedItem.displayUUID?.let { uuid ->
                (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->
                    display.setItemStack(curedLeather)
                    
                    display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                    display.location.world?.spawnParticle(Particle.HAPPY_VILLAGER, display.location, 10, 0.3, 0.3, 0.3, 0.0)
                }
            }
            
            
            WorkstationDataManager.updatePlacedItems(pos, workstationData.placedItems)
            WorkstationDataManager.saveData()
            return true
        }
    }
    return false
}

fun LeatherBedBlockBehavior.Companion.getCuringTimeRemaining(pos: BlockPos): Long? {
    if (!stabilizingLeather.containsKey(pos)) return null
    val startTime = curingStartTimes[pos] ?: return null
    val elapsed = System.currentTimeMillis() - startTime
    val remaining = CURING_TIME_MS - elapsed
    return if (remaining > 0) remaining else 0
}

fun LeatherBedBlockBehavior.Companion.setCuringTime(timeMs: Long) {
    CURING_TIME_MS = timeMs
}


fun LeatherBedBlockBehavior.Companion.resumeCuringProcesses() {
    Atom.instance?.logger?.info("Resuming leather curing processes...")
    
    
    WorkstationDataManager.getAllWorkstations().forEach { (key, data) ->
        if (data.type == "leather_bed" && data.curingStartTime != null) {
            val pos = data.position
            val elapsedTime = System.currentTimeMillis() - data.curingStartTime!!
            val remainingTime = CURING_TIME_MS - elapsedTime
            
            if (remainingTime > 0) {
                
                Atom.instance?.logger?.info("Resuming curing at $pos with ${remainingTime / 1000}s remaining")
                curingStartTimes[pos] = data.curingStartTime!!
                
                val job = GlobalScope.launch {
                    delay(remainingTime)
                    
                    
                    val updatedData = WorkstationDataManager.getWorkstationData(pos, "leather_bed")
                    val oldLeatherItem = updatedData.placedItems.lastOrNull()
                    
                    if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                        completeCuring(pos, updatedData, oldLeatherItem)
                    }
                    
                    stabilizingLeather.remove(pos)
                    curingStartTimes.remove(pos)
                    updatedData.curingStartTime = null
                    WorkstationDataManager.saveData()
                }
                
                stabilizingLeather[pos] = job
            } else {
                
                Atom.instance?.logger?.info("Completing overdue curing at $pos")
                val oldLeatherItem = data.placedItems.lastOrNull()
                if (oldLeatherItem != null && oldLeatherItem.item.type == Material.LEATHER) {
                    completeCuring(pos, data, oldLeatherItem)
                }
                data.curingStartTime = null
                WorkstationDataManager.saveData()
            }
        }
    }
}

private fun LeatherBedBlockBehavior.Companion.completeCuring(
    pos: BlockPos,
    workstationData: WorkstationDataManager.WorkstationData,
    oldLeatherItem: PlacedItem
) {
    val animals = listOf("cow", "pig", "sheep", "chicken", "rabbit", "horse", "donkey", "mule", "llama", "goat", "cat", "wolf", "fox", "panda", "polar_bear", "ocelot", "camel")
    val randomAnimal = animals.random()
    val curedLeatherId = "atom:animal_leather_cured_$randomAnimal"
    
    CraftEngineItems.byId(Key.of(curedLeatherId))?.let { curedItem ->
        val curedLeather = curedItem.buildItemStack()
        
        
        workstationData.placedItems.clear()
        val newPlacedItem = PlacedItem(
            item = curedLeather,
            position = oldLeatherItem.position,
            yaw = oldLeatherItem.yaw,
            displayUUID = oldLeatherItem.displayUUID
        )
        workstationData.placedItems.add(newPlacedItem)
        
        
        newPlacedItem.displayUUID?.let { uuid ->
            (Bukkit.getEntity(uuid) as? org.bukkit.entity.ItemDisplay)?.let { display ->
                
                Bukkit.getScheduler().runTask(Atom.instance!!, Runnable {
                    display.setItemStack(curedLeather)
                    
                    display.location.world?.playSound(display.location, Sound.ITEM_TRIDENT_HIT, 1.0f, 1.5f)
                    display.location.world?.spawnParticle(Particle.HAPPY_VILLAGER, display.location, 10, 0.3, 0.3, 0.3, 0.0)
                })
            }
        }
        
        
        WorkstationDataManager.updatePlacedItems(pos, workstationData.placedItems)
        WorkstationDataManager.saveData()
        
        Atom.instance?.logger?.info("Leather cured successfully at $pos to $curedLeatherId")
    }
}


class LeatherBedBlockEntity(
    pos: BlockPos,
    blockState: ImmutableBlockState
) : BlockEntity(Workstations.LEATHER_BED_ENTITY_TYPE, pos, blockState) {
    
    init {
        Atom.instance?.logger?.info("LeatherBedBlockEntity initialized at $pos")
    }
    
    override fun loadCustomData(tag: CompoundTag) {
        super.loadCustomData(tag)
    }
    
    override fun saveCustomData(tag: CompoundTag) {
        super.saveCustomData(tag)
    }
}
