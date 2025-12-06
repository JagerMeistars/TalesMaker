package dcs.jagermeistars.talesmaker.pathfinding.movement;

/**
 * Result of a movement tick execution.
 */
public enum MovementResult {
    /**
     * Movement is still in progress, continue next tick.
     */
    IN_PROGRESS,

    /**
     * Movement completed successfully, advance to next.
     */
    SUCCESS,

    /**
     * Movement failed, need to recalculate path.
     */
    FAILED,

    /**
     * Movement was canceled externally.
     */
    CANCELED
}