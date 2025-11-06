package org.shotrush.atom.content.foragingage.workstations.craftingbasket;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.shotrush.atom.Atom;
import org.shotrush.atom.core.blocks.InteractiveSurface;
import org.shotrush.atom.core.blocks.annotation.AutoRegister;

@AutoRegister(priority = 33)
public class CraftingBasket extends InteractiveSurface {

    public CraftingBasket(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public CraftingBasket(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    protected boolean useGuiMode() {
        return true; 
    }
    
    
    @Override
    public int getMaxItems() {
        return 0;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        return false;
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        return new Vector3f(0, 0, 0);
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        ItemStack basketItem = createItemWithCustomModel(Material.STONE_BUTTON, "crafting_basket");

        spawnDisplay(display, plugin, basketItem, new Vector3f(0, 0.5f, 0), new AxisAngle4f(), new Vector3f(1f, 1f, 1f), false, 1f, 0.2f);
    }


    @Override
    public void update(float globalAngle) {
        
    }

    @Override
    public String getIdentifier() {
        return "crafting_basket";
    }

    @Override
    public String getDisplayName() {
        return "§eCrafting Basket";
    }

    @Override
    public Material getItemMaterial() {
        return Material.STONE_BUTTON;
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "§7A basket for crafting items",
                "§8Right-click to open crafting GUI"
        };
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) {
            return null;
        }

        CraftingBasket basket = new CraftingBasket((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        basket.deserializeAdditionalData(parts, 5);

        return basket;
    }
}
