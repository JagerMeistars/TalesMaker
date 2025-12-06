package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Interface representing a calculated path.
 */
public interface IPath {

    /**
     * Get all positions in the path.
     *
     * @return list of block positions from start to end
     */
    List<BlockPos> positions();

    /**
     * Get all movements in the path.
     *
     * @return list of movements to execute
     */
    List<Movement> movements();

    /**
     * Get the goal this path was calculated for.
     *
     * @return the goal
     */
    Goal getGoal();

    /**
     * Get the number of positions in the path.
     *
     * @return path length
     */
    int length();

    /**
     * Get the starting position.
     *
     * @return start BlockPos
     */
    BlockPos getSrc();

    /**
     * Get the destination position.
     *
     * @return destination BlockPos
     */
    BlockPos getDest();

    /**
     * Get the total movement cost of the path.
     *
     * @return total cost
     */
    double getTotalCost();

    /**
     * Check if this path reaches the goal completely.
     *
     * @return true if goal is reached, false if partial path
     */
    boolean isComplete();

    /**
     * Create a partial path up to the specified index.
     *
     * @param index the index to cut at
     * @return a new path containing only positions up to index
     */
    IPath cutoffAtIndex(int index);

    /**
     * Get position at specific index.
     *
     * @param index the index
     * @return BlockPos at that index
     */
    default BlockPos getPosition(int index) {
        return positions().get(index);
    }

    /**
     * Get movement at specific index.
     *
     * @param index the index
     * @return Movement at that index
     */
    default Movement getMovement(int index) {
        return movements().get(index);
    }
}