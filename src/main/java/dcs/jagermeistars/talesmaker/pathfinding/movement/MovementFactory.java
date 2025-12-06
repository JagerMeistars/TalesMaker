package dcs.jagermeistars.talesmaker.pathfinding.movement;

import dcs.jagermeistars.talesmaker.pathfinding.movement.movements.*;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * Factory for creating appropriate Movement instances based on position deltas.
 */
public final class MovementFactory {

    private MovementFactory() {
        // Utility class
    }

    /**
     * Create the appropriate movement type for moving from one position to another.
     *
     * @param from source position
     * @param to   destination position
     * @return the appropriate Movement, or null if no valid movement exists
     */
    @Nullable
    public static Movement createMovement(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        int horizontalDist = Math.abs(dx) + Math.abs(dz);
        boolean isDiagonal = Math.abs(dx) == 1 && Math.abs(dz) == 1;

        // Vertical only movement (climbing)
        if (dx == 0 && dz == 0) {
            if (Math.abs(dy) == 1) {
                return new MovementPillar(from, to);
            }
            return null; // Invalid vertical movement
        }

        // Same level movement
        if (dy == 0) {
            if (isDiagonal) {
                return new MovementDiagonal(from, to);
            }
            if (horizontalDist == 1) {
                return new MovementTraverse(from, to);
            }
            if (horizontalDist == 2 && (dx == 0 || dz == 0)) {
                // Parkour jump (2 blocks in one direction)
                return new MovementParkour(from, to);
            }
            return null;
        }

        // Moving up
        if (dy == 1) {
            if (horizontalDist == 1 && !isDiagonal) {
                return new MovementAscend(from, to);
            }
            // Could be climbing
            if (horizontalDist == 0) {
                return new MovementPillar(from, to);
            }
            return null;
        }

        // Moving down
        if (dy < 0) {
            int fallDist = Math.abs(dy);

            // Step down (1 block)
            if (fallDist == 1 && horizontalDist == 1 && !isDiagonal) {
                return new MovementDescend(from, to);
            }

            // Fall (2-3 blocks)
            if (fallDist >= 2 && fallDist <= 3 && horizontalDist <= 1) {
                return new MovementFall(from, to);
            }

            // Climbing down
            if (horizontalDist == 0) {
                return new MovementPillar(from, to);
            }

            return null;
        }

        return null;
    }

    /**
     * Check if a movement between two positions is potentially valid.
     * Quick check without creating the movement object.
     */
    public static boolean isValidMovement(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());

        // Maximum horizontal distance is 2 (parkour)
        if (dx > 2 || dz > 2) return false;

        // Maximum up is 1 (jump)
        if (dy > 1) return false;

        // Maximum down is 3 (fall)
        if (dy < -3) return false;

        // Can't move diagonally and vertically at same time (except climb)
        if (dx >= 1 && dz >= 1 && dy != 0) return false;

        // Parkour can't have vertical component
        if ((dx == 2 || dz == 2) && dy != 0) return false;

        return true;
    }
}
