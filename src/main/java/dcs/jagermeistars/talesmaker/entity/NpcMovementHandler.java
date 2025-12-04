package dcs.jagermeistars.talesmaker.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all movement-related logic for NPC entities.
 * Includes goto, patrol, follow, directional, and wander movement modes.
 */
public class NpcMovementHandler {

    // Stuck detection constants
    private static final double ARRIVAL_THRESHOLD_SQ = 0.09; // 0.3 blocks for precise positioning
    private static final int STUCK_TIMEOUT_TICKS = 40; // 2 seconds

    private final NpcEntity npc;

    // Stuck detection state
    private int stuckTicks = 0;
    private double lastX, lastY, lastZ;

    // Wander cooldown
    private int wanderCooldown = 0;

    public NpcMovementHandler(NpcEntity npc) {
        this.npc = npc;
    }

    /**
     * Moves NPC to a specific position using pathfinding.
     */
    public void moveToPosition(double x, double y, double z) {
        npc.setMovementTarget(x, y, z);
        npc.setMovementTargetEntityId(-1);
        npc.setMovementState("goto");

        // Enable AI for pathfinding
        npc.setNoAi(false);

        // Start navigation
        npc.getNavigation().moveTo(x, y, z, 1.0D);
    }

    /**
     * Stops all movement and resets movement state.
     */
    public void stopMovement() {
        npc.setMovementState("idle");
        npc.setMovementTargetEntityId(-1);
        npc.setPatrolIndex(0);
        npc.setDirectionalRemaining(0.0f);
        npc.getNavigation().stop();

        // Reset stuck detection
        stuckTicks = 0;

        // Re-enable NoAi to prevent random wandering
        npc.setNoAi(true);
    }

    /**
     * Updates movement behavior. Called every tick when movement is active.
     */
    public void tick() {
        String state = npc.getMovementState();
        if (state == null || state.equals("idle")) {
            return;
        }

        switch (state) {
            case "goto" -> updateGotoMovement();
            case "patrol" -> updatePatrolMovement();
            case "follow" -> updateFollowMovement();
            case "directional" -> updateDirectionalMovement();
            case "wander" -> updateWanderMovement();
        }
    }

    /**
     * Updates goto movement - moves to target position.
     */
    private void updateGotoMovement() {
        double targetX = npc.getMovementTargetX();
        double targetY = npc.getMovementTargetY();
        double targetZ = npc.getMovementTargetZ();

        // Calculate distances
        double dx = npc.getX() - targetX;
        double dy = npc.getY() - targetY;
        double dz = npc.getZ() - targetZ;
        double horizontalDistSq = dx * dx + dz * dz;

        // Check if we've reached the target (precise positioning)
        if (horizontalDistSq < ARRIVAL_THRESHOLD_SQ && Math.abs(dy) < 1.0) {
            stopMovement();
            return;
        }

        // Stuck detection
        if (checkStuck()) {
            stopMovement();
            return;
        }

        // Update path if navigation is done
        if (npc.getNavigation().isDone()) {
            boolean success = npc.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
            if (!success) {
                stuckTicks += 10; // Speed up timeout if path not found
            }
        }

        // Look in the direction of movement
        npc.getLookControl().setLookAt(targetX, targetY + 1.5, targetZ);
    }

    /**
     * Starts patrol movement between specified points.
     */
    public void startPatrol(List<BlockPos> points) {
        if (points == null || points.size() < 2) {
            return;
        }

        // Convert points to JSON
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            BlockPos pos = points.get(i);
            json.append("[").append(pos.getX()).append(",").append(pos.getY()).append(",").append(pos.getZ()).append("]");
            if (i < points.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        npc.setPatrolPoints(json.toString());
        npc.setPatrolIndex(0);
        npc.setMovementState("patrol");

        // Enable AI for pathfinding
        npc.setNoAi(false);

        // Move to first point
        BlockPos first = points.get(0);
        npc.getNavigation().moveTo(first.getX() + 0.5, first.getY(), first.getZ() + 0.5, 1.0D);
    }

    /**
     * Updates patrol movement - cycles through patrol points.
     */
    private void updatePatrolMovement() {
        String pointsJson = npc.getPatrolPoints();
        if (pointsJson == null || pointsJson.isEmpty()) {
            stopMovement();
            return;
        }

        List<BlockPos> points = parsePatrolPoints(pointsJson);
        if (points.isEmpty()) {
            stopMovement();
            return;
        }

        int currentIndex = npc.getPatrolIndex();
        BlockPos targetPos = points.get(currentIndex);

        // Calculate distances to center of block
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY();
        double targetZ = targetPos.getZ() + 0.5;
        double dx = npc.getX() - targetX;
        double dy = npc.getY() - targetY;
        double dz = npc.getZ() - targetZ;
        double horizontalDistSq = dx * dx + dz * dz;

        // Check if we've reached the target (precise positioning)
        if (horizontalDistSq < ARRIVAL_THRESHOLD_SQ && Math.abs(dy) < 1.0) {
            // Move to next point
            int nextIndex = (currentIndex + 1) % points.size();
            npc.setPatrolIndex(nextIndex);
            BlockPos nextPos = points.get(nextIndex);
            npc.getNavigation().moveTo(nextPos.getX() + 0.5, nextPos.getY(), nextPos.getZ() + 0.5, 1.0D);
            stuckTicks = 0;
            return;
        }

        // Stuck detection - skip to next point if stuck
        if (checkStuck()) {
            int nextIndex = (currentIndex + 1) % points.size();
            npc.setPatrolIndex(nextIndex);
            BlockPos nextPos = points.get(nextIndex);
            npc.getNavigation().moveTo(nextPos.getX() + 0.5, nextPos.getY(), nextPos.getZ() + 0.5, 1.0D);
            stuckTicks = 0;
            return;
        }

        // Update path if navigation is done
        if (npc.getNavigation().isDone()) {
            boolean success = npc.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
            if (!success) {
                stuckTicks += 10;
            }
        }
    }

    /**
     * Parses patrol points from JSON string.
     */
    private List<BlockPos> parsePatrolPoints(String json) {
        List<BlockPos> points = new ArrayList<>();
        if (json == null || json.isEmpty() || !json.startsWith("[")) {
            return points;
        }

        try {
            String inner = json.substring(1, json.length() - 1);
            int depth = 0;
            int start = 0;
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '[') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        String point = inner.substring(start + 1, i);
                        String[] coords = point.split(",");
                        if (coords.length == 3) {
                            int x = Integer.parseInt(coords[0].trim());
                            int y = Integer.parseInt(coords[1].trim());
                            int z = Integer.parseInt(coords[2].trim());
                            points.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Invalid JSON - return empty list
        }
        return points;
    }

    /**
     * Starts following an entity.
     */
    public void startFollow(Entity target) {
        if (target == null) {
            return;
        }

        npc.setMovementTargetEntityId(target.getId());
        npc.setMovementState("follow");

        // Enable AI for pathfinding
        npc.setNoAi(false);

        // Start moving toward target
        npc.getNavigation().moveTo(target, 1.0D);
    }

    /**
     * Updates follow movement - continuously follows target entity.
     */
    private void updateFollowMovement() {
        int targetId = npc.getMovementTargetEntityId();
        if (targetId < 0) {
            stopMovement();
            return;
        }

        Entity target = npc.level().getEntity(targetId);
        if (target == null || !target.isAlive()) {
            stopMovement();
            return;
        }

        double distSq = npc.distanceToSqr(target);

        // If too far, move closer
        if (distSq > 4.0) { // More than 2 blocks away
            // Stuck detection
            if (checkStuck()) {
                stopMovement();
                return;
            }

            if (npc.getNavigation().isDone() || npc.tickCount % 20 == 0) {
                boolean success = npc.getNavigation().moveTo(target, 1.0D);
                if (!success) {
                    stuckTicks += 10;
                }
            }
        } else {
            // Close enough - stop navigation but keep following state
            npc.getNavigation().stop();
            stuckTicks = 0;
        }
    }

    /**
     * Starts directional movement (forward/backward/left/right) for specified distance.
     */
    public void startDirectionalMovement(String direction, float distance) {
        float yaw = npc.getYRot();
        float radians = (float) Math.toRadians(yaw);

        float dx = 0, dz = 0;
        switch (direction.toLowerCase()) {
            case "forward" -> {
                dx = -(float) Math.sin(radians);
                dz = (float) Math.cos(radians);
            }
            case "backward" -> {
                dx = (float) Math.sin(radians);
                dz = -(float) Math.cos(radians);
            }
            case "left" -> {
                dx = (float) Math.cos(radians);
                dz = (float) Math.sin(radians);
            }
            case "right" -> {
                dx = -(float) Math.cos(radians);
                dz = -(float) Math.sin(radians);
            }
            default -> { return; }
        }

        double targetX = npc.getX() + dx * distance;
        double targetY = npc.getY();
        double targetZ = npc.getZ() + dz * distance;

        npc.setMovementTarget(targetX, targetY, targetZ);
        npc.setDirectionalVector(dx, dz);
        npc.setDirectionalRemaining(distance);
        npc.setMovementState("directional");

        // Enable AI for pathfinding
        npc.setNoAi(false);

        // Start navigation
        npc.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
    }

    /**
     * Updates directional movement.
     */
    private void updateDirectionalMovement() {
        double targetX = npc.getMovementTargetX();
        double targetY = npc.getMovementTargetY();
        double targetZ = npc.getMovementTargetZ();

        // Calculate distances
        double dx = npc.getX() - targetX;
        double dy = npc.getY() - targetY;
        double dz = npc.getZ() - targetZ;
        double horizontalDistSq = dx * dx + dz * dz;

        // Check if we've reached the target (precise positioning)
        if (horizontalDistSq < ARRIVAL_THRESHOLD_SQ && Math.abs(dy) < 1.0) {
            stopMovement();
            return;
        }

        // Stuck detection
        if (checkStuck()) {
            stopMovement();
            return;
        }

        // Update path if navigation is done
        if (npc.getNavigation().isDone()) {
            boolean success = npc.getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
            if (!success) {
                stuckTicks += 10;
            }
        }
    }

    /**
     * Starts wander movement within a circular radius.
     */
    public void startWander(double centerX, double centerY, double centerZ, float radius) {
        String json = "[[" + (int) centerX + "," + (int) centerZ + "," + (int) radius + "]]";
        npc.setWanderPolygon(json);
        npc.setWanderY((float) centerY);
        npc.setMovementState("wander");
        wanderCooldown = 0;

        // Enable AI for pathfinding
        npc.setNoAi(false);

        // Pick initial wander target
        pickNewWanderTarget();
    }

    /**
     * Updates wander movement - picks random points within wander area.
     */
    private void updateWanderMovement() {
        if (wanderCooldown > 0) {
            wanderCooldown--;
        }

        // Stuck detection while moving
        if (!npc.getNavigation().isDone()) {
            if (checkStuck()) {
                npc.getNavigation().stop();
                stuckTicks = 0;
                wanderCooldown = 20;
            }
        }

        // If navigation is done, wait for cooldown then pick new target
        if (npc.getNavigation().isDone()) {
            if (wanderCooldown <= 0) {
                pickNewWanderTarget();
                wanderCooldown = 40 + npc.getRandom().nextInt(60);
                stuckTicks = 0;
            }
        }
    }

    /**
     * Picks a new random wander target within the wander area.
     */
    private void pickNewWanderTarget() {
        String polygonJson = npc.getWanderPolygon();
        float wanderY = npc.getWanderY();

        if (polygonJson == null || polygonJson.isEmpty()) {
            stopMovement();
            return;
        }

        try {
            String inner = polygonJson.substring(2, polygonJson.length() - 2);
            String[] parts = inner.split(",");
            if (parts.length >= 3) {
                int centerX = Integer.parseInt(parts[0].trim());
                int centerZ = Integer.parseInt(parts[1].trim());
                int radius = Integer.parseInt(parts[2].trim());

                double angle = npc.getRandom().nextDouble() * Math.PI * 2;
                double dist = Math.sqrt(npc.getRandom().nextDouble()) * radius;

                double targetX = centerX + Math.cos(angle) * dist;
                double targetZ = centerZ + Math.sin(angle) * dist;

                npc.getNavigation().moveTo(targetX + 0.5, wanderY, targetZ + 0.5, 1.0D);
            }
        } catch (Exception e) {
            stopMovement();
        }
    }

    /**
     * Checks if NPC is stuck and updates stuck counter.
     * @return true if stuck for more than timeout
     */
    private boolean checkStuck() {
        double movedSq = (npc.getX() - lastX) * (npc.getX() - lastX)
                       + (npc.getZ() - lastZ) * (npc.getZ() - lastZ);

        if (movedSq < 0.001) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }

        lastX = npc.getX();
        lastY = npc.getY();
        lastZ = npc.getZ();

        return stuckTicks > STUCK_TIMEOUT_TICKS;
    }

    /**
     * Checks if any movement is active.
     */
    public boolean isMovementActive() {
        String state = npc.getMovementState();
        return state != null && !state.equals("idle");
    }
}
