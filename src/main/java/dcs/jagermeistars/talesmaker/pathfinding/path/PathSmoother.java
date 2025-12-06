package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.context.WorldContext;
import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Smooths calculated paths using line-of-sight checks.
 * Removes unnecessary waypoints and creates smoother movement.
 *
 * This effectively gives "16+ directions" by allowing direct movement
 * between non-adjacent waypoints when there's clear line of sight.
 */
public final class PathSmoother {

    private PathSmoother() {
        // Utility class
    }

    /**
     * Smooth a path by removing unnecessary waypoints.
     * Uses line-of-sight checks to skip intermediate points.
     *
     * @param path    the original path
     * @param context world context for collision checks
     * @param config  pathing config for entity dimensions
     * @return smoothed path with fewer waypoints
     */
    public static SmoothPath smooth(IPath path, WorldContext context, PathingConfig config) {
        if (path == null || path.length() <= 2) {
            return path != null ? SmoothPath.fromPath(path) : null;
        }

        List<BlockPos> original = path.positions();
        List<Vec3> smoothed = new ArrayList<>();

        // Always include start
        smoothed.add(Vec3.atBottomCenterOf(original.get(0)));

        int currentIndex = 0;
        while (currentIndex < original.size() - 1) {
            // Find the farthest point we can reach directly
            int farthestVisible = findFarthestVisible(original, currentIndex, context, config);

            // Add the farthest visible point
            BlockPos target = original.get(farthestVisible);
            smoothed.add(Vec3.atBottomCenterOf(target));

            currentIndex = farthestVisible;
        }

        return new SmoothPath(smoothed, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Find the farthest point from current that can be reached in a straight line.
     */
    private static int findFarthestVisible(List<BlockPos> positions, int currentIndex,
                                           WorldContext context, PathingConfig config) {
        int farthest = currentIndex + 1;
        BlockPos current = positions.get(currentIndex);

        // Check each subsequent point
        for (int i = currentIndex + 2; i < positions.size(); i++) {
            BlockPos target = positions.get(i);

            // Don't skip points with significant Y change (jumps/falls need intermediate handling)
            int dy = target.getY() - current.getY();
            if (Math.abs(dy) > 0) {
                break;
            }

            // Check line of sight
            if (hasLineOfSight(current, target, context, config)) {
                farthest = i;
            } else {
                break; // No point checking further if this one fails
            }
        }

        return farthest;
    }

    /**
     * Check if there's a clear line of sight between two positions.
     * Uses Bresenham-style stepping to check all blocks along the path.
     * For wide entities, also checks that corners won't be clipped.
     */
    private static boolean hasLineOfSight(BlockPos from, BlockPos to,
                                          WorldContext context, PathingConfig config) {
        float width = config.getEntityWidth();
        float height = config.getEntityHeight();

        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        // For wide entities, don't allow diagonal smoothing at all
        // This prevents corner clipping issues
        if (width > 1.0f && dx != 0 && dz != 0) {
            // Diagonal movement for wide entity - check if it's safe
            // Wide entities can clip corners, so be very conservative
            int signX = dx > 0 ? 1 : -1;
            int signZ = dz > 0 ? 1 : -1;

            // Check all intermediate positions including the corners
            int absX = Math.abs(dx);
            int absZ = Math.abs(dz);

            for (int ix = 0; ix <= absX; ix++) {
                for (int iz = 0; iz <= absZ; iz++) {
                    int checkX = from.getX() + ix * signX;
                    int checkZ = from.getZ() + iz * signZ;
                    int checkY = from.getY();

                    if (!canEntityStandAt(checkX, checkY, checkZ, width, height, context)) {
                        return false;
                    }
                }
            }
            return true;
        }

        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) return true;

        double stepX = (double) dx / steps;
        double stepZ = (double) dz / steps;

        // Check each position along the line
        for (int i = 0; i <= steps; i++) {
            int checkX = from.getX() + (int) Math.round(stepX * i);
            int checkZ = from.getZ() + (int) Math.round(stepZ * i);
            int checkY = from.getY();

            // Check if entity can stand here
            if (!canEntityStandAt(checkX, checkY, checkZ, width, height, context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if an entity can stand at a position considering dimensions.
     */
    private static boolean canEntityStandAt(int x, int y, int z, float width, float height,
                                            WorldContext context) {
        // Use WorldContext's footprint check which handles width correctly
        if (width > 1.0f) {
            return context.hasFootprint(x, y, z, width, height);
        }

        // Standard check for normal-sized entities
        if (!context.isSolid(x, y - 1, z)) {
            return false;
        }
        return context.hasHeadroom(x, y, z, height);
    }

    /**
     * Subdivide a smoothed path to add intermediate waypoints for smoother visual movement.
     * This creates the "16 direction" effect by adding sub-block precision points.
     *
     * @param path          the smoothed path
     * @param maxSegmentLength maximum distance between waypoints
     * @return path with subdivided segments
     */
    public static SmoothPath subdivide(SmoothPath path, double maxSegmentLength) {
        if (path == null || path.getWaypoints().size() <= 1) {
            return path;
        }

        List<Vec3> original = path.getWaypoints();
        List<Vec3> subdivided = new ArrayList<>();
        subdivided.add(original.get(0));

        for (int i = 0; i < original.size() - 1; i++) {
            Vec3 from = original.get(i);
            Vec3 to = original.get(i + 1);

            double distance = from.distanceTo(to);

            if (distance > maxSegmentLength) {
                // Subdivide this segment
                int segments = (int) Math.ceil(distance / maxSegmentLength);
                for (int j = 1; j <= segments; j++) {
                    double t = (double) j / segments;
                    Vec3 intermediate = from.lerp(to, t);
                    subdivided.add(intermediate);
                }
            } else {
                subdivided.add(to);
            }
        }

        return new SmoothPath(subdivided, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Full smoothing pipeline: smooth then subdivide.
     *
     * @param path              original block-based path
     * @param context           world context
     * @param config            pathing config
     * @param maxSegmentLength  maximum segment length for subdivision (0.5-1.0 recommended)
     * @return fully smoothed path
     */
    public static SmoothPath smoothAndSubdivide(IPath path, WorldContext context,
                                                 PathingConfig config, double maxSegmentLength) {
        SmoothPath smoothed = smooth(path, context, config);
        return subdivide(smoothed, maxSegmentLength);
    }
}
