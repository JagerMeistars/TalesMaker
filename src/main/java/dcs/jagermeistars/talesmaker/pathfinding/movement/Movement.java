package dcs.jagermeistars.talesmaker.pathfinding.movement;

import net.minecraft.core.BlockPos;

/**
 * Abstract base class for all movement types.
 * Each movement represents a single step from one block to another.
 */
public abstract class Movement {
    protected final BlockPos src;
    protected final BlockPos dest;
    protected MovementState state = MovementState.PREPPING;
    protected int ticksInState = 0;

    // Cached cost for lazy evaluation (Baritone optimization)
    private Double cachedCost = null;

    public Movement(BlockPos src, BlockPos dest) {
        this.src = src.immutable();
        this.dest = dest.immutable();
    }

    /**
     * Calculate the cost of this movement.
     * Called lazily and cached.
     *
     * @param ctx movement context
     * @return movement cost
     */
    public abstract double calculateCost(MovementContext ctx);

    /**
     * Execute one tick of this movement.
     *
     * @param ctx movement context
     * @return result of this tick
     */
    public abstract MovementResult tick(MovementContext ctx);

    /**
     * Check if this movement can be executed.
     *
     * @param ctx movement context
     * @return true if movement is possible
     */
    public abstract boolean canExecute(MovementContext ctx);

    /**
     * Get the current state of this movement.
     */
    public MovementState getState() {
        return state;
    }

    /**
     * Set the movement state.
     */
    protected void setState(MovementState newState) {
        if (this.state != newState) {
            this.state = newState;
            this.ticksInState = 0;
        }
    }

    /**
     * Reset the movement to initial state.
     */
    public void reset() {
        state = MovementState.PREPPING;
        ticksInState = 0;
    }

    /**
     * Get the cost with lazy caching.
     */
    public double getCost(MovementContext ctx) {
        if (cachedCost == null) {
            cachedCost = calculateCost(ctx);
        }
        return cachedCost;
    }

    /**
     * Invalidate the cached cost.
     */
    public void invalidateCost() {
        cachedCost = null;
    }

    /**
     * Get the source position.
     */
    public BlockPos getSrc() {
        return src;
    }

    /**
     * Get the destination position.
     */
    public BlockPos getDest() {
        return dest;
    }

    /**
     * Get ticks spent in current state.
     */
    public int getTicksInState() {
        return ticksInState;
    }

    /**
     * Increment tick counter. Called each tick.
     */
    protected void incrementTicks() {
        ticksInState++;
    }

    /**
     * Check if movement is complete.
     */
    public boolean isComplete() {
        return state == MovementState.COMPLETE;
    }

    /**
     * Check if movement has failed.
     */
    public boolean isFailed() {
        return state == MovementState.FAILED;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + src.toShortString() + " -> " + dest.toShortString() + "}";
    }
}