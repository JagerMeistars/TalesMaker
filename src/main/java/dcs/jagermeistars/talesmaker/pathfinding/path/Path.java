package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of IPath representing a calculated path.
 */
public class Path implements IPath {
    private final List<BlockPos> positions;
    private final List<Movement> movements;
    private final Goal goal;
    private final double totalCost;
    private final boolean complete;

    public Path(List<BlockPos> positions, List<Movement> movements, Goal goal, double totalCost, boolean complete) {
        this.positions = Collections.unmodifiableList(new ArrayList<>(positions));
        this.movements = movements != null ? Collections.unmodifiableList(new ArrayList<>(movements)) : Collections.emptyList();
        this.goal = goal;
        this.totalCost = totalCost;
        this.complete = complete;
    }

    @Override
    public List<BlockPos> positions() {
        return positions;
    }

    @Override
    public List<Movement> movements() {
        return movements;
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public int length() {
        return positions.size();
    }

    @Override
    public BlockPos getSrc() {
        return positions.isEmpty() ? null : positions.get(0);
    }

    @Override
    public BlockPos getDest() {
        return positions.isEmpty() ? null : positions.get(positions.size() - 1);
    }

    @Override
    public double getTotalCost() {
        return totalCost;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public IPath cutoffAtIndex(int index) {
        if (index <= 0) {
            return new CutoffPath(Collections.emptyList(), Collections.emptyList(), goal, 0);
        }
        if (index >= positions.size()) {
            return this;
        }

        List<BlockPos> cutPositions = positions.subList(0, index + 1);
        List<Movement> cutMovements = index < movements.size()
            ? movements.subList(0, index)
            : movements;

        // Estimate cost for partial path
        double cutCost = totalCost * ((double) index / positions.size());

        return new CutoffPath(cutPositions, cutMovements, goal, cutCost);
    }

    /**
     * Create a new path with movements populated.
     */
    public Path withMovements(List<Movement> newMovements) {
        return new Path(positions, newMovements, goal, totalCost, complete);
    }

    @Override
    public String toString() {
        return "Path{" +
                "length=" + positions.size() +
                ", cost=" + String.format("%.2f", totalCost) +
                ", complete=" + complete +
                ", from=" + (getSrc() != null ? getSrc().toShortString() : "null") +
                ", to=" + (getDest() != null ? getDest().toShortString() : "null") +
                '}';
    }
}
