package org.shotrush.atom.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;
import org.shotrush.atom.commands.annotation.AutoRegister;
import org.shotrush.atom.content.foragingage.workstations.leatherbed.LeatherBedHandler;

@AutoRegister(priority = 34)
@CommandAlias("leatherbed|leatherrack")
@Description("Get a leather drying bed")
public class LeatherBedCommand extends BaseCommand {

    @Default
    @CommandPermission("atom.leatherbed")
    public void onLeatherBed(Player player) {
        Atom.getInstance().getBlockManager().giveBlockItem(player, "leather_bed");
    }
    
    @Subcommand("finishcuring|cure")
    @CommandPermission("atom.leatherbed.admin")
    @Description("Instantly finish curing leather on the nearest leather bed")
    public void onFinishCuring(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        
        if (targetBlock == null) {
            player.sendMessage("§cNo block in range! Look at a leather bed.");
            return;
        }
        
        Location blockLoc = targetBlock.getLocation();
        boolean finished = LeatherBedHandler.finishCuringNearby(blockLoc);
        
        if (finished) {
            player.sendMessage("§aLeather curing completed instantly!");
        } else {
            player.sendMessage("§cNo curing leather found nearby. Make sure there's scraped leather on the bed.");
        }
    }
}
