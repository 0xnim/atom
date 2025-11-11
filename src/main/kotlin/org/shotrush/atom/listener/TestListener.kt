package org.shotrush.atom.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.registerSuspendingEvents
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.shotrush.atom.Atom
import org.shotrush.atom.item.Molds
import kotlin.coroutines.CoroutineContext

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext): Pair<Class<out Event>, (event: Event) -> CoroutineContext> {
    return (T::class.java to runner) as Pair<Class<out Event>, (event: Event) -> CoroutineContext>
}

object TestListener : Listener {
    fun register(atom: Atom) {
        val eventDispatcher = mapOf(eventDef<PlayerInteractEvent> {
            atom.entityDispatcher(it.player)
        })
        atom.server.pluginManager.registerSuspendingEvents(this, atom, eventDispatcher)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (Molds.isFilledMold(item)) {
            val (mold, head) = Molds.emptyMold(item)
            event.player.inventory.remove(item.clone().apply { amount = 1 })
            event.player.inventory.addItem(mold)
            event.player.inventory.addItem(head)
        }
    }
}