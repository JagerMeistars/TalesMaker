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
 * Basic horizontal walking movement to an adjacent block.
 */
public class MovementTraverse extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.25; // 0.5 blocks squared

    public MovementTraverse(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.WALK;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Check destination has ground
        BlockPos below = dest.below();
        if (!ctx.getBlockState(below).isSolid()) {
            return false;
        }

        // Check vertical clearance for entity height
        float height = ctx.getConfig().getEntityHeight();
        return MovementHelper.hasVerticalClearance(ctx, dest, height);
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
                Vec3 currentPos = ctx.getPosition();

                // Debug every 10 ticks
                if (getTicksInState() % 10 == 0) {
                    //System.out.println("[MovementTraverse] RUNNING: currentPos=" + currentPos + " targetPos=" + targetPos);
                }

                // Check if we've reached the destination
                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ)) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Move towards destination
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
