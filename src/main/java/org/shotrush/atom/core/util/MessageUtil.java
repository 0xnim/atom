package org.shotrush.atom.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class MessageUtil {
    
    public static void send(Player player, String message) {
        send(player, message, MessageType.ACTION_BAR);
    }
    
    public static void send(Player player, String message, MessageType type) {
        switch (type) {
            case CHAT:
                player.sendMessage(message);
                break;
            case ACTION_BAR:
                player.sendActionBar(Component.text(message));
                break;
            case TITLE:
                player.showTitle(Title.title(
                    Component.text(message),
                    Component.empty(),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                    )
                ));
                break;
            case SUBTITLE:
                player.showTitle(Title.title(
                    Component.empty(),
                    Component.text(message),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                    )
                ));
                break;
        }
    }
    
    public static void send(Player player, String message, MessageType type, long fadeIn, long stay, long fadeOut) {
        if (type == MessageType.TITLE || type == MessageType.SUBTITLE) {
            Component titleComponent = type == MessageType.TITLE ? Component.text(message) : Component.empty();
            Component subtitleComponent = type == MessageType.SUBTITLE ? Component.text(message) : Component.empty();
            
            player.showTitle(Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                    Duration.ofMillis(fadeIn),
                    Duration.ofMillis(stay),
                    Duration.ofMillis(fadeOut)
                )
            ));
        } else {
            send(player, message, type);
        }
    }
    
    public enum MessageType {
        CHAT,
        ACTION_BAR,
        TITLE,
        SUBTITLE
    }
}
