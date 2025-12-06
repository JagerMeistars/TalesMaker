package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

/**
 * Goal to get within a certain radius of a position.
 */
public class GoalNear implements Goal {

    private final int x;
    private final int y;
    private final int z;
    private final int radiusSq;
    private final int radius;

    public GoalNear(BlockPos pos, int radius) {
        this(pos.getX(), pos.getY(), pos.getZ(), radius);
    }

    public GoalNear(int x, int y, int z, int radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.radiusSq = radius * radius;
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        int dx = this.x - x;
        int dy = this.y - y;
        int dz = this.z - z;
        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int dx = Math.abs(this.x - x);
        int dy = Math.abs(this.y - y);
        int dz = Math.abs(this.z - z);

        // Distance minus radius (clamped to 0)
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.max(0, distance - radius);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getRadius() {
        return radius;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return "GoalNear{" + x + ", " + y + ", " + z + " r=" + radius + "}";
    }
}
