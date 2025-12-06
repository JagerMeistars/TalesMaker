package dcs.jagermeistars.talesmaker.pathfinding.path;

import dcs.jagermeistars.talesmaker.pathfinding.goals.Goal;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A partial/cutoff path that doesn't reach the goal.
 * Used when pathfinding times out or hits iteration limit.
 */
public class CutoffPath implements IPath {
    private final List<BlockPos> positions;
    private final List<Movement> movements;
    private final Goal goal;
    private final double totalCost;

    public CutoffPath(List<BlockPos> positions, List<Movement> movements, Goal goal, double totalCost) {
        this.positions = Collections.unmodifiableList(new ArrayList<>(positions));
        this.movements = movements != null ? Collections.unmodifiableList(new ArrayList<>(movements)) : Collections.emptyList();
        this.goal = goal;
        this.totalCost = totalCost;
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

    /**
     * CutoffPath is always incomplete - it doesn't reach the goal.
     */
    @Override
    public boolean isComplete() {
        return false;
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

        double cutCost = totalCost * ((double) index / positions.size());

        return new CutoffPath(cutPositions, cutMovements, goal, cutCost);
    }

    /**
     * Create a new cutoff path with movements populated.
     */
    public CutoffPath withMovements(List<Movement> newMovements) {
        return new CutoffPath(positions, newMovements, goal, totalCost);
    }

    @Override
    public String toString() {
        return "CutoffPath{" +
                "length=" + positions.size() +
                ", cost=" + String.format("%.2f", totalCost) +
                ", from=" + (getSrc() != null ? getSrc().toShortString() : "null") +
                ", to=" + (getDest() != null ? getDest().toShortString() : "null") +
                " (partial)}";
    }
}
