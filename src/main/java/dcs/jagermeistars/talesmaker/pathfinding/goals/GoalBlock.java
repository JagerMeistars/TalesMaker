package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

/**
 * Goal to reach an exact block position.
 */
public class GoalBlock implements Goal {

    private final int x;
    private final int y;
    private final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int dx = Math.abs(this.x - x);
        int dy = Math.abs(this.y - y);
        int dz = Math.abs(this.z - z);

        // Manhattan distance with diagonal optimization
        // Diagonal moves cost sqrt(2), straight moves cost 1
        int horizontal = dx + dz;
        int diagonal = Math.min(dx, dz);
        int straight = horizontal - 2 * diagonal;

        return diagonal * 1.414 + straight + dy * 1.5;
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

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return "GoalBlock{" + x + ", " + y + ", " + z + "}";
    }
}
