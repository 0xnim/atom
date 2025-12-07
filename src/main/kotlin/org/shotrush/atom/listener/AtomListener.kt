package org.shotrush.atom.listener

import org.bukkit.event.Event
import org.bukkit.event.Listener
import kotlin.coroutines.CoroutineContext

typealias EventClass = Class<out Event>
typealias EventRunner = (event: Event) -> CoroutineContext

interface AtomListener : Listener {
    val eventDefs: Map<EventClass, EventRunner>
}

inline fun <reified T : Event> eventDef(noinline runner: (event: T) -> CoroutineContext): Pair<EventClass, EventRunner> {
    return (T::class.java to runner) as Pair<EventClass, EventRunner>
}
