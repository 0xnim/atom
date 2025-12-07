package org.shotrush.atom.commands

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.*
import org.joml.Vector3i
import org.shotrush.atom.Atom
import org.shotrush.atom.sendMiniMessage
import org.shotrush.atom.systems.room.RoomRegistry
import org.shotrush.atom.systems.room.RoomScanner

object RoomCommands {
    fun register() {
        commandTree("room") {
            literalArgument("current", true) {
                withPermission("atom.command.room.current")
                playerExecutor { player, arguments ->
                    val room = RoomRegistry.roomAt(player.location)
                    if (room == null) {
                        player.sendMiniMessage("<red>You are not in a room</red>")
                    } else {
                        player.sendMiniMessage("<green>You are in room ${room.id}</green>")
                    }
                }
            }
            literalArgument("scan") {
                withPermission("atom.command.room.scan")
                integerArgument("maxVolume", 10, Short.MAX_VALUE.toInt(), true) {
                    playerExecutor { player, arguments ->
                        val maxVolume = arguments["maxVolume"] as? Int ?: 800
                        Atom.instance.launch(Atom.instance.entityDispatcher(player)) {
                            player.sendMiniMessage("<green>Scanning for rooms...</green>")
                            val scan = RoomScanner.scanAt(
                                player.world,
                                Vector3i(player.location.blockX, player.location.blockY, player.location.blockZ),
                                maxVolume = maxVolume,
                                retries = 0
                            )
                            player.sendMiniMessage(
                                "<green>Scanned room ${scan?.id ?: "<red>failed</red>"}</green>"
                            )
                        }
                    }
                }
            }
            literalArgument("save") {
                withPermission("atom.command.room.save")
                anyExecutor { executor, arguments ->
                    Atom.instance.launch {
                        executor.sendMiniMessage("<green>Saving rooms...</green>")
                        try {
                            RoomRegistry.saveAllToDisk()
                            executor.sendMiniMessage("<green>Saved rooms</green>")
                        } catch (e: Exception) {
                            executor.sendMiniMessage("<red>Failed to save rooms</red>")
                            e.printStackTrace()
                        }
                    }
                }
            }
            literalArgument("load") {
                withPermission("atom.command.room.load")
                anyExecutor { executor, arguments ->
                    Atom.instance.launch {
                        executor.sendMiniMessage("<green>Loading rooms...</green>")
                        try {
                            RoomRegistry.readAllFromDisk()
                            executor.sendMiniMessage("<green>Loaded rooms</green>")
                        } catch (e: Exception) {
                            executor.sendMiniMessage("<red>Failed to load rooms</red>")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}