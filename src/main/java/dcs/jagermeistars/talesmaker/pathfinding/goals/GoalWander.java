package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

import java.util.Random;

/**
 * Goal for random wandering within a defined area.
 * Generates random destinations within bounds.
 */
public class GoalWander implements Goal {

    private final BlockPos center;
    private final int radiusXZ;
    private final int radiusY;
    private final int radiusXZSq;
    private final Random random;
    private final int tolerance;

    private BlockPos currentTarget;

    /**
     * Create a wander goal centered on a position.
     * @param center Center of wander area
     * @param radiusXZ Horizontal radius
     */
    public GoalWander(BlockPos center, int radiusXZ) {
        this(center, radiusXZ, 4, 1);
    }

    /**
     * Create a wander goal with full options.
     * @param center Center of wander area
     * @param radiusXZ Horizontal radius
     * @param radiusY Vertical radius
     * @param tolerance Distance tolerance for reaching target
     */
    public GoalWander(BlockPos center, int radiusXZ, int radiusY, int tolerance) {
        this.center = center;
        this.radiusXZ = radiusXZ;
        this.radiusY = radiusY;
        this.radiusXZSq = radiusXZ * radiusXZ;
        this.tolerance = tolerance;
        this.random = new Random();
        this.currentTarget = generateNewTarget();
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        if (currentTarget == null) {
            return true;
        }

        int dx = Math.abs(currentTarget.getX() - x);
        int dy = Math.abs(currentTarget.getY() - y);
        int dz = Math.abs(currentTarget.getZ() - z);

        return dx <= tolerance && dy <= tolerance && dz <= tolerance;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        if (currentTarget == null) {
            return 0;
        }

        int dx = Math.abs(currentTarget.getX() - x);
        int dy = Math.abs(currentTarget.getY() - y);
        int dz = Math.abs(currentTarget.getZ() - z);

        int diagonal = Math.min(dx, dz);
        int straight = dx + dz - 2 * diagonal;

        return diagonal * 1.414 + straight + dy * 1.5;
    }

    @Override
    public boolean isReachable(BlockPos from) {
        // Check if from position is within extended range
        int dx = from.getX() - center.getX();
        int dz = from.getZ() - center.getZ();
        int distSq = dx * dx + dz * dz;

        // Allow starting from slightly outside the area
        return distSq <= radiusXZSq * 4;
    }

    /**
     * Generate a new random target within the wander area.
     * @return New target position
     */
    public BlockPos generateNewTarget() {
        // Generate random offset within circular area
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = Math.sqrt(random.nextDouble()) * radiusXZ;

        int offsetX = (int) (Math.cos(angle) * distance);
        int offsetZ = (int) (Math.sin(angle) * distance);
        int offsetY = random.nextInt(radiusY * 2 + 1) - radiusY;

        currentTarget = center.offset(offsetX, offsetY, offsetZ);
        return currentTarget;
    }

    /**
     * Advance to a new random target.
     * Called when the current target is reached.
     */
    public void advanceToNextTarget() {
        generateNewTarget();
    }

    /**
     * Get the current target position.
     */
    public BlockPos getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Set a specific target within the wander area.
     */
    public void setCurrentTarget(BlockPos target) {
        this.currentTarget = target;
    }

    /**
     * Check if a position is within the wander area.
     */
    public boolean isWithinArea(BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dy = pos.getY() - center.getY();
        int dz = pos.getZ() - center.getZ();

        if (Math.abs(dy) > radiusY) {
            return false;
        }

        return dx * dx + dz * dz <= radiusXZSq;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadiusXZ() {
        return radiusXZ;
    }

    public int getRadiusY() {
        return radiusY;
    }

    public int getTolerance() {
        return tolerance;
    }

    @Override
    public String toString() {
        return "GoalWander{center=" + center + " r=" + radiusXZ +
                " target=" + currentTarget + "}";
    }
}
