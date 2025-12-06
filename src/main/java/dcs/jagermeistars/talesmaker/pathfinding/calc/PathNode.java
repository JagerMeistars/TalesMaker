package dcs.jagermeistars.talesmaker.pathfinding.calc;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Node in the pathfinding graph.
 * Stores position, costs, and parent for path reconstruction.
 */
public class PathNode implements Comparable<PathNode> {
    private final BlockPos pos;
    private double gCost; // Actual cost from start
    private double hCost; // Heuristic cost to goal
    private PathNode parent;

    // Index in the binary heap for efficient updates
    private int heapIndex = -1;

    public PathNode(BlockPos pos) {
        this.pos = pos.immutable();
        this.gCost = Double.MAX_VALUE;
        this.hCost = 0;
        this.parent = null;
    }

    public PathNode(int x, int y, int z) {
        this(new BlockPos(x, y, z));
    }

    /**
     * Get the total estimated cost (f = g + h).
     */
    public double getFCost() {
        return gCost + hCost;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getX() {
        return pos.getX();
    }

    public int getY() {
        return pos.getY();
    }

    public int getZ() {
        return pos.getZ();
    }

    public double getGCost() {
        return gCost;
    }

    public void setGCost(double gCost) {
        this.gCost = gCost;
    }

    public double getHCost() {
        return hCost;
    }

    public void setHCost(double hCost) {
        this.hCost = hCost;
    }

    public PathNode getParent() {
        return parent;
    }

    public void setParent(PathNode parent) {
        this.parent = parent;
    }

    public int getHeapIndex() {
        return heapIndex;
    }

    public void setHeapIndex(int heapIndex) {
        this.heapIndex = heapIndex;
    }

    /**
     * Check if this node is in the heap.
     */
    public boolean isInHeap() {
        return heapIndex >= 0;
    }

    /**
     * Reset this node for reuse.
     */
    public void reset() {
        gCost = Double.MAX_VALUE;
        hCost = 0;
        parent = null;
        heapIndex = -1;
    }

    @Override
    public int compareTo(PathNode other) {
        int compare = Double.compare(this.getFCost(), other.getFCost());
        if (compare == 0) {
            // Tie-breaker: prefer lower hCost (closer to goal)
            compare = Double.compare(this.hCost, other.hCost);
        }
        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathNode pathNode = (PathNode) o;
        return pos.equals(pathNode.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return "PathNode{" + pos.toShortString() + ", f=" + String.format("%.2f", getFCost()) + "}";
    }

    /**
     * Create a hash code for a position (for use in maps without creating BlockPos).
     */
    public static long posHash(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }
}