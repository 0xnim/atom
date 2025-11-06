package org.shotrush.atom.content.foragingage.items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.items.CustomItem;
import org.shotrush.atom.core.items.annotation.AutoRegister;

import java.util.Arrays;
import java.util.List;

@AutoRegister(priority = 4)
public class KnifeItem extends CustomItem {
    
    public KnifeItem(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getIdentifier() {
        return "knife";
    }
    
    @Override
    public Material getMaterial() {
        return Material.WOODEN_SWORD;
    }
    
    @Override
    public String getDisplayName() {
        return "§fFlint Knife";
    }
    
    @Override
    public List<String> getLore() {
        return Arrays.asList(
            "§7A sharp cutting tool",
            "§7Made from high quality flint",
            "§8• Tool",
            "§8[Foraging Age Tool]"
        );
    }

    @Override
    protected void applyCustomMeta(ItemMeta meta) {
        org.shotrush.atom.core.util.ItemUtil.setCustomModelName(meta, "flint_knife");
    }
    
    @Override
    public ItemStack create() {
        ItemStack item = super.create();

        Consumable consumable = Consumable.consumable()
            .consumeSeconds(10000.0f)
            .animation(ItemUseAnimation.BRUSH)
            .build();
        item.setData(DataComponentTypes.CONSUMABLE, consumable);
        
        return item;
    }
}
