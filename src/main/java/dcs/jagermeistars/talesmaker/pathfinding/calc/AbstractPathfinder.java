package dcs.jagermeistars.talesmaker.pathfinding.calc;

import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import dcs.jagermeistars.talesmaker.pathfinding.context.WorldContext;
import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import dcs.jagermeistars.talesmaker.pathfinding.path.CutoffPath;
import dcs.jagermeistars.talesmaker.pathfinding.path.IPath;
import dcs.jagermeistars.talesmaker.pathfinding.path.Path;
import net.minecraft.core.BlockPos;

import java.util.*;

/**
 * A* pathfinder implementation with Baritone-inspired optimizations.
 */
public class AbstractPathfinder {
    private final Goal goal;
    private final WorldContext context;
    private final PathingConfig config;

    // A* data structures
    private final BinaryHeapOpenSet openSet;
    private final Map<Long, PathNode> nodeMap;

    // Statistics
    private int nodesEvaluated;
    private long computeTimeNanos;

    // Time check interval (Baritone optimization: check every 64 nodes)
    private static final int TIME_CHECK_INTERVAL = 64;

    public AbstractPathfinder(Goal goal, WorldContext context) {
        this.goal = goal;
        this.context = context;
        this.config = context.getConfig();
        this.openSet = new BinaryHeapOpenSet();
        this.nodeMap = new HashMap<>();
    }

    /**
     * Calculate a path from start to goal.
     *
     * @param start     starting position
     * @param maxNodes  maximum nodes to evaluate
     * @param maxTimeMs maximum time in milliseconds
     * @return the calculated path
     */
    public IPath calculate(BlockPos start, int maxNodes, long maxTimeMs) {
        long startTime = System.nanoTime();
        long maxTimeNanos = maxTimeMs * 1_000_000L;
        nodesEvaluated = 0;

        // Debug: Check start position validity
        boolean canStand = context.canStandAt(start.getX(), start.getY(), start.getZ());
        // Initialize start node
        PathNode startNode = getOrCreateNode(start);
        startNode.setGCost(0);
        startNode.setHCost(goal.heuristic(start));
        openSet.insert(startNode);

        // Track best partial path
        PathNode bestNode = startNode;
        double bestHeuristic = startNode.getHCost();

        while (!openSet.isEmpty()) {
            // Time check every N nodes (Baritone optimization)
            if ((nodesEvaluated & (TIME_CHECK_INTERVAL - 1)) == 0) {
                if (System.nanoTime() - startTime > maxTimeNanos) {
                    computeTimeNanos = System.nanoTime() - startTime;
                    return createPartialPath(bestNode, start);
                }
            }

            // Node limit check
            if (nodesEvaluated >= maxNodes) {
                computeTimeNanos = System.nanoTime() - startTime;
                return createPartialPath(bestNode, start);
            }

            PathNode current = openSet.poll();
            nodesEvaluated++;

            // Goal check
            if (goal.isAtGoal(current.getX(), current.getY(), current.getZ())) {
                computeTimeNanos = System.nanoTime() - startTime;
                return createCompletePath(current, start);
            }

            // Update best partial path
            double currentH = current.getHCost();
            if (currentH < bestHeuristic) {
                bestHeuristic = currentH;
                bestNode = current;
            }

            // Expand neighbors
            expandNode(current);
        }

        // No path found
        computeTimeNanos = System.nanoTime() - startTime;
        if (bestNode != startNode) {
            return createPartialPath(bestNode, start);
        }
        return null;
    }

    // Debug counter for first few nodes
    private int debugCount = 0;

    /**
     * Expand a node by evaluating all possible movements from it.
     */
    private void expandNode(PathNode current) {
        int x = current.getX();
        int y = current.getY();
        int z = current.getZ();

        // Debug first 3 expansions
        if (debugCount < 3) {
            System.out.println("[Pathfinding DEBUG] Expanding node at: " + x + ", " + y + ", " + z);
            System.out.println("[Pathfinding DEBUG]   Cardinal checks:");
            System.out.println("[Pathfinding DEBUG]   +X: canStand=" + context.canStandAt(x+1, y, z));
            System.out.println("[Pathfinding DEBUG]   -X: canStand=" + context.canStandAt(x-1, y, z));
            System.out.println("[Pathfinding DEBUG]   +Z: canStand=" + context.canStandAt(x, y, z+1));
            System.out.println("[Pathfinding DEBUG]   -Z: canStand=" + context.canStandAt(x, y, z-1));
            debugCount++;
        }

        // Cardinal directions
        tryMove(current, x + 1, y, z, ActionCosts.WALK);
        tryMove(current, x - 1, y, z, ActionCosts.WALK);
        tryMove(current, x, y, z + 1, ActionCosts.WALK);
        tryMove(current, x, y, z - 1, ActionCosts.WALK);

        // Diagonal directions (4)
        if (canMoveDiagonal(x, y, z, 1, 1)) {
            tryMove(current, x + 1, y, z + 1, ActionCosts.DIAGONAL);
        }
        if (canMoveDiagonal(x, y, z, 1, -1)) {
            tryMove(current, x + 1, y, z - 1, ActionCosts.DIAGONAL);
        }
        if (canMoveDiagonal(x, y, z, -1, 1)) {
            tryMove(current, x - 1, y, z + 1, ActionCosts.DIAGONAL);
        }
        if (canMoveDiagonal(x, y, z, -1, -1)) {
            tryMove(current, x - 1, y, z - 1, ActionCosts.DIAGONAL);
        }

        // Jump up (in all 4 cardinal directions)
        tryJumpUp(current, x + 1, y + 1, z);
        tryJumpUp(current, x - 1, y + 1, z);
        tryJumpUp(current, x, y + 1, z + 1);
        tryJumpUp(current, x, y + 1, z - 1);

        // Step/fall down (in all 4 cardinal directions)
        tryDescend(current, x + 1, y, z);
        tryDescend(current, x - 1, y, z);
        tryDescend(current, x, y, z + 1);
        tryDescend(current, x, y, z - 1);

        // Climb up/down (if on climbable)
        if (context.isClimbable(x, y, z)) {
            tryMove(current, x, y + 1, z, ActionCosts.CLIMB);
            tryMove(current, x, y - 1, z, ActionCosts.CLIMB);
        }

        // Parkour jumps (2-block gaps in cardinal directions)
        if (config.canParkour()) {
            tryParkour(current, x + 2, y, z, x + 1);
            tryParkour(current, x - 2, y, z, x - 1);
            tryParkour(current, x, y, z + 2, z + 1);
            tryParkour(current, x, y, z - 2, z - 1);
        }
    }

    /**
     * Try a basic movement to a position.
     */
    private void tryMove(PathNode from, int toX, int toY, int toZ, double baseCost) {
        if (!context.canStandAt(toX, toY, toZ)) {
            return;
        }

        double cost = baseCost;

        // Water penalty
        if (context.isWater(toX, toY, toZ)) {
            if (!config.canSwim()) return;
            cost += ActionCosts.WATER_PENALTY;
        }

        // Door handling
        if (context.isDoor(toX, toY, toZ)) {
            if (!config.canOpenDoors() && !context.isDoorOpen(toX, toY, toZ)) {
                return;
            }
            cost += ActionCosts.DOOR - ActionCosts.WALK;
        }

        updateNode(from, toX, toY, toZ, cost);
    }

    /**
     * Check if diagonal movement is allowed (no corner cutting).
     * For wide entities, diagonal movement is restricted near walls.
     */
    private boolean canMoveDiagonal(int x, int y, int z, int dx, int dz) {
        float width = config.getEntityWidth();

        // Standard check first: both adjacent cardinal positions are passable
        if (!context.isPassable(x + dx, y, z) || !context.isPassable(x, y, z + dz)) {
            return false;
        }
        if (!context.canStandAt(x + dx, y, z + dz)) {
            return false;
        }

        // For wide entities, add extra check for the "inner corner"
        // When moving diagonally past a corner, wide entity clips through it
        if (width > 1.0f) {
            // Check if there's a wall at the corner we're cutting
            // The corner is at (x, z) relative to start, opposite to movement direction
            int cornerX = x - dx;
            int cornerZ = z - dz;

            // If there's a solid block at the inner corner, don't allow diagonal
            // This prevents wide entities from clipping through corners
            if (context.isSolid(cornerX, y, cornerZ) || context.isSolid(cornerX, y + 1, cornerZ)) {
                return false;
            }

            // Also check the outer corners that wide entity might hit
            if (context.isSolid(x + dx, y, cornerZ) || context.isSolid(x + dx, y + 1, cornerZ)) {
                return false;
            }
            if (context.isSolid(cornerX, y, z + dz) || context.isSolid(cornerX, y + 1, z + dz)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Try jumping up one block.
     */
    private void tryJumpUp(PathNode from, int toX, int toY, int toZ) {
        int fromX = from.getX();
        int fromY = from.getY();
        int fromZ = from.getZ();

        // Need headroom at start position for jump
        int height = (int) Math.ceil(config.getEntityHeight());
        if (!context.hasHeadroom(fromX, fromY, fromZ, height + 1)) {
            return;
        }

        // Destination must be standable
        if (!context.canStandAt(toX, toY, toZ)) {
            return;
        }

        updateNode(from, toX, toY, toZ, ActionCosts.JUMP_UP);
    }

    /**
     * Try stepping or falling down.
     */
    private void tryDescend(PathNode from, int toX, int toY, int toZ) {
        // Check if we can walk directly (same level or step down via step height)
        if (context.canStandAt(toX, toY, toZ)) {
            // Already handled by tryMove
            return;
        }

        // Check for fall/step down
        for (int fallDist = 1; fallDist <= config.getMaxFallDistance(); fallDist++) {
            int landY = toY - fallDist;
            if (context.canStandAt(toX, landY, toZ)) {
                double cost = fallDist == 1 ? ActionCosts.STEP_DOWN : ActionCosts.fallCost(fallDist);
                updateNode(from, toX, landY, toZ, cost);
                return;
            }
        }
    }

    /**
     * Try a parkour jump across a gap.
     */
    private void tryParkour(PathNode from, int toX, int toY, int toZ, int gapCheck) {
        int fromX = from.getX();
        int fromY = from.getY();
        int fromZ = from.getZ();

        // Gap must be air
        int gapX = (toX != fromX) ? gapCheck : fromX;
        int gapZ = (toZ != fromZ) ? gapCheck : fromZ;

        if (!context.isPassable(gapX, fromY, gapZ) || context.isSolid(gapX, fromY - 1, gapZ)) {
            return; // No gap or gap has ground
        }

        // Need headroom for jump arc
        int height = (int) Math.ceil(config.getEntityHeight());
        if (!context.hasHeadroom(fromX, fromY, fromZ, height + 1)) {
            return;
        }

        // Destination must be standable
        if (!context.canStandAt(toX, toY, toZ)) {
            return;
        }

        updateNode(from, toX, toY, toZ, ActionCosts.parkourCost(2));
    }

    /**
     * Update or create a node with a new path cost.
     */
    private void updateNode(PathNode from, int toX, int toY, int toZ, double movementCost) {
        double newG = from.getGCost() + movementCost;
        PathNode neighbor = getOrCreateNode(toX, toY, toZ);

        if (newG < neighbor.getGCost()) {
            neighbor.setGCost(newG);
            neighbor.setHCost(goal.heuristic(toX, toY, toZ));
            neighbor.setParent(from);

            if (neighbor.isInHeap()) {
                openSet.update(neighbor);
            } else {
                openSet.insert(neighbor);
            }
        }
    }

    /**
     * Get or create a node for a position.
     */
    private PathNode getOrCreateNode(BlockPos pos) {
        return getOrCreateNode(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Get or create a node for coordinates.
     */
    private PathNode getOrCreateNode(int x, int y, int z) {
        long hash = BlockPos.asLong(x, y, z);
        PathNode node = nodeMap.get(hash);
        if (node == null) {
            node = new PathNode(x, y, z);
            nodeMap.put(hash, node);
        }
        return node;
    }

    /**
     * Create a complete path from the goal node.
     */
    private IPath createCompletePath(PathNode endNode, BlockPos start) {
        List<BlockPos> positions = reconstructPositions(endNode);
        List<Movement> movements = Collections.emptyList(); // Will be populated by MovementExecutor
        return new Path(positions, movements, goal, endNode.getGCost(), true);
    }

    /**
     * Create a partial path from the best node found.
     */
    private IPath createPartialPath(PathNode bestNode, BlockPos start) {
        List<BlockPos> positions = reconstructPositions(bestNode);
        List<Movement> movements = Collections.emptyList();
        return new CutoffPath(positions, movements, goal, bestNode.getGCost());
    }

    /**
     * Reconstruct the path from end node to start.
     */
    private List<BlockPos> reconstructPositions(PathNode endNode) {
        List<BlockPos> positions = new ArrayList<>();
        PathNode current = endNode;

        while (current != null) {
            positions.add(current.getPos());
            current = current.getParent();

            // Safety limit
            if (positions.size() > config.getMaxPathLength()) {
                break;
            }
        }

        Collections.reverse(positions);
        return positions;
    }

    // Statistics getters
    public int getNodesEvaluated() {
        return nodesEvaluated;
    }

    public long getComputeTimeNanos() {
        return computeTimeNanos;
    }

    public double getComputeTimeMs() {
        return computeTimeNanos / 1_000_000.0;
    }
}