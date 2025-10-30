package org.shotrush.atom.listener;

import org.bukkit.util.BoundingBox;
import org.shotrush.atom.Atom;
import org.shotrush.atom.display.DisplayGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DisplayRotationCollisionListener {
    
    private final Map<UUID, RotationData> rotatingGroups = new HashMap<>();
    private final Map<UUID, Set<UUID>> collidingPairs = new HashMap<>();
    
    public void registerRotatingGroup(UUID groupId, float degreesPerSecond, String axis) {
        rotatingGroups.put(groupId, new RotationData(degreesPerSecond, axis));
    }
    
    public void unregisterRotatingGroup(UUID groupId) {
        if (!rotatingGroups.containsKey(groupId)) return;
        
        rotatingGroups.remove(groupId);
        
        Set<UUID> connectedGroups = collidingPairs.remove(groupId);
        if (connectedGroups != null) {
            var dm = Atom.getInstance().getDisplayManager();
            for (UUID connectedId : connectedGroups) {
                if (rotatingGroups.containsKey(connectedId)) {
                    rotatingGroups.remove(connectedId);
                    DisplayGroup group = dm.getDisplayGroups().getIfPresent(connectedId);
                    if (group != null && group.isRotating()) {
                        group.stopContinuousRotation();
                    }
                }
                Set<UUID> connectedPairs = collidingPairs.get(connectedId);
                if (connectedPairs != null) {
                    connectedPairs.remove(groupId);
                }
            }
        }
    }
    
    public void checkCollisionsForGroup(DisplayGroup group1) {
        RotationData rotation1 = rotatingGroups.get(group1.getId());
        if (rotation1 == null || !group1.isRotating()) return;
        
        if (group1.getDisplays().isEmpty() || group1.getDisplays().stream().allMatch(d -> d.isDead())) {
            unregisterRotatingGroup(group1.getId());
            return;
        }
        
        BoundingBox box1 = getGroupBoundingBox(group1);
        if (box1 == null) return;
        
        var dm = Atom.getInstance().getDisplayManager();
        for (DisplayGroup group2 : dm.getDisplayGroups().asMap().values()) {
            if (group1.getId().equals(group2.getId())) continue;
            
            BoundingBox box2 = getGroupBoundingBox(group2);
            if (box2 == null) continue;
            
            if (box1.overlaps(box2)) {
                CollisionType collisionType = getCollisionType(box1, box2);
                handleCollision(group1, group2, rotation1, collisionType);
            } else {
                removeCollision(group1.getId(), group2.getId());
            }
        }
    }
    
    private void handleCollision(DisplayGroup rotating, DisplayGroup other, RotationData rotationData, CollisionType collisionType) {
        boolean wasColliding = isColliding(rotating.getId(), other.getId());
        
        if (!wasColliding) {
            addCollision(rotating.getId(), other.getId());
        }
        
        if (!other.isRotating() && !rotatingGroups.containsKey(other.getId())) {
            String axis;
            float speed;
            
            float yaw = other.getOrigin().getYaw();
            float normalizedYaw = ((yaw % 360) + 360) % 360;
            
            float otherPitch = Math.abs(other.getOrigin().getPitch());
            if (otherPitch > 80 && otherPitch < 100) {
                axis = "y";
            } else if ((normalizedYaw >= 315 || normalizedYaw < 45) || (normalizedYaw >= 135 && normalizedYaw < 225)) {
                axis = "z";
            } else if ((normalizedYaw >= 45 && normalizedYaw < 135) || (normalizedYaw >= 225 && normalizedYaw < 315)) {
                axis = "x";
            } else {
                axis = "y";
            }
            
            speed = -rotationData.degreesPerSecond;
            
            rotatingGroups.put(other.getId(), new RotationData(speed, axis));
            other.startContinuousRotation(speed, axis);
        }
    }
    
    private String getPerpendicularAxis(String currentAxis) {
        return switch (currentAxis.toLowerCase()) {
            case "x" -> "z";
            case "y" -> "x";
            case "z" -> "y";
            default -> "y";
        };
    }
    
    private CollisionType getCollisionType(BoundingBox box1, BoundingBox box2) {
        double center1X = (box1.getMinX() + box1.getMaxX()) / 2;
        double center1Y = (box1.getMinY() + box1.getMaxY()) / 2;
        double center1Z = (box1.getMinZ() + box1.getMaxZ()) / 2;
        
        double center2X = (box2.getMinX() + box2.getMaxX()) / 2;
        double center2Y = (box2.getMinY() + box2.getMaxY()) / 2;
        double center2Z = (box2.getMinZ() + box2.getMaxZ()) / 2;
        
        double deltaX = Math.abs(center1X - center2X);
        double deltaY = Math.abs(center1Y - center2Y);
        double deltaZ = Math.abs(center1Z - center2Z);
        
        double threshold = 0.5;
        
        boolean alignedX = deltaX < threshold;
        boolean alignedY = deltaY < threshold;
        boolean alignedZ = deltaZ < threshold;
        
        boolean isDirect;
        String perpendicularAxis;
        
        if (alignedX && alignedY) {
            isDirect = true;
            perpendicularAxis = "z";
        } else if (alignedX && alignedZ) {
            isDirect = true;
            perpendicularAxis = "y";
        } else if (alignedY && alignedZ) {
            isDirect = true;
            perpendicularAxis = "x";
        } else {
            isDirect = false;
            if (deltaX > deltaY && deltaX > deltaZ) {
                perpendicularAxis = "x";
            } else if (deltaY > deltaZ) {
                perpendicularAxis = "y";
            } else {
                perpendicularAxis = "z";
            }
        }
        
        return new CollisionType(isDirect, perpendicularAxis);
    }
    
    private static class CollisionType {
        boolean isDirect;
        String perpendicularAxis;
        
        CollisionType(boolean isDirect, String perpendicularAxis) {
            this.isDirect = isDirect;
            this.perpendicularAxis = perpendicularAxis;
        }
    }
    
    private void removeCollision(UUID id1, UUID id2) {
        if (isColliding(id1, id2)) {
            collidingPairs.getOrDefault(id1, new HashSet<>()).remove(id2);
            collidingPairs.getOrDefault(id2, new HashSet<>()).remove(id1);
            
            Set<UUID> remainingConnections = collidingPairs.get(id2);
            if (remainingConnections == null || remainingConnections.isEmpty()) {
                var dm = Atom.getInstance().getDisplayManager();
                DisplayGroup group = dm.getDisplayGroups().getIfPresent(id2);
                if (group != null && group.isRotating() && rotatingGroups.containsKey(id2)) {
                    rotatingGroups.remove(id2);
                    group.stopContinuousRotation();
                }
            }
        }
    }
    
    private boolean isColliding(UUID id1, UUID id2) {
        return collidingPairs.getOrDefault(id1, new HashSet<>()).contains(id2);
    }
    
    private void addCollision(UUID id1, UUID id2) {
        collidingPairs.computeIfAbsent(id1, k -> new HashSet<>()).add(id2);
        collidingPairs.computeIfAbsent(id2, k -> new HashSet<>()).add(id1);
    }
    
    private String getPairKey(UUID id1, UUID id2) {
        return id1.compareTo(id2) < 0 ? id1 + ":" + id2 : id2 + ":" + id1;
    }
    
    private BoundingBox getGroupBoundingBox(DisplayGroup group) {
        if (group.getDisplays().isEmpty()) return null;
        
        BoundingBox combined = null;
        for (org.bukkit.entity.Display display : group.getDisplays()) {
            BoundingBox displayBox = getDisplayBoundingBox(display);
            if (displayBox != null) {
                if (combined == null) {
                    combined = displayBox;
                } else {
                    combined.union(displayBox);
                }
            }
        }
        return combined;
    }
    
    private BoundingBox getDisplayBoundingBox(org.bukkit.entity.Display display) {
        org.bukkit.Location loc = display.getLocation();
        var transform = display.getTransformation();
        org.joml.Vector3f scale = transform.getScale();
        org.joml.Vector3f translation = transform.getTranslation();
        
        double halfWidth = scale.x / 2.0;
        double halfHeight = scale.y / 2.0;
        double halfDepth = scale.z / 2.0;
        
        return new BoundingBox(
            loc.getX() + translation.x - halfWidth,
            loc.getY() + translation.y - halfHeight,
            loc.getZ() + translation.z - halfDepth,
            loc.getX() + translation.x + halfWidth,
            loc.getY() + translation.y + halfHeight,
            loc.getZ() + translation.z + halfDepth
        );
    }
    
    private static class RotationData {
        float degreesPerSecond;
        String axis;
        
        RotationData(float degreesPerSecond, String axis) {
            this.degreesPerSecond = degreesPerSecond;
            this.axis = axis;
        }
    }
}
