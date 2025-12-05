package dcs.jagermeistars.talesmaker.pathfinding.core;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Узел пути для A* алгоритма
 */
public class PathNode implements Comparable<PathNode> {
    public final int x, y, z;

    @Nullable
    public PathNode parent;

    public float gCost;  // Стоимость от старта
    public float hCost;  // Эвристика до цели
    public PathType type;
    public boolean visited;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.gCost = Float.MAX_VALUE;
        this.hCost = 0;
        this.type = PathType.OPEN;
        this.visited = false;
    }

    public PathNode(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public float getFCost() {
        return gCost + hCost;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public long asLong() {
        return BlockPos.asLong(x, y, z);
    }

    @Override
    public int compareTo(PathNode other) {
        int fCompare = Float.compare(this.getFCost(), other.getFCost());
        if (fCompare != 0) return fCompare;
        return Float.compare(this.hCost, other.hCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathNode other)) return false;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(asLong());
    }

    @Override
    public String toString() {
        return String.format("PathNode[%d, %d, %d](g=%.1f, h=%.1f, f=%.1f)",
            x, y, z, gCost, hCost, getFCost());
    }
}
