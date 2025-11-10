package org.shotrush.atom.core.workstations;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class WorkstationData {
    private static final Map<String, org.shotrush.atom.core.workstations.WorkstationHandler<?>> handlers = new HashMap<>();
    
    @Getter
    private final Location blockLocation;
    @Getter
    private final String workstationType;
    @Getter
    private final List<PlacedItem> placedItems = new ArrayList<>();
    
    public static void registerHandler(String type, org.shotrush.atom.core.workstations.WorkstationHandler<?> handler) {
        handlers.put(type, handler);
    }
    
    public static org.shotrush.atom.core.workstations.WorkstationHandler<?> getHandler(String type) {
        return handlers.get(type);
    }
    
    public WorkstationData(Location blockLocation, String workstationType) {
        this.blockLocation = blockLocation;
        this.workstationType = workstationType;
    }
    
    public boolean placeItem(ItemStack item, Vector3f position, float yaw) {
        ItemStack singleItem = item.clone();
        singleItem.setAmount(1);
        PlacedItem placedItem = new PlacedItem(singleItem, position, yaw);
        placedItems.add(placedItem);
        spawnItemDisplay(placedItem);
        return true;
    }
    
    public ItemStack removeLastItem() {
        if (placedItems.isEmpty()) return null;
        PlacedItem item = placedItems.remove(placedItems.size() - 1);
        removeItemDisplay(item);
        return item.getItem();
    }
    
    public void clearAllItems() {
        for (PlacedItem item : new ArrayList<>(placedItems)) {
            removeItemDisplay(item);
        }
        placedItems.clear();
    }
    
    private void spawnItemDisplay(PlacedItem item) {
        if (blockLocation.getWorld() == null) return;
        
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
        Location displayLoc = center.clone().add(
            item.getPosition().x,
            item.getPosition().y,
            item.getPosition().z
        );
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(displayLoc, () -> {
            ItemDisplay display = (ItemDisplay) displayLoc.getWorld().spawnEntity(displayLoc, org.bukkit.entity.EntityType.ITEM_DISPLAY);
            display.setItemStack(item.getItem());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            
            
            String locationKey = "Workstation_" + workstationType + "_" + 
                blockLocation.getBlockX() + "_" + blockLocation.getBlockY() + "_" + blockLocation.getBlockZ();
            org.shotrush.atom.core.data.PersistentData.set(display, "workstation_id", locationKey);
            
            
            org.shotrush.atom.core.workstations.WorkstationHandler<?> handler = handlers.get(workstationType);
            org.joml.AxisAngle4f rotation = handler != null ? handler.getItemRotation() 
                : new org.joml.AxisAngle4f((float) Math.toRadians(90), 1, 0, 0);
            Vector3f scale = handler != null ? handler.getItemScale() 
                : new Vector3f(0.5f, 0.5f, 0.5f);
            
            org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),
                rotation,
                scale,
                new org.joml.AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transformation);
            display.setViewRange(64.0f);
            display.setShadowRadius(0.0f);
            display.setShadowStrength(0.0f);
            
            item.setDisplayUUID(display.getUniqueId());
            org.shotrush.atom.content.systems.ItemHeatSystem.startItemDisplayHeatTracking(display);
        });
    }
    
    private void removeItemDisplay(PlacedItem item) {
        if (blockLocation.getWorld() == null) return;
        
        
        String locationKey = "Workstation_" + workstationType + "_" + 
            blockLocation.getBlockX() + "_" + blockLocation.getBlockY() + "_" + blockLocation.getBlockZ();
        
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
        Vector3f pos = item.getPosition();
        Location expectedLoc = center.clone().add(pos.x, pos.y, pos.z);
        
        org.shotrush.atom.core.api.scheduler.SchedulerAPI.runTask(expectedLoc, () -> {
            
            for (Entity nearby : expectedLoc.getWorld().getNearbyEntities(expectedLoc, 0.5, 0.5, 0.5)) {
                if (nearby instanceof ItemDisplay display) {
                    String displayId = org.shotrush.atom.core.data.PersistentData.getString(display, "workstation_id", "");
                    if (locationKey.equals(displayId)) {
                        nearby.remove();
                        return; 
                    }
                }
            }
        });
    }
    
    public void removeAllDisplays() {
        if (blockLocation.getWorld() == null) return;
        
        
        String locationKey = "Workstation_" + workstationType + "_" + 
            blockLocation.getBlockX() + "_" + blockLocation.getBlockY() + "_" + blockLocation.getBlockZ();
        
        
        Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
        for (Entity entity : center.getWorld().getNearbyEntities(center, 2, 2, 2)) {
            if (entity instanceof ItemDisplay display) {
                String displayId = org.shotrush.atom.core.data.PersistentData.getString(display, "workstation_id", "");
                if (locationKey.equals(displayId)) {
                    entity.remove();
                }
            }
        }
    }
    
    public void respawnAllDisplays() {
        if (blockLocation.getWorld() == null) return;
        
        
        removeAllDisplays();
        
        
        for (PlacedItem item : placedItems) {
            spawnItemDisplay(item);
        }
    }
    
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(workstationType).append(";");
        sb.append(placedItems.size());
        
        for (PlacedItem item : placedItems) {
            try {
                String base64 = itemToBase64(item.getItem());
                sb.append(";").append(base64)
                    .append(",").append(item.getPosition().x)
                    .append(",").append(item.getPosition().y)
                    .append(",").append(item.getPosition().z)
                    .append(",").append(item.getYaw());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    
    public static WorkstationData deserialize(Location location, String data) {
        String[] parts = data.split(";");
        if (parts.length < 2) return null;
        
        String type = parts[0];
        WorkstationData workstation = new WorkstationData(location, type);
        
        try {
            int itemCount = Integer.parseInt(parts[1]);
            for (int i = 0; i < itemCount; i++) {
                int partIndex = 2 + i;
                if (partIndex >= parts.length) break;
                
                String[] itemData = parts[partIndex].split(",");
                String base64 = itemData[0];
                float x = Float.parseFloat(itemData[1]);
                float y = Float.parseFloat(itemData[2]);
                float z = Float.parseFloat(itemData[3]);
                float yaw = Float.parseFloat(itemData[4]);
                
                ItemStack item = itemFromBase64(base64);
                Vector3f position = new Vector3f(x, y, z);
                workstation.placedItems.add(new PlacedItem(item, position, yaw));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return workstation;
    }
    
    private static String itemToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    private static ItemStack itemFromBase64(String base64) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }
    
    public static class PlacedItem {
        @Getter
        private final ItemStack item;
        @Getter
        private final Vector3f position;
        @Getter
        private final float yaw;
        @Setter @Getter
        private java.util.UUID displayUUID;
        
        public PlacedItem(ItemStack item, Vector3f position, float yaw) {
            this.item = item;
            this.position = position;
            this.yaw = yaw;
        }
    }
}
