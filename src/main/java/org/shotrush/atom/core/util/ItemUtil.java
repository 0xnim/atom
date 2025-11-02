package org.shotrush.atom.core.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtil {
    
    public static ItemStack createItemWithCustomModel(Material material, String modelName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            org.bukkit.inventory.meta.components.CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setStrings(java.util.List.of(modelName));
            meta.setCustomModelDataComponent(component);
            item.setItemMeta(meta);
        }
        return item;
    }
}
