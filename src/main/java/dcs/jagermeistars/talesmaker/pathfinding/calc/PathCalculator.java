package dcs.jagermeistars.talesmaker.pathfinding.calc;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import dcs.jagermeistars.talesmaker.pathfinding.context.WorldContext;
import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import dcs.jagermeistars.talesmaker.pathfinding.path.IPath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Asynchronous path calculator.
 * Runs pathfinding in a separate thread to avoid blocking the main game thread.
 */
public class PathCalculator {
    // Single thread executor for pathfinding
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "TalesMaker-Pathfinding");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final PathingConfig config;
    private CompletableFuture<IPath> currentCalculation;
    private volatile boolean canceled = false;

    public PathCalculator(PathingConfig config) {
        this.config = config;
    }

    public PathCalculator() {
        this(PathingConfig.defaultNpc());
    }

    /**
     * Request a new path calculation.
     * Any existing calculation will be canceled.
     *
     * @param goal       the goal to reach
     * @param start      starting position
     * @param level      the world (for capturing blocks)
     * @param onComplete callback when path is ready (called on main thread)
     */
    public void requestPath(Goal goal, BlockPos start, Level level, Consumer<IPath> onComplete) {
        requestPath(goal, start, level, config, onComplete);
    }

    /**
     * Request a new path calculation with custom config.
     *
     * @param goal       the goal to reach
     * @param start      starting position
     * @param level      the world
     * @param pathConfig pathfinding configuration
     * @param onComplete callback when path is ready
     */
    public void requestPath(Goal goal, BlockPos start, Level level, PathingConfig pathConfig,
                            Consumer<IPath> onComplete) {
        // Cancel any existing calculation
        cancel();
        canceled = false;

        // Capture world region on main thread
        // Use smaller radius to avoid memory issues (32 blocks = ~274k blocks max)
        WorldContext context = new WorldContext(pathConfig);
        int captureRadius = (int) Math.min(pathConfig.getMaxRange(), 32);
        context.captureRegion(level, start, captureRadius);

        // Debug: Check if start position is valid
        boolean canStandAtStart = context.canStandAt(start.getX(), start.getY(), start.getZ());
        boolean hasGroundBelow = context.isSolid(start.getX(), start.getY() - 1, start.getZ());
        boolean isPassableAtStart = context.isPassable(start.getX(), start.getY(), start.getZ());
        TalesMaker.LOGGER.debug("[Pathfinding] Start check: pos={}, canStand={}, ground={}, passable={}",
            start, canStandAtStart, hasGroundBelow, isPassableAtStart);

        // Start async calculation
        currentCalculation = CompletableFuture.supplyAsync(() -> {
            if (canceled) return null;

            AbstractPathfinder pathfinder = new AbstractPathfinder(goal, context);
            return pathfinder.calculate(
                    start,
                    pathConfig.getMaxIterations(),
                    pathConfig.getMaxTimeoutMs()
            );
        }, EXECUTOR).whenComplete((path, error) -> {
            if (canceled) return;

            if (error != null) {
                // Log error but don't crash
                scheduleCallback(level, () -> onComplete.accept(null));
            } else {
                scheduleCallback(level, () -> onComplete.accept(path));
            }
        });
    }

    /**
     * Request path to a specific block position.
     */
    public void requestPathTo(BlockPos target, BlockPos start, Level level, Consumer<IPath> onComplete) {
        requestPath(new SimpleGoalBlock(target), start, level, onComplete);
    }

    /**
     * Cancel the current calculation.
     */
    public void cancel() {
        canceled = true;
        if (currentCalculation != null && !currentCalculation.isDone()) {
            currentCalculation.cancel(true);
        }
        currentCalculation = null;
    }

    /**
     * Check if a calculation is in progress.
     */
    public boolean isCalculating() {
        return currentCalculation != null && !currentCalculation.isDone();
    }

    /**
     * Check if calculation was canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Get the current config.
     */
    public PathingConfig getConfig() {
        return config;
    }

    /**
     * Schedule a callback on the main server thread.
     */
    private void scheduleCallback(Level level, Runnable callback) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(callback);
        } else {
            // Fallback: just run it (might be on wrong thread)
            callback.run();
        }
    }

    /**
     * Shutdown the pathfinding executor.
     * Should be called when the mod is unloading.
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(1, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simple goal implementation for reaching a specific block.
     * Used internally for requestPathTo().
     */
    private static class SimpleGoalBlock implements Goal {
        private final int x, y, z;

        SimpleGoalBlock(BlockPos pos) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }

        @Override
        public boolean isAtGoal(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return ActionCosts.octileDistance(x, y, z, this.x, this.y, this.z);
        }
    }
}
