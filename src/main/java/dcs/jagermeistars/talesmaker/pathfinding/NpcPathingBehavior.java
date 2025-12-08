package dcs.jagermeistars.talesmaker.pathfinding;

import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import dcs.jagermeistars.talesmaker.pathfinding.calc.PathCalculator;
import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import dcs.jagermeistars.talesmaker.pathfinding.context.WorldContext;
import dcs.jagermeistars.talesmaker.pathfinding.goals.*;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementResult;
import dcs.jagermeistars.talesmaker.pathfinding.movement.SmoothMovementExecutor;
import dcs.jagermeistars.talesmaker.pathfinding.path.PathSmoother;
import dcs.jagermeistars.talesmaker.pathfinding.path.SmoothPath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Main pathfinding behavior controller for NPCs.
 * Manages goal setting, path calculation, and movement execution.
 */
public class NpcPathingBehavior {

    private final NpcEntity npc;
    private final PathCalculator calculator;
    private final SmoothMovementExecutor executor;
    private final PathingConfig config;

    // World context for path smoothing
    private final WorldContext smoothingContext;

    // Current goal
    @Nullable
    private Goal currentGoal;

    // For dynamic goals (follow, patrol)
    @Nullable
    private GoalFollow followGoal;
    @Nullable
    private GoalPatrol patrolGoal;

    // Repath tracking
    private BlockPos lastTargetPos;
    private int repathCooldown;
    private static final int REPATH_COOLDOWN_TICKS = 20; // 1 second
    private static final double REPATH_DISTANCE_SQ = 9.0; // 3 blocks

    // Exact target position (for precise positioning)
    @Nullable
    private Vec3 exactTargetPosition;

    // State
    private boolean active;
    private String state = "idle"; // idle, calculating, moving

    public NpcPathingBehavior(NpcEntity npc) {
        this(npc, PathingConfig.defaultNpc());
    }

    public NpcPathingBehavior(NpcEntity npc, PathingConfig config) {
        this.npc = npc;
        this.config = config;
        this.calculator = new PathCalculator(config);
        this.executor = new SmoothMovementExecutor();
        this.smoothingContext = new WorldContext(config);
        this.active = false;
    }

    /**
     * Get current config with updated NPC dimensions.
     * This ensures we always use the current hitbox size, not the one from construction time.
     */
    private PathingConfig getCurrentConfig() {
        float width = npc.getBbWidth();
        float height = npc.getBbHeight();

        // If dimensions match default, return stored config
        if (Math.abs(width - config.getEntityWidth()) < 0.01f &&
            Math.abs(height - config.getEntityHeight()) < 0.01f) {
            return config;
        }

        // Create updated config with current dimensions
        return PathingConfig.builder()
                .entityWidth(width)
                .entityHeight(height)
                .stepHeight(config.getStepHeight())
                .maxIterations(config.getMaxIterations())
                .maxTimeoutMs(config.getMaxTimeoutMs())
                .maxRange(config.getMaxRange())
                .maxPathLength(config.getMaxPathLength())
                .maxFallDistance(config.getMaxFallDistance())
                .maxJumpHeight(config.getMaxJumpHeight())
                .canSwim(config.canSwim())
                .canClimb(config.canClimb())
                .canOpenDoors(config.canOpenDoors())
                .canParkour(config.canParkour())
                .build();
    }

    /**
     * Called every tick from NpcEntity.tick()
     */
    public void tick() {
        if (!active || currentGoal == null) {
            return;
        }

        // Decrement repath cooldown
        if (repathCooldown > 0) {
            repathCooldown--;
        }

        // Check if we need to repath for dynamic goals
        checkForRepath();

        // Execute current movement
        if (executor.isExecuting()) {
            MovementContext ctx = new MovementContext(npc, getCurrentConfig());
            MovementResult result = executor.tick(ctx);

            switch (result) {
                case SUCCESS:
                    onPathComplete();
                    break;
                case FAILED:
                    onPathFailed();
                    break;
                case IN_PROGRESS:
                    // Continue
                    break;
                case CANCELED:
                    stop();
                    break;
            }
        } else if (!calculator.isCalculating() && state.equals("calculating")) {
            // Calculation finished but no path received - failed
            state = "idle";
        }
    }

    /**
     * Check if we need to recalculate path for dynamic goals.
     */
    private void checkForRepath() {
        if (repathCooldown > 0) {
            return;
        }

        // Check follow goal - target might have moved
        if (followGoal != null && followGoal.isTargetValid()) {
            BlockPos targetPos = followGoal.getTargetPosition();
            if (targetPos != null && shouldRepath(targetPos)) {
                requestPath(followGoal);
                return;
            }
        }

        // Check if executor failed or finished
        if (executor.isFailed()) {
            // Try to repath
            if (currentGoal != null) {
                requestPath(currentGoal);
            }
        }
    }

    /**
     * Check if we should repath based on target movement.
     */
    private boolean shouldRepath(BlockPos newTarget) {
        if (lastTargetPos == null) {
            return true;
        }
        double distSq = lastTargetPos.distSqr(newTarget);
        return distSq > REPATH_DISTANCE_SQ;
    }

    /**
     * Called when path execution completes successfully.
     */
    private void onPathComplete() {
        state = "idle";

        // Handle dynamic goals
        if (patrolGoal != null) {
            // Advance to next waypoint and continue
            patrolGoal.advanceToNextWaypoint();
            requestPath(patrolGoal);
        } else if (followGoal != null && followGoal.isTargetValid()) {
            // Check if target moved, repath if needed
            BlockPos targetPos = followGoal.getTargetPosition();
            if (targetPos != null && !followGoal.isAtGoal(npc.blockPosition())) {
                requestPath(followGoal);
            }
        } else {
            // Static goal reached
            stop();
        }
    }

    /**
     * Called when path execution fails.
     */
    private void onPathFailed() {
        state = "idle";

        // Try repathing after cooldown
        if (currentGoal != null && repathCooldown <= 0) {
            repathCooldown = REPATH_COOLDOWN_TICKS;
            requestPath(currentGoal);
        }
    }

    /**
     * Request a path to the current goal.
     */
    private void requestPath(Goal goal) {
        state = "calculating";
        BlockPos start = npc.blockPosition();

        if (goal instanceof GoalFollow gf && gf.isTargetValid()) {
            lastTargetPos = gf.getTargetPosition();
        } else if (goal instanceof GoalPatrol gp) {
            lastTargetPos = gp.getCurrentWaypointBlockPos();
        } else if (goal instanceof GoalBlock gb) {
            lastTargetPos = gb.getBlockPos();
        } else if (goal instanceof GoalNear gn) {
            lastTargetPos = gn.getBlockPos();
        }

        calculator.requestPath(goal, start, npc.level(), getCurrentConfig(), path -> {
            if (path != null && path.length() > 1) {
                // Capture world data for smoothing (callback runs on main thread)
                smoothingContext.captureRegion(npc.level(), path.getSrc(), path.getDest(), 2);

                // Smooth the path using line-of-sight checks
                SmoothPath smoothed = PathSmoother.smooth(path, smoothingContext, getCurrentConfig());

                // If we have exact target position, replace the last waypoint
                if (exactTargetPosition != null && smoothed != null) {
                    smoothed = replaceLastWaypoint(smoothed, exactTargetPosition);
                }

                executor.setPath(smoothed);
                state = "moving";
            } else {
                state = "idle";
                // Could not find path
                if (currentGoal != null) {
                    repathCooldown = REPATH_COOLDOWN_TICKS * 2; // Longer cooldown on failure
                }
            }
        });

        repathCooldown = REPATH_COOLDOWN_TICKS;
    }

    // ===== Public API =====

    /**
     * Move to a specific block position.
     */
    public void moveToPosition(BlockPos target) {
        moveToPosition(target.getX(), target.getY(), target.getZ());
    }

    /**
     * Move to a specific position with sub-block precision.
     * The NPC will stop exactly at the specified coordinates.
     */
    public void moveToPosition(double x, double y, double z) {
        clearDynamicGoals();
        // Store exact target for precise final positioning
        this.exactTargetPosition = new Vec3(x, y, z);
        // Path to the block containing the target
        GoalBlock goal = new GoalBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        setGoal(goal);
    }

    /**
     * Move near a position (within radius).
     */
    public void moveNear(BlockPos target, int radius) {
        clearDynamicGoals();
        GoalNear goal = new GoalNear(target, radius);
        setGoal(goal);
    }

    /**
     * Move to XZ coordinates (any Y level).
     */
    public void moveToXZ(int x, int z) {
        clearDynamicGoals();
        GoalXZ goal = new GoalXZ(x, z);
        setGoal(goal);
    }

    /**
     * Start following an entity.
     */
    public void startFollow(Entity target) {
        startFollow(target, 2, 32);
    }

    /**
     * Start following an entity with distance parameters.
     */
    public void startFollow(Entity target, int minDistance, int maxDistance) {
        clearDynamicGoals();
        followGoal = new GoalFollow(target, minDistance, maxDistance);
        setGoal(followGoal);
    }

    /**
     * Start patrolling waypoints with Vec3 precision.
     */
    public void startPatrolVec3(List<Vec3> waypoints) {
        startPatrolVec3(waypoints, 0.3, true);
    }

    /**
     * Start patrolling waypoints with Vec3 precision and options.
     */
    public void startPatrolVec3(List<Vec3> waypoints, double tolerance, boolean reverseOnEnd) {
        if (waypoints == null || waypoints.isEmpty()) {
            return;
        }
        clearDynamicGoals();
        patrolGoal = new GoalPatrol(waypoints, tolerance, reverseOnEnd);
        setGoal(patrolGoal);
    }

    /**
     * Start patrolling waypoints (BlockPos version for legacy compatibility).
     */
    public void startPatrol(List<BlockPos> waypoints) {
        startPatrol(waypoints, 1, true);
    }

    /**
     * Start patrolling waypoints with options (BlockPos version for legacy compatibility).
     */
    public void startPatrol(List<BlockPos> waypoints, int tolerance, boolean reverseOnEnd) {
        if (waypoints == null || waypoints.isEmpty()) {
            return;
        }
        clearDynamicGoals();
        patrolGoal = GoalPatrol.fromBlockPositions(waypoints, tolerance, reverseOnEnd);
        setGoal(patrolGoal);
    }

    /**
     * Set a custom goal.
     */
    public void setGoal(Goal goal) {
        currentGoal = goal;
        active = true;
        executor.reset();
        requestPath(goal);
    }

    /**
     * Stop all pathfinding.
     */
    public void stop() {
        active = false;
        state = "idle";
        currentGoal = null;
        calculator.cancel();
        executor.reset();
        clearDynamicGoals();
    }

    /**
     * Clear dynamic goal references.
     */
    private void clearDynamicGoals() {
        followGoal = null;
        patrolGoal = null;
        exactTargetPosition = null;
    }

    /**
     * Replace the last waypoint in a smooth path with an exact target position.
     * This allows sub-block precision for the final destination.
     */
    private SmoothPath replaceLastWaypoint(SmoothPath path, Vec3 exactTarget) {
        List<Vec3> waypoints = path.getWaypoints();
        if (waypoints.isEmpty()) {
            return path;
        }

        // Create a new list with the last waypoint replaced
        List<Vec3> newWaypoints = new java.util.ArrayList<>(waypoints);
        newWaypoints.set(newWaypoints.size() - 1, exactTarget);

        return new SmoothPath(newWaypoints, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Check if currently active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Check if currently calculating a path.
     */
    public boolean isCalculating() {
        return calculator.isCalculating();
    }

    /**
     * Check if currently executing a path.
     */
    public boolean isMoving() {
        return executor.isExecuting();
    }

    /**
     * Get current state.
     */
    public String getState() {
        return state;
    }

    /**
     * Get current goal.
     */
    @Nullable
    public Goal getCurrentGoal() {
        return currentGoal;
    }

    /**
     * Get current smooth path.
     */
    @Nullable
    public SmoothPath getCurrentPath() {
        return executor.getCurrentPath();
    }

    /**
     * Get movement progress (0.0 to 1.0).
     */
    public double getProgress() {
        return executor.getProgress();
    }

    /**
     * Get the pathfinding config.
     */
    public PathingConfig getConfig() {
        return config;
    }
}
