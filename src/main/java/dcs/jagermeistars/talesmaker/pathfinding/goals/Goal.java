package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

/**
 * Base interface for pathfinding goals.
 * Goals define where the entity wants to go and provide heuristics for A*.
 */
public interface Goal {

    /**
     * Check if the goal is satisfied at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if this position satisfies the goal
     */
    boolean isAtGoal(int x, int y, int z);

    /**
     * Calculate the heuristic estimate from position to goal.
     * Must be admissible (never overestimate actual cost).
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return estimated cost to reach goal
     */
    double heuristic(int x, int y, int z);

    /**
     * Check if the goal can potentially be reached from the given position.
     * Used for early termination of impossible paths.
     *
     * @param from starting position
     * @return true if goal is potentially reachable
     */
    default boolean isReachable(BlockPos from) {
        return true;
    }

    /**
     * Convenience method to check goal at BlockPos.
     */
    default boolean isAtGoal(BlockPos pos) {
        return isAtGoal(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Convenience method to calculate heuristic from BlockPos.
     */
    default double heuristic(BlockPos pos) {
        return heuristic(pos.getX(), pos.getY(), pos.getZ());
    }
}