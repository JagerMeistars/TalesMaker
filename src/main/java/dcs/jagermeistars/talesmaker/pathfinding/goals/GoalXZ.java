package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

/**
 * Goal to reach a specific X,Z coordinate at any Y level.
 * Useful for navigating to a location regardless of elevation.
 */
public class GoalXZ implements Goal {

    private final int x;
    private final int z;

    public GoalXZ(BlockPos pos) {
        this(pos.getX(), pos.getZ());
    }

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        return this.x == x && this.z == z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int dx = Math.abs(this.x - x);
        int dz = Math.abs(this.z - z);

        // Diagonal optimization for horizontal distance only
        int diagonal = Math.min(dx, dz);
        int straight = dx + dz - 2 * diagonal;

        return diagonal * 1.414 + straight;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return "GoalXZ{" + x + ", " + z + "}";
    }
}
