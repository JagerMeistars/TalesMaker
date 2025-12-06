package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Goal for patrolling between waypoints.
 * Cycles through waypoints in order, optionally reversing or looping.
 */
public class GoalPatrol implements Goal {

    private final List<BlockPos> waypoints;
    private int currentIndex;
    private boolean reverseOnEnd;
    private boolean reversing;
    private final int tolerance;

    /**
     * Create a patrol goal with waypoints.
     * @param waypoints List of positions to patrol between
     */
    public GoalPatrol(List<BlockPos> waypoints) {
        this(waypoints, 1, true);
    }

    /**
     * Create a patrol goal with waypoints and options.
     * @param waypoints List of positions to patrol between
     * @param tolerance Distance tolerance for reaching waypoints
     * @param reverseOnEnd If true, reverse direction at ends; if false, loop to start
     */
    public GoalPatrol(List<BlockPos> waypoints, int tolerance, boolean reverseOnEnd) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints cannot be empty");
        }
        this.waypoints = new ArrayList<>(waypoints);
        this.currentIndex = 0;
        this.tolerance = tolerance;
        this.reverseOnEnd = reverseOnEnd;
        this.reversing = false;
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        BlockPos current = getCurrentWaypoint();
        int dx = Math.abs(current.getX() - x);
        int dy = Math.abs(current.getY() - y);
        int dz = Math.abs(current.getZ() - z);

        return dx <= tolerance && dy <= tolerance && dz <= tolerance;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        BlockPos current = getCurrentWaypoint();
        int dx = Math.abs(current.getX() - x);
        int dy = Math.abs(current.getY() - y);
        int dz = Math.abs(current.getZ() - z);

        int diagonal = Math.min(dx, dz);
        int straight = dx + dz - 2 * diagonal;

        return diagonal * 1.414 + straight + dy * 1.5;
    }

    /**
     * Get the current target waypoint.
     */
    public BlockPos getCurrentWaypoint() {
        return waypoints.get(currentIndex);
    }

    /**
     * Get the index of the current waypoint.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Advance to the next waypoint.
     * Called when the current waypoint is reached.
     * @return true if there are more waypoints, false if patrol is complete (only for non-looping)
     */
    public boolean advanceToNextWaypoint() {
        if (waypoints.size() == 1) {
            return true; // Single waypoint, always stay
        }

        if (reverseOnEnd) {
            // Ping-pong between endpoints
            if (reversing) {
                currentIndex--;
                if (currentIndex <= 0) {
                    currentIndex = 0;
                    reversing = false;
                }
            } else {
                currentIndex++;
                if (currentIndex >= waypoints.size() - 1) {
                    currentIndex = waypoints.size() - 1;
                    reversing = true;
                }
            }
        } else {
            // Loop back to start
            currentIndex = (currentIndex + 1) % waypoints.size();
        }

        return true;
    }

    /**
     * Reset patrol to the first waypoint.
     */
    public void reset() {
        currentIndex = 0;
        reversing = false;
    }

    /**
     * Set the current waypoint index directly.
     */
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < waypoints.size()) {
            this.currentIndex = index;
        }
    }

    public List<BlockPos> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    public int getTolerance() {
        return tolerance;
    }

    public boolean isReverseOnEnd() {
        return reverseOnEnd;
    }

    public boolean isReversing() {
        return reversing;
    }

    @Override
    public String toString() {
        return "GoalPatrol{waypoint=" + (currentIndex + 1) + "/" + waypoints.size() +
                " at=" + getCurrentWaypoint() + "}";
    }
}
