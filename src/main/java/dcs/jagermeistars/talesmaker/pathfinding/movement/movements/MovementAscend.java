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
 * Jump up one block while moving horizontally.
 * Destination is 1 block higher and 1 block horizontal from source.
 */
public class MovementAscend extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.36; // Slightly larger for jumps
    private static final int MAX_TICKS = 40;

    public MovementAscend(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.JUMP_UP;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        float height = ctx.getConfig().getEntityHeight();

        // Check we have headroom to jump (need height + 1 for jumping)
        int clearanceNeeded = (int) Math.ceil(height) + 1;
        for (int i = 1; i <= clearanceNeeded; i++) {
            if (ctx.getBlockState(src.above(i)).isSolid()) {
                return false;
            }
        }

        // Check destination has ground
        if (!ctx.getBlockState(dest.below()).isSolid()) {
            return false;
        }

        // Check vertical clearance at destination
        return MovementHelper.hasVerticalClearance(ctx, dest, height);
    }

    @Override
    public MovementResult tick(MovementContext ctx) {
        incrementTicks();

        // Timeout check
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
                setState(MovementState.WAITING);
                return MovementResult.IN_PROGRESS;

            case WAITING:
                // Wait until on ground and ready to jump
                if (ctx.isOnGround()) {
                    // Jump towards target
                    Vec3 targetPos = Vec3.atBottomCenterOf(dest);
                    MovementHelper.jumpTowards(ctx.getEntity(), targetPos);
                    setState(MovementState.RUNNING);
                }
                return MovementResult.IN_PROGRESS;

            case RUNNING:
                Vec3 currentPos = ctx.getPosition();
                Vec3 targetPos = Vec3.atBottomCenterOf(dest);

                // Check if we've reached the destination
                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ)
                        && ctx.isOnGround() && currentPos.y >= dest.getY() - 0.5) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Continue moving towards destination
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
