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

    // Stuck detection
    private Vec3 lastPosition;
    private int stuckTicks;
    private static final int STUCK_THRESHOLD = 20; // 1 second at 20 TPS
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

    // Door interaction
    private static final double DOOR_INTERACT_DISTANCE_SQ = 2.25; // 1.5 blocks

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

        // Check for doors ahead and open them
        tryOpenDoorsAhead(ctx, currentPos, targetWaypoint);

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

        // Handle Y level changes (jumping/falling)
        double dy = targetWaypoint.y - currentPos.y;
        if (dy > 0.5 && ctx.isOnGround()) {
            // Need to jump
            MovementHelper.jumpTowards(ctx.getEntity(), targetWaypoint);
        } else {
            MovementHelper.moveTowards(ctx.getEntity(), targetWaypoint);
        }

        return MovementResult.IN_PROGRESS;
    }

    /**
     * Check for closed doors between current position and target, and open them.
     */
    private void tryOpenDoorsAhead(MovementContext ctx, Vec3 currentPos, Vec3 targetWaypoint) {
        // Check current block and next few blocks towards target for doors
        BlockPos currentBlock = new BlockPos((int) Math.floor(currentPos.x),
                                              (int) Math.floor(currentPos.y),
                                              (int) Math.floor(currentPos.z));
        BlockPos targetBlock = new BlockPos((int) Math.floor(targetWaypoint.x),
                                             (int) Math.floor(targetWaypoint.y),
                                             (int) Math.floor(targetWaypoint.z));

        // Check blocks between current and target (up to 2 blocks ahead)
        int dx = Integer.compare(targetBlock.getX(), currentBlock.getX());
        int dz = Integer.compare(targetBlock.getZ(), currentBlock.getZ());

        for (int i = 0; i <= 2; i++) {
            BlockPos checkPos = currentBlock.offset(dx * i, 0, dz * i);

            // Check both at feet level and head level (doors are 2 blocks tall)
            tryOpenDoorAt(ctx, currentPos, checkPos);
            tryOpenDoorAt(ctx, currentPos, checkPos.above());
        }
    }

    /**
     * Try to open a door at the given position if it's closed and within range.
     */
    private void tryOpenDoorAt(MovementContext ctx, Vec3 currentPos, BlockPos doorPos) {
        BlockState state = ctx.getBlockState(doorPos);

        if (!(state.getBlock() instanceof DoorBlock door)) {
            return;
        }

        // Check if door is closed
        boolean isOpen = state.getValue(DoorBlock.OPEN);
        if (isOpen) {
            return;
        }

        // Check if within interaction range
        Vec3 doorCenter = Vec3.atCenterOf(doorPos);
        double distSq = currentPos.distanceToSqr(doorCenter);

        if (distSq < DOOR_INTERACT_DISTANCE_SQ) {
            // Open the door
            door.setOpen(ctx.getEntity(), ctx.getLevel(), state, doorPos, true);
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
}
