package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.context.WorldContext;
import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import dcs.jagermeistars.talesmaker.pathfinding.movement.PassageAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
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
            // For short paths, still apply centering for wide NPCs
            if (path != null && config.getEntityWidth() > 1.0f) {
                return smoothShortPath(path, context, config);
            }
            return path != null ? SmoothPath.fromPath(path) : null;
        }

        List<BlockPos> original = path.positions();
        List<Vec3> smoothed = new ArrayList<>();
        float entityWidth = config.getEntityWidth();

        // Always include start - use centered position for wide NPCs
        smoothed.add(calculateWaypointPosition(original.get(0), null, context, config));

        int currentIndex = 0;
        while (currentIndex < original.size() - 1) {
            // Find the farthest point we can reach directly
            int farthestVisible = findFarthestVisible(original, currentIndex, context, config);

            // Add the farthest visible point with proper centering
            BlockPos prevPos = original.get(currentIndex);
            BlockPos target = original.get(farthestVisible);
            smoothed.add(calculateWaypointPosition(target, prevPos, context, config));

            currentIndex = farthestVisible;
        }

        return new SmoothPath(smoothed, path.getGoal(), path.getTotalCost(), path.isComplete());
    }

    /**
     * Calculate waypoint position with proper centering for wide NPCs.
     * Uses PassageAnalyzer to find optimal position in passages.
     *
     * @param pos     target block position
     * @param prevPos previous position (for movement direction), or null for start
     * @param context world context
     * @param config  pathing config
     * @return Vec3 waypoint with proper centering
     */
    private static Vec3 calculateWaypointPosition(BlockPos pos, BlockPos prevPos,
                                                   WorldContext context, PathingConfig config) {
        float entityWidth = config.getEntityWidth();
        float entityHeight = config.getEntityHeight();

        // For narrow NPCs, use standard block center
        if (entityWidth <= 1.0f) {
            return Vec3.atBottomCenterOf(pos);
        }

        // For wide NPCs, calculate optimal centered position in passage
        // Check passage width along both axes and center accordingly
        double x = calculateCenteredCoordForPath(pos, Direction.Axis.X, entityWidth, entityHeight, context);
        double z = calculateCenteredCoordForPath(pos, Direction.Axis.Z, entityWidth, entityHeight, context);
        double y = pos.getY();

        System.out.println("[PathSmoother] Wide NPC waypoint: pos=" + pos + " -> (" +
            String.format("%.2f", x) + ", " + y + ", " + String.format("%.2f", z) +
            ") entityWidth=" + entityWidth);

        return new Vec3(x, y, z);
    }

    /**
     * Calculate centered coordinate along an axis for path smoothing.
     * For wide NPCs in narrow passages, centers the NPC in the passage.
     *
     * Key insight: For a passage of width N blocks and NPC width W:
     * - If N == W (e.g., 2-block passage, 2-wide NPC), center must be at block boundary
     * - We check for WALLS (solid blocks at entity height), not just headroom
     */
    private static double calculateCenteredCoordForPath(BlockPos pos, Direction.Axis axis,
                                                         float entityWidth, float entityHeight,
                                                         WorldContext context) {
        double baseCoord = (axis == Direction.Axis.X) ? pos.getX() : pos.getZ();

        // Find walls (solid blocks) on each side at entity's height level
        // A wall is a block that would block passage (solid at y or y+1)
        int wallNegative = findWallDistance(pos, axis, -1, entityHeight, context);
        int wallPositive = findWallDistance(pos, axis, 1, entityHeight, context);

        // Calculate passage boundaries (the last passable positions before walls)
        // wallNegative is negative (e.g., -2 means wall at offset -2)
        // wallPositive is positive (e.g., 2 means wall at offset +2)
        double passageStart = baseCoord + wallNegative + 1; // +1 because wall itself is not passable
        double passageEnd = baseCoord + wallPositive;       // wall position, passage ends before it

        double passageWidth = passageEnd - passageStart;
        double passageCenter = (passageStart + passageEnd) / 2.0;

        System.out.println("[PathSmoother] axis=" + axis + " pos=" +
            (axis == Direction.Axis.X ? pos.getX() : pos.getZ()) +
            " wallNeg=" + wallNegative + " wallPos=" + wallPositive +
            " passageStart=" + passageStart + " passageEnd=" + passageEnd +
            " passageWidth=" + passageWidth + " center=" + passageCenter);

        return passageCenter;
    }

    /**
     * Find distance to nearest wall in given direction.
     * Returns the offset where a wall (solid block) is found.
     *
     * @param pos starting position
     * @param axis axis to search along
     * @param direction -1 for negative, +1 for positive
     * @param entityHeight entity height for headroom check
     * @param context world context
     * @return offset where wall is found (negative for negative direction, positive for positive)
     */
    private static int findWallDistance(BlockPos pos, Direction.Axis axis, int direction,
                                        float entityHeight, WorldContext context) {
        for (int i = 1; i <= 10; i++) {
            int offset = i * direction;
            BlockPos checkPos = offsetByAxis(pos, axis, offset);

            // Check if this position is blocked (wall)
            // A wall is any block that blocks horizontal movement at ANY height level
            // This includes bottom slabs, fences, walls, etc.
            if (isHorizontallyBlocked(checkPos, entityHeight, context)) {
                return offset;
            }
        }
        // No wall found within range - return far boundary
        return 10 * direction;
    }

    /**
     * Check if a position is blocked for horizontal movement.
     * Unlike hasHeadroom which checks vertical space, this checks if ANY solid
     * block would prevent horizontal passage (including bottom slabs, fences, etc.)
     */
    private static boolean isHorizontallyBlocked(BlockPos pos, float entityHeight, WorldContext context) {
        int fullBlocks = (int) Math.ceil(entityHeight);

        // Check all blocks from feet to head level
        for (int dy = 0; dy < fullBlocks; dy++) {
            BlockState state = context.getBlockState(pos.getX(), pos.getY() + dy, pos.getZ());

            // Air is never blocked
            if (state.isAir()) {
                continue;
            }

            // If block has any collision (blocksMotion), it's a wall
            // This catches slabs, fences, walls, etc.
            if (state.blocksMotion()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to offset a BlockPos along a specific axis.
     */
    private static BlockPos offsetByAxis(BlockPos pos, Direction.Axis axis, int offset) {
        return switch (axis) {
            case X -> pos.offset(offset, 0, 0);
            case Y -> pos.offset(0, offset, 0);
            case Z -> pos.offset(0, 0, offset);
        };
    }

    /**
     * Smooth a short path (2 or fewer positions) with proper centering for wide NPCs.
     */
    private static SmoothPath smoothShortPath(IPath path, WorldContext context, PathingConfig config) {
        List<BlockPos> original = path.positions();
        List<Vec3> smoothed = new ArrayList<>();

        for (int i = 0; i < original.size(); i++) {
            BlockPos prevPos = i > 0 ? original.get(i - 1) : null;
            smoothed.add(calculateWaypointPosition(original.get(i), prevPos, context, config));
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
