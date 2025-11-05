package org.shotrush.atom.content.foragingage.items;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 3)
public class SharpenedFlint extends CustomItem {
    
    public SharpenedFlint(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "sharpened_flint";
    }
    
    @Override
    public Material getMaterial() {
        return Material.FLINT;
    }
    
    @Override
    public String getDisplayName() {
        return "§fSharpened Flint";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A carefully knapped piece of flint",
            "§7Sharp enough to be useful",
            "§8• Crafting Material",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "sharpened_flint");
    }
    
    public void damageItem(ItemStack item, Player player) {
        if (item == null || item.getAmount() <= 0) return;
        
        int currentAmount = item.getAmount();
        
        if (Math.random() < 0.4) {
            item.setAmount(currentAmount - 1);
            
            if (currentAmount - 1 <= 0) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                org.shotrush.atom.core.ui.ActionBarManager.send(player, "§cYour Sharpened Flint broke!");
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.2f);
            }
        }
    }
}
