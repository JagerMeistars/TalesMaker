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
 * Step down one block while moving horizontally.
 * Destination is 1 block lower and 1 block horizontal from source.
 */
public class MovementDescend extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.25;
    private static final int MAX_TICKS = 40;

    public MovementDescend(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.STEP_DOWN;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Check destination is passable
        if (ctx.getBlockState(dest).isSolid()) {
            return false;
        }

        // Check destination has ground
        if (!ctx.getBlockState(dest.below()).isSolid()) {
            return false;
        }

        // Check headroom at destination
        if (ctx.getBlockState(dest.above()).isSolid()) {
            return false;
        }

        return true;
    }

    @Override
    public MovementResult tick(MovementContext ctx) {
        incrementTicks();

        if (getTicksInState() > MAX_TICKS) {
            setState(MovementState.FAILED);
            return MovementResult.FAILED;
        }

        switch (state) {
            case PREPPING:
                if (!canExecute(ctx)) {
                    setState(MovementState.FAILED);
                    return MovementResult.FAILED;
                }
                setState(MovementState.RUNNING);
                return MovementResult.IN_PROGRESS;

            case RUNNING:
                Vec3 targetPos = Vec3.atBottomCenterOf(dest);

                // Check if we've reached the destination
                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ) && ctx.isOnGround()) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Move towards destination (will naturally fall)
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
