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
 * Fall 2-3 blocks down (with optional horizontal movement).
 */
public class MovementFall extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.36;
    private static final int MAX_TICKS = 60;

    private final int fallDistance;

    public MovementFall(BlockPos src, BlockPos dest) {
        super(src, dest);
        this.fallDistance = src.getY() - dest.getY();
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.fallCost(fallDistance);
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

        // Check fall path is clear
        for (int y = src.getY() - 1; y >= dest.getY(); y--) {
            BlockPos checkPos = new BlockPos(dest.getX(), y, dest.getZ());
            if (ctx.getBlockState(checkPos).isSolid() && y != dest.getY() - 1) {
                return false; // Blocked, but allow the landing block
            }
        }

        return fallDistance >= 2 && fallDistance <= 3;
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

                // Move towards destination (gravity handles the fall)
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
