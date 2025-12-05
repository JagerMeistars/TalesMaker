package dcs.jagermeistars.talesmaker.pathfinding.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Результат поиска пути - последовательность узлов
 */
public class CustomPath {
    private final List<PathNode> nodes;
    private final BlockPos target;
    private int currentIndex;

    public CustomPath(List<PathNode> nodes, BlockPos target) {
        this.nodes = new ArrayList<>(nodes);
        this.target = target;
        this.currentIndex = 0;
    }

    @Nullable
    public PathNode getCurrentNode() {
        if (currentIndex >= nodes.size()) return null;
        return nodes.get(currentIndex);
    }

    @Nullable
    public PathNode getNextNode() {
        if (currentIndex + 1 >= nodes.size()) return null;
        return nodes.get(++currentIndex);
    }

    @Nullable
    public PathNode peekNextNode() {
        if (currentIndex + 1 >= nodes.size()) return null;
        return nodes.get(currentIndex + 1);
    }

    public boolean isDone() {
        return currentIndex >= nodes.size();
    }

    public int getLength() {
        return nodes.size();
    }

    public int getRemainingLength() {
        return Math.max(0, nodes.size() - currentIndex);
    }

    public BlockPos getTarget() {
        return target;
    }

    public List<PathNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void reset() {
        currentIndex = 0;
    }

    public void advance() {
        if (currentIndex < nodes.size()) {
            currentIndex++;
        }
    }

    @Nullable
    public Vec3 getCurrentTargetPos() {
        PathNode node = getCurrentNode();
        if (node == null) return null;
        return new Vec3(node.x + 0.5, node.y, node.z + 0.5);
    }

    public boolean isNearNode(Vec3 pos, double threshold) {
        PathNode node = getCurrentNode();
        if (node == null) return false;
        double dx = pos.x - (node.x + 0.5);
        double dy = pos.y - node.y;
        double dz = pos.z - (node.z + 0.5);
        return (dx * dx + dz * dz) < threshold * threshold && Math.abs(dy) < 1.5;
    }
}
