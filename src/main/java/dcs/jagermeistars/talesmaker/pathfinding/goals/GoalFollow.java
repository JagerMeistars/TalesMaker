package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Goal to follow a moving entity.
 * Dynamically updates target position based on entity location.
 */
public class GoalFollow implements Goal {

    private final Entity target;
    private final int minDistance;
    private final int maxDistance;
    private final int minDistanceSq;
    private final int maxDistanceSq;

    /**
     * Create a follow goal with default distances.
     * @param target Entity to follow
     */
    public GoalFollow(Entity target) {
        this(target, 2, 10);
    }

    /**
     * Create a follow goal with specified distances.
     * @param target Entity to follow
     * @param minDistance Minimum distance to maintain (stop following when closer)
     * @param maxDistance Maximum distance before giving up
     */
    public GoalFollow(Entity target, int minDistance, int maxDistance) {
        this.target = target;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.minDistanceSq = minDistance * minDistance;
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        if (target == null || !target.isAlive()) {
            return true; // Goal satisfied if target is gone
        }

        BlockPos targetPos = target.blockPosition();
        int dx = targetPos.getX() - x;
        int dy = targetPos.getY() - y;
        int dz = targetPos.getZ() - z;
        int distSq = dx * dx + dy * dy + dz * dz;

        // Within acceptable follow range
        return distSq <= minDistanceSq;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        if (target == null || !target.isAlive()) {
            return 0;
        }

        BlockPos targetPos = target.blockPosition();
        int dx = Math.abs(targetPos.getX() - x);
        int dy = Math.abs(targetPos.getY() - y);
        int dz = Math.abs(targetPos.getZ() - z);

        // Distance minus minimum distance (want to get within minDistance)
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.max(0, distance - minDistance);
    }

    @Override
    public boolean isReachable(BlockPos from) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Check if target is too far
        double distSq = from.distSqr(target.blockPosition());
        return distSq <= maxDistanceSq;
    }

    /**
     * Get the current target position.
     * @return Current BlockPos of target, or null if target is invalid
     */
    public BlockPos getTargetPosition() {
        if (target == null || !target.isAlive()) {
            return null;
        }
        return target.blockPosition();
    }

    /**
     * Check if the target has moved significantly since last path calculation.
     * @param lastKnownPos Last known position of target
     * @param threshold Movement threshold in blocks
     * @return true if target has moved beyond threshold
     */
    public boolean hasTargetMoved(BlockPos lastKnownPos, double threshold) {
        if (target == null || !target.isAlive() || lastKnownPos == null) {
            return true;
        }
        return lastKnownPos.distSqr(target.blockPosition()) > threshold * threshold;
    }

    public Entity getTarget() {
        return target;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public boolean isTargetValid() {
        return target != null && target.isAlive();
    }

    @Override
    public String toString() {
        String targetName = target != null ? target.getName().getString() : "null";
        return "GoalFollow{" + targetName + " min=" + minDistance + " max=" + maxDistance + "}";
    }
}
