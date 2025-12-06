package dcs.jagermeistars.talesmaker.pathfinding.calc;

import net.minecraft.core.BlockPos;

/**
 * Constants and utilities for movement cost calculation.
 */
public final class ActionCosts {

    // Base movement costs
    public static final double WALK = 1.0;
    public static final double DIAGONAL = 1.414;  // sqrt(2)
    public static final double JUMP_UP = 2.0;
    public static final double STEP_DOWN = 1.5;
    public static final double FALL_BASE = 2.0;
    public static final double FALL_PER_BLOCK = 0.5;
    public static final double PARKOUR_BASE = 3.0;
    public static final double PARKOUR_PER_BLOCK = 1.0;
    public static final double SWIM = 2.0;
    public static final double CLIMB = 1.0;
    public static final double DOOR = 1.5;

    // Penalty costs
    public static final double WATER_PENALTY = 1.0;
    public static final double DANGER_PENALTY = 10.0;

    // Cost indicating impossible movement
    public static final double IMPOSSIBLE = Double.MAX_VALUE;

    private ActionCosts() {
        // Utility class
    }

    /**
     * Calculate fall cost based on distance.
     *
     * @param blocks number of blocks to fall
     * @return movement cost
     */
    public static double fallCost(int blocks) {
        if (blocks <= 0) return 0;
        return FALL_BASE + (blocks - 1) * FALL_PER_BLOCK;
    }

    /**
     * Calculate parkour (gap jump) cost based on distance.
     *
     * @param gapBlocks number of blocks to jump across
     * @return movement cost
     */
    public static double parkourCost(int gapBlocks) {
        if (gapBlocks <= 0) return WALK;
        return PARKOUR_BASE + gapBlocks * PARKOUR_PER_BLOCK;
    }

    /**
     * Calculate octile distance heuristic (3D).
     * Admissible heuristic for A* that accounts for diagonal movement.
     *
     * @param from start position
     * @param to   end position
     * @return estimated cost
     */
    public static double octileDistance(BlockPos from, BlockPos to) {
        return octileDistance(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ()
        );
    }

    /**
     * Calculate octile distance heuristic (3D).
     */
    public static double octileDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);

        // Sort to get min, mid, max
        int min, mid, max;
        if (dx <= dy && dx <= dz) {
            min = dx;
            if (dy <= dz) { mid = dy; max = dz; }
            else { mid = dz; max = dy; }
        } else if (dy <= dz) {
            min = dy;
            if (dx <= dz) { mid = dx; max = dz; }
            else { mid = dz; max = dx; }
        } else {
            min = dz;
            if (dx <= dy) { mid = dx; max = dy; }
            else { mid = dy; max = dx; }
        }

        // Octile distance in 3D:
        // - min steps use 3D diagonal (cost ~1.73)
        // - (mid - min) steps use 2D diagonal (cost ~1.41)
        // - (max - mid) steps use straight (cost 1.0)
        double diag3D = 1.732; // sqrt(3)
        double diag2D = 1.414; // sqrt(2)

        return min * diag3D + (mid - min) * diag2D + (max - mid) * WALK;
    }

    /**
     * Calculate Manhattan distance (simple, fast).
     */
    public static double manhattanDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1) + Math.abs(z2 - z1);
    }

    /**
     * Calculate Euclidean distance (straight line).
     */
    public static double euclideanDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculate 2D octile distance (ignoring Y).
     * Useful for primarily horizontal movement.
     */
    public static double octileDistance2D(int x1, int z1, int x2, int z2) {
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int min = Math.min(dx, dz);
        int max = Math.max(dx, dz);
        return min * DIAGONAL + (max - min) * WALK;
    }
}