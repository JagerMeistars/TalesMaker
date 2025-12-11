package dcs.jagermeistars.talesmaker.pathfinding.movement;

import dcs.jagermeistars.talesmaker.pathfinding.path.IPath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Executes movements along a calculated path.
 * Handles movement sequencing, stuck detection, and failure recovery.
 */
public class MovementExecutor {
    private IPath currentPath;
    private int currentIndex;
    private Movement currentMovement;

    // Stuck detection
    private Vec3 lastPosition;
    private int stuckTicks;
    private static final int STUCK_THRESHOLD = 40;
    private static final double STUCK_DISTANCE_SQ = 0.01; // 0.1 blocks squared

    // State
    private boolean finished;
    private boolean failed;

    public MovementExecutor() {
        reset();
    }

    /**
     * Set a new path to execute.
     */
    public void setPath(IPath path) {
        reset();
        this.currentPath = path;

        if (path != null && path.length() > 1) {
            this.currentIndex = 0;
            this.currentMovement = createMovementForSegment(0);
        } else {
            this.finished = true;
        }
    }

    /**
     * Reset the executor state.
     */
    public void reset() {
        currentPath = null;
        currentIndex = 0;
        currentMovement = null;
        lastPosition = null;
        stuckTicks = 0;
        finished = false;
        failed = false;
    }

    /**
     * Execute one tick of movement.
     *
     * @param ctx the movement context
     * @return result of this tick
     */
    public MovementResult tick(MovementContext ctx) {

        if (finished) {
            return MovementResult.SUCCESS;
        }
        if (failed) {
            return MovementResult.FAILED;
        }
        if (currentPath == null || currentMovement == null) {
            return MovementResult.FAILED;
        }

        // Stuck detection
        Vec3 currentPos = ctx.getPosition();
        if (lastPosition != null) {
            if (currentPos.distanceToSqr(lastPosition) < STUCK_DISTANCE_SQ) {
                stuckTicks++;
                if (stuckTicks > STUCK_THRESHOLD) {
                    failed = true;
                    return MovementResult.FAILED;
                }
            } else {
                stuckTicks = 0;
            }
        }
        lastPosition = currentPos;

        // Execute current movement
        MovementResult result = currentMovement.tick(ctx);

        switch (result) {
            case SUCCESS:
                // Advance to next movement
                if (!advanceToNext(ctx)) {
                    finished = true;
                    return MovementResult.SUCCESS;
                }
                return MovementResult.IN_PROGRESS;

            case FAILED:
                failed = true;
                return MovementResult.FAILED;

            case IN_PROGRESS:
                return MovementResult.IN_PROGRESS;

            case CANCELED:
                reset();
                return MovementResult.CANCELED;

            default:
                return MovementResult.IN_PROGRESS;
        }
    }

    /**
     * Advance to the next movement in the path.
     *
     * @return true if there's another movement, false if path is complete
     */
    private boolean advanceToNext(MovementContext ctx) {
        currentIndex++;
        if (currentIndex >= currentPath.length() - 1) {
            // Reached end of path
            return false;
        }

        currentMovement = createMovementForSegment(currentIndex);
        if (currentMovement == null) {
            // Failed to create movement
            return false;
        }

        stuckTicks = 0;
        return true;
    }

    /**
     * Create a movement for a path segment.
     */
    @Nullable
    private Movement createMovementForSegment(int index) {
        if (currentPath == null || index >= currentPath.length() - 1) {
            return null;
        }

        BlockPos from = currentPath.getPosition(index);
        BlockPos to = currentPath.getPosition(index + 1);

        return MovementFactory.createMovement(from, to);
    }

    /**
     * Check if execution is finished (path complete).
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Check if execution failed.
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Check if currently executing.
     */
    public boolean isExecuting() {
        return currentPath != null && !finished && !failed;
    }

    /**
     * Get the current movement being executed.
     */
    @Nullable
    public Movement getCurrentMovement() {
        return currentMovement;
    }

    /**
     * Get the current path.
     */
    @Nullable
    public IPath getCurrentPath() {
        return currentPath;
    }

    /**
     * Get current position index in path.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Get remaining positions in path.
     */
    public int getRemainingPositions() {
        if (currentPath == null) return 0;
        return Math.max(0, currentPath.length() - currentIndex - 1);
    }

    /**
     * Cancel current execution.
     */
    public void cancel() {
        reset();
    }

    /**
     * Get progress as percentage (0.0 to 1.0).
     */
    public double getProgress() {
        if (currentPath == null || currentPath.length() <= 1) {
            return finished ? 1.0 : 0.0;
        }
        return (double) currentIndex / (currentPath.length() - 1);
    }
}
