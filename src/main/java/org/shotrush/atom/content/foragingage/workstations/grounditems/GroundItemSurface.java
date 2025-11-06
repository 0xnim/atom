package org.shotrush.atom.content.foragingage.workstations.grounditems;

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
import org.shotrush.atom.core.util.ActionBarManager;


@AutoRegister(priority = 30)
public class GroundItemSurface extends InteractiveSurface {

    public GroundItemSurface(Location spawnLocation, Location blockLocation, BlockFace blockFace) {
        super(spawnLocation, blockLocation, blockFace);
    }

    public GroundItemSurface(Location spawnLocation, BlockFace blockFace) {
        super(spawnLocation, blockFace);
    }

    @Override
    public int getMaxItems() {
        return 2;
    }

    @Override
    public boolean canPlaceItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    @Override
    public Vector3f calculatePlacement(Player player, int itemCount) {
        
        if (itemCount == 0) {
            return new Vector3f(-0.2f, 0.01f, 0.2f);
        } else {
            return new Vector3f(0.2f, 0.01f, -0.2f);
        }
    }

    @Override
    protected void cleanupExistingEntities() {
        if (spawnLocation.getWorld() == null) return;
        
        for (org.bukkit.entity.Entity entity : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 1.0, 1.0, 1.0)) {
            if (entity instanceof ItemDisplay display) {
                if (interactionUUID != null) {
                    String parentId = org.shotrush.atom.core.data.PersistentData.getString(
                        display,
                        "parent_workstation",
                        null
                    );
                    if (interactionUUID.toString().equals(parentId)) {
                        entity.remove();
                    }
                } else {
                    if (entity.getLocation().distance(spawnLocation) < 0.5) {
                        entity.remove();
                    }
                }
            } else if (entity instanceof org.bukkit.entity.Interaction) {
                if (entity.getLocation().distance(spawnLocation) < 0.1) {
                    entity.remove();
                }
            }
        }
    }

    @Override
    public void spawn(Atom plugin, RegionAccessor accessor) {
        cleanupExistingEntities();
        
        ItemDisplay display = (ItemDisplay) accessor.spawnEntity(spawnLocation, EntityType.ITEM_DISPLAY);
        
        spawnDisplay(display, plugin, new ItemStack(Material.AIR), 
            new Vector3f(0, 0, 0), 
            new AxisAngle4f(0, 0, 0, 1), 
            new Vector3f(1f, 1f, 1f), 
            false, 1.0f, 0.3f);
    }

    @Override
    protected AxisAngle4f getItemDisplayRotation(PlacedItem item) {
        float yaw = item.getYaw();
        AxisAngle4f yawRot = new AxisAngle4f(yaw, 0, 1, 0);
        AxisAngle4f flatRot = new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
        return org.shotrush.atom.core.blocks.util.BlockRotationUtil.combineRotations(yawRot, flatRot);
    }
    
    @Override
    protected Vector3f getItemDisplayScale(PlacedItem item) {
        return new Vector3f(0.5f, 0.5f, 0.5f);
    }

    @Override
    public boolean onInteract(Player player, boolean sneaking) {
        
        return false;
    }

    @Override
    public boolean onWrenchInteract(Player player, boolean sneaking) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        
        if (hand.getType() == Material.AIR) {
            return false; 
        }

        if (!canPlaceItem(hand)) {
            return false; 
        }

        if (placedItems.size() >= getMaxItems()) {
            ActionBarManager.send(player, "§cFull (max " + getMaxItems() + " items)");
            return false;
        }

        float randomYaw = (float) (Math.random() * Math.PI * 2);
        if (placeItem(player, hand, calculatePlacement(player, placedItems.size()), randomYaw)) {
            hand.setAmount(hand.getAmount() - 1);
            ActionBarManager.send(player, "§aItem placed (" + placedItems.size() + "/" + getMaxItems() + ")");
            return true;
        }
        
        return false;
    }

    @Override
    protected void removeEntities() {
        
        for (PlacedItem item : placedItems) {
            removeItemDisplay(item);
            if (getSpawnLocation().getWorld() != null) {
                getSpawnLocation().getWorld().dropItemNaturally(getSpawnLocation(), item.getItem());
            }
        }
        
        
        super.removeEntities();
    }

    @Override
    public void update(float globalAngle) {
    }

    @Override
    public String getIdentifier() {
        return "ground_items";
    }

    @Override
    public String getDisplayName() {
        return "§7Ground Items";
    }

    @Override
    public Material getItemMaterial() {
        return Material.AIR;
    }

    @Override
    public String[] getLore() {
        return new String[]{
            "§7Items on the ground",
            "§8Sneak + right-click with item to place",
            "§8Left-click item to pick it up",
            "§8Break to remove all items"
        };
    }
    
    
    public boolean hasItems() {
        return !placedItems.isEmpty();
    }
    
    public boolean removeItemByUUID(java.util.UUID displayUUID) {
        for (int i = 0; i < placedItems.size(); i++) {
            PlacedItem item = placedItems.get(i);
            if (item.getDisplayUUID() != null && item.getDisplayUUID().equals(displayUUID)) {
                ItemStack removed = item.getItem().clone();
                removeItemDisplay(item);
                placedItems.remove(i);
                
                
                if (getSpawnLocation().getWorld() != null) {
                    getSpawnLocation().getWorld().dropItemNaturally(getSpawnLocation(), removed);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public org.shotrush.atom.core.blocks.CustomBlock deserialize(String data) {
        Object[] parsed = parseDeserializeData(data);
        if (parsed == null) return null;
        
        GroundItemSurface surface = new GroundItemSurface((Location) parsed[1], (BlockFace) parsed[2]);
        String[] parts = data.split(";");
        surface.deserializeAdditionalData(parts, 5);
        
        return surface;
    }
}
