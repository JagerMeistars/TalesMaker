package dcs.jagermeistars.talesmaker.pathfinding.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Goal for patrolling between waypoints.
 * Cycles through waypoints in order, optionally reversing or looping.
 * Supports sub-block precision with Vec3 coordinates.
 */
public class GoalPatrol implements Goal {

    private final List<Vec3> waypoints;
    private int currentIndex;
    private boolean reverseOnEnd;
    private boolean reversing;
    private final double tolerance;

    /**
     * Create a patrol goal with Vec3 waypoints.
     * @param waypoints List of precise positions to patrol between
     */
    public GoalPatrol(List<Vec3> waypoints) {
        this(waypoints, 0.3, true);
    }

    /**
     * Create a patrol goal with waypoints and options.
     * @param waypoints List of precise positions to patrol between
     * @param tolerance Distance tolerance for reaching waypoints
     * @param reverseOnEnd If true, reverse direction at ends; if false, loop to start
     */
    public GoalPatrol(List<Vec3> waypoints, double tolerance, boolean reverseOnEnd) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints cannot be empty");
        }
        this.waypoints = new ArrayList<>(waypoints);
        this.currentIndex = 0;
        this.tolerance = tolerance;
        this.reverseOnEnd = reverseOnEnd;
        this.reversing = false;
    }

    /**
     * Create a patrol goal from BlockPos waypoints (legacy support).
     * @param blockWaypoints List of block positions
     * @param tolerance Distance tolerance (as int, converted to double)
     * @param reverseOnEnd If true, reverse direction at ends
     */
    public static GoalPatrol fromBlockPositions(List<BlockPos> blockWaypoints, int tolerance, boolean reverseOnEnd) {
        List<Vec3> vec3Waypoints = new ArrayList<>();
        for (BlockPos pos : blockWaypoints) {
            vec3Waypoints.add(Vec3.atBottomCenterOf(pos));
        }
        return new GoalPatrol(vec3Waypoints, tolerance, reverseOnEnd);
    }

    @Override
    public boolean isAtGoal(int x, int y, int z) {
        Vec3 current = getCurrentWaypoint();
        double dx = Math.abs(current.x - (x + 0.5));
        double dy = Math.abs(current.y - y);
        double dz = Math.abs(current.z - (z + 0.5));

        return dx <= tolerance && dy <= tolerance && dz <= tolerance;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        Vec3 current = getCurrentWaypoint();
        double dx = Math.abs(current.x - (x + 0.5));
        double dy = Math.abs(current.y - y);
        double dz = Math.abs(current.z - (z + 0.5));

        double diagonal = Math.min(dx, dz);
        double straight = dx + dz - 2 * diagonal;

        return diagonal * 1.414 + straight + dy * 1.5;
    }

    /**
     * Get the current target waypoint as Vec3.
     */
    public Vec3 getCurrentWaypoint() {
        return waypoints.get(currentIndex);
    }

    /**
     * Get the current target waypoint as BlockPos (for pathfinding).
     */
    public BlockPos getCurrentWaypointBlockPos() {
        Vec3 wp = waypoints.get(currentIndex);
        return new BlockPos((int) Math.floor(wp.x), (int) Math.floor(wp.y), (int) Math.floor(wp.z));
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

    public List<Vec3> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    public double getTolerance() {
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
