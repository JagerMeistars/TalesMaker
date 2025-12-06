package dcs.jagermeistars.talesmaker.pathfinding.movement;

/**
 * State machine states for movement execution.
 */
public enum MovementState {
    /**
     * Preparing to execute the movement.
     * Checking prerequisites, positioning.
     */
    PREPPING,

    /**
     * Waiting for the right moment to execute.
     * E.g., waiting for jump timing at edge.
     */
    WAITING,

    /**
     * Actively executing the movement.
     */
    RUNNING,

    /**
     * Movement failed, needs replanning.
     */
    FAILED,

    /**
     * Movement completed successfully.
     */
    COMPLETE
}