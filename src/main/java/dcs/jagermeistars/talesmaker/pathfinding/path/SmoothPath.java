package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A smoothed path with sub-block precision waypoints.
 * Uses Vec3 positions instead of BlockPos for smoother movement.
 */
public class SmoothPath {
    private final List<Vec3> waypoints;
    private final Goal goal;
    private final double totalCost;
    private final boolean complete;

    public SmoothPath(List<Vec3> waypoints, Goal goal, double totalCost, boolean complete) {
        this.waypoints = Collections.unmodifiableList(new ArrayList<>(waypoints));
        this.goal = goal;
        this.totalCost = totalCost;
        this.complete = complete;
    }

    /**
     * Create a SmoothPath from a regular block-based path.
     */
    public static SmoothPath fromPath(IPath path) {
        if (path == null) {
            return null;
        }

        List<Vec3> waypoints = new ArrayList<>();
        for (BlockPos pos : path.positions()) {
            waypoints.add(Vec3.atBottomCenterOf(pos));
        }

        return new SmoothPath(waypoints, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Create a SmoothPath with interpolated waypoints for smoother movement.
     * Does NOT remove any original waypoints - only adds intermediate points.
     *
     * @param path the original path
     * @param maxSegmentLength maximum distance between waypoints (0.5 recommended)
     * @return interpolated smooth path
     */
    public static SmoothPath fromPathInterpolated(IPath path, double maxSegmentLength) {
        if (path == null) {
            return null;
        }

        List<BlockPos> positions = path.positions();
        if (positions.size() <= 1) {
            return fromPath(path);
        }

        List<Vec3> waypoints = new ArrayList<>();
        waypoints.add(Vec3.atBottomCenterOf(positions.get(0)));

        for (int i = 0; i < positions.size() - 1; i++) {
            Vec3 from = Vec3.atBottomCenterOf(positions.get(i));
            Vec3 to = Vec3.atBottomCenterOf(positions.get(i + 1));

            double distance = from.distanceTo(to);

            if (distance > maxSegmentLength) {
                // Add intermediate points
                int segments = (int) Math.ceil(distance / maxSegmentLength);
                for (int j = 1; j <= segments; j++) {
                    double t = (double) j / segments;
                    Vec3 intermediate = from.lerp(to, t);
                    waypoints.add(intermediate);
                }
            } else {
                waypoints.add(to);
            }
        }

        return new SmoothPath(waypoints, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Get all waypoints.
     */
    public List<Vec3> getWaypoints() {
        return waypoints;
    }

    /**
     * Get waypoint at index.
     */
    public Vec3 getWaypoint(int index) {
        return waypoints.get(index);
    }

    /**
     * Get number of waypoints.
     */
    public int length() {
        return waypoints.size();
    }

    /**
     * Get the goal.
     */
    public Goal getGoal() {
        return goal;
    }

    /**
     * Get total estimated cost.
     */
    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Check if path is complete.
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Get start position.
     */
    public Vec3 getStart() {
        return waypoints.isEmpty() ? null : waypoints.get(0);
    }

    /**
     * Get end position.
     */
    public Vec3 getEnd() {
        return waypoints.isEmpty() ? null : waypoints.get(waypoints.size() - 1);
    }

    /**
     * Calculate total path length (sum of all segment distances).
     */
    public double calculateTotalLength() {
        if (waypoints.size() < 2) {
            return 0;
        }

        double length = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            length += waypoints.get(i).distanceTo(waypoints.get(i + 1));
        }
        return length;
    }

    /**
     * Get the direction from one waypoint to the next.
     *
     * @param index waypoint index
     * @return normalized direction vector, or Vec3.ZERO if at end
     */
    public Vec3 getDirection(int index) {
        if (index >= waypoints.size() - 1) {
            return Vec3.ZERO;
        }

        Vec3 from = waypoints.get(index);
        Vec3 to = waypoints.get(index + 1);
        Vec3 delta = to.subtract(from);

        double length = delta.length();
        if (length < 0.001) {
            return Vec3.ZERO;
        }

        return delta.scale(1.0 / length);
    }

    @Override
    public String toString() {
        return "SmoothPath{" +
                "waypoints=" + waypoints.size() +
                ", cost=" + String.format("%.2f", totalCost) +
                ", complete=" + complete +
                ", length=" + String.format("%.2f", calculateTotalLength()) +
                '}';
    }
}
