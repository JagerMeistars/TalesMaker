package dcs.jagermeistars.talesmaker.pathfinding.movement.movements;

import dcs.jagermeistars.talesmaker.pathfinding.calc.ActionCosts;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementHelper;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementResult;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Diagonal movement to an adjacent block (moving in both X and Z).
 */
public class MovementDiagonal extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.25;

    public MovementDiagonal(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.DIAGONAL;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Check destination has ground
        if (!ctx.getBlockState(dest.below()).isSolid()) {
            return false;
        }

        // Check vertical clearance for entity height at destination
        float height = ctx.getConfig().getEntityHeight();
        if (!MovementHelper.hasVerticalClearance(ctx, dest, height)) {
            return false;
        }

        // Check both adjacent cardinal directions are passable (no corner cutting)
        int dx = dest.getX() - src.getX();
        int dz = dest.getZ() - src.getZ();

        BlockPos side1 = src.offset(dx, 0, 0);
        BlockPos side2 = src.offset(0, 0, dz);

        // Check vertical clearance for both sides
        return MovementHelper.hasVerticalClearance(ctx, side1, height) &&
               MovementHelper.hasVerticalClearance(ctx, side2, height);
    }

    @Override
    public MovementResult tick(MovementContext ctx) {
        incrementTicks();

        switch (state) {
            case PREPPING:
                if (!canExecute(ctx)) {
                    setState(MovementState.FAILED);
                    return MovementResult.FAILED;
                }
                setState(MovementState.RUNNING);
                return MovementResult.IN_PROGRESS;

            case RUNNING:
                Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);

                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ)) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                MovementHelper.moveTowards(ctx.getEntity(), targetPos);
                return MovementResult.IN_PROGRESS;

            case COMPLETE:
                return MovementResult.SUCCESS;

            case FAILED:
                return MovementResult.FAILED;

            default:
                return MovementResult.IN_PROGRESS;
        }
    }
}
