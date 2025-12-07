package dcs.jagermeistars.talesmaker.pathfinding.movement;

import dcs.jagermeistars.talesmaker.pathfinding.path.SmoothPath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Executes smooth paths with sub-block precision.
 * Provides smoother movement than the block-based MovementExecutor.
 */
public class SmoothMovementExecutor {

    private SmoothPath currentPath;
    private int currentWaypointIndex;

    // Movement parameters
    private static final double WAYPOINT_REACH_THRESHOLD = 0.3;
    private static final double WAYPOINT_REACH_THRESHOLD_SQ = WAYPOINT_REACH_THRESHOLD * WAYPOINT_REACH_THRESHOLD;
    private static final double DOOR_INTERACT_DISTANCE_SQ = 2.5 * 2.5; // 2.5 blocks

    // Stuck detection
    private Vec3 lastPosition;
    private int stuckTicks;
    private static final int STUCK_THRESHOLD = 60;
    private static final double STUCK_DISTANCE_SQ = 0.01;

    // State
    private boolean finished;
    private boolean failed;

    public SmoothMovementExecutor() {
        reset();
    }

    /**
     * Set a new smooth path to execute.
     */
    public void setPath(SmoothPath path) {
        reset();
        this.currentPath = path;

        if (path != null && path.length() > 1) {
            this.currentWaypointIndex = 0;
        } else {
            this.finished = true;
        }
    }

    /**
     * Reset the executor state.
     */
    public void reset() {
        currentPath = null;
        currentWaypointIndex = 0;
        lastPosition = null;
        stuckTicks = 0;
        finished = false;
        failed = false;
    }

    /**
     * Execute one tick of movement.
     *
     * @param ctx movement context
     * @return result of this tick
     */
    public MovementResult tick(MovementContext ctx) {
        if (finished) {
            return MovementResult.SUCCESS;
        }
        if (failed) {
            return MovementResult.FAILED;
        }
        if (currentPath == null) {
            return MovementResult.FAILED;
        }

        Vec3 currentPos = ctx.getPosition();

        // Stuck detection
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

        // Get target waypoint
        Vec3 targetWaypoint = currentPath.getWaypoint(currentWaypointIndex + 1);

        // Check if we've reached the current target
        double distXZ = distanceXZSq(currentPos, targetWaypoint);
        if (distXZ < WAYPOINT_REACH_THRESHOLD_SQ) {
            // Advance to next waypoint
            currentWaypointIndex++;

            if (currentWaypointIndex >= currentPath.length() - 1) {
                // Reached end of path
                finished = true;
                return MovementResult.SUCCESS;
            }

            // Get new target
            targetWaypoint = currentPath.getWaypoint(currentWaypointIndex + 1);
            stuckTicks = 0;
        }

        // Check for doors along the path and open them if needed
        if (ctx.getConfig().canOpenDoors()) {
            tryOpenDoorsAlongPath(ctx, currentPos, targetWaypoint);
        }

        // Handle Y level changes (jumping/falling)
        double dy = targetWaypoint.y - currentPos.y;
        if (dy > 0.5 && ctx.isOnGround()) {
            // Need to jump
            MovementHelper.jumpTowards(ctx.getEntity(), targetWaypoint);
        } else {
            // Check for wall collision and adjust movement if needed
            Vec3 adjustedTarget = adjustForWallCollision(ctx, currentPos, targetWaypoint);
            MovementHelper.moveTowards(ctx.getEntity(), adjustedTarget);
        }

        return MovementResult.IN_PROGRESS;
    }

    /**
     * Check for doors between current position and target, and open them if needed.
     */
    private void tryOpenDoorsAlongPath(MovementContext ctx, Vec3 currentPos, Vec3 targetWaypoint) {
        // Check blocks around the current position and towards target for doors
        BlockPos currentBlock = new BlockPos((int) Math.floor(currentPos.x), (int) Math.floor(currentPos.y), (int) Math.floor(currentPos.z));
        BlockPos targetBlock = new BlockPos((int) Math.floor(targetWaypoint.x), (int) Math.floor(targetWaypoint.y), (int) Math.floor(targetWaypoint.z));

        // Check in a small area around the NPC for doors
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos checkPos = currentBlock.offset(dx, dy, dz);
                    tryOpenDoorAt(ctx, checkPos, currentPos);
                }
            }
        }

        // Also check along the path to target
        if (!currentBlock.equals(targetBlock)) {
            int stepX = Integer.compare(targetBlock.getX(), currentBlock.getX());
            int stepZ = Integer.compare(targetBlock.getZ(), currentBlock.getZ());

            BlockPos intermediatePos = currentBlock.offset(stepX, 0, stepZ);
            for (int dy = 0; dy <= 1; dy++) {
                tryOpenDoorAt(ctx, intermediatePos.offset(0, dy, 0), currentPos);
            }
        }
    }

    /**
     * Try to open a door at the given position if it exists and is closed.
     */
    private void tryOpenDoorAt(MovementContext ctx, BlockPos doorPos, Vec3 entityPos) {
        BlockState state = ctx.getLevel().getBlockState(doorPos);

        if (state.getBlock() instanceof DoorBlock door) {
            boolean isOpen = state.getValue(DoorBlock.OPEN);

            if (!isOpen) {
                // Check distance to door
                Vec3 doorCenter = Vec3.atCenterOf(doorPos);
                double distSq = entityPos.distanceToSqr(doorCenter);

                if (distSq < DOOR_INTERACT_DISTANCE_SQ) {
                    // Open the door
                    door.setOpen(ctx.getEntity(), ctx.getLevel(), state, doorPos, true);
                }
            }
        }
    }

    /**
     * Calculate squared XZ distance between two positions.
     */
    private double distanceXZSq(Vec3 a, Vec3 b) {
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        return dx * dx + dz * dz;
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
     * Get the current smooth path.
     */
    @Nullable
    public SmoothPath getCurrentPath() {
        return currentPath;
    }

    /**
     * Get current waypoint index.
     */
    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    /**
     * Get target waypoint.
     */
    @Nullable
    public Vec3 getTargetWaypoint() {
        if (currentPath == null || currentWaypointIndex >= currentPath.length() - 1) {
            return null;
        }
        return currentPath.getWaypoint(currentWaypointIndex + 1);
    }

    /**
     * Get remaining waypoints.
     */
    public int getRemainingWaypoints() {
        if (currentPath == null) return 0;
        return Math.max(0, currentPath.length() - currentWaypointIndex - 1);
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
        return (double) currentWaypointIndex / (currentPath.length() - 1);
    }

    /**
     * Adjust target position to avoid wall collisions.
     * Currently disabled - just returns the original target.
     *
     * @param ctx         movement context
     * @param currentPos  current entity position
     * @param target      original target waypoint
     * @return original target (no adjustment)
     */
    private Vec3 adjustForWallCollision(MovementContext ctx, Vec3 currentPos, Vec3 target) {
        // Wall collision adjustment disabled - relying on A* and PathSmoother
        // to generate paths that wide entities can actually follow
        return target;
    }
}
