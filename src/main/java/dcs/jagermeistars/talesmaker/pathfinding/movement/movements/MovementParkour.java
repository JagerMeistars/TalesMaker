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
 * Parkour jump across a 1-2 block gap.
 */
public class MovementParkour extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.49; // Larger for parkour
    private static final int MAX_TICKS = 60;
    private static final double SPRINT_SPEED_MULT = 1.3;

    private final int gapDistance;
    private boolean hasJumped = false;

    public MovementParkour(BlockPos src, BlockPos dest) {
        super(src, dest);
        int dx = Math.abs(dest.getX() - src.getX());
        int dz = Math.abs(dest.getZ() - src.getZ());
        this.gapDistance = Math.max(dx, dz);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.parkourCost(gapDistance);
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Check destination is passable and has ground
        if (ctx.getBlockState(dest).isSolid()) {
            return false;
        }
        if (!ctx.getBlockState(dest.below()).isSolid()) {
            return false;
        }

        // Check headroom for jump arc (2 blocks above src)
        if (ctx.getBlockState(src.above()).isSolid() ||
            ctx.getBlockState(src.above(2)).isSolid()) {
            return false;
        }

        // Check headroom at destination
        if (ctx.getBlockState(dest.above()).isSolid()) {
            return false;
        }

        // Verify there's actually a gap
        int dx = dest.getX() - src.getX();
        int dz = dest.getZ() - src.getZ();

        if (dx != 0) {
            int midX = src.getX() + (dx > 0 ? 1 : -1);
            BlockPos gapPos = new BlockPos(midX, src.getY(), src.getZ());
            // Gap should be air (no ground)
            if (ctx.getBlockState(gapPos.below()).isSolid()) {
                return false; // Not actually a gap
            }
        }
        if (dz != 0) {
            int midZ = src.getZ() + (dz > 0 ? 1 : -1);
            BlockPos gapPos = new BlockPos(src.getX(), src.getY(), midZ);
            if (ctx.getBlockState(gapPos.below()).isSolid()) {
                return false;
            }
        }

        return gapDistance >= 1 && gapDistance <= 2;
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
                hasJumped = false;
                setState(MovementState.WAITING);
                return MovementResult.IN_PROGRESS;

            case WAITING:
                // Sprint towards edge and prepare to jump
                if (ctx.isOnGround()) {
                    Vec3 currentPos = ctx.getPosition();
                    Vec3 srcCenter = MovementHelper.calculateTargetPosition(ctx, src, src);

                    // Calculate edge position
                    double edgeX = srcCenter.x + (dest.getX() - src.getX()) * 0.45;
                    double edgeZ = srcCenter.z + (dest.getZ() - src.getZ()) * 0.45;

                    double distToEdgeSq = currentPos.distanceToSqr(edgeX, currentPos.y, edgeZ);

                    // Jump when near edge
                    if (distToEdgeSq < 0.15) {
                        Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);
                        MovementHelper.jumpTowards(ctx.getEntity(), targetPos);
                        hasJumped = true;
                        setState(MovementState.RUNNING);
                    } else {
                        // Sprint towards edge
                        MovementHelper.moveTowards(ctx.getEntity(), new Vec3(edgeX, currentPos.y, edgeZ), SPRINT_SPEED_MULT);
                    }
                }
                return MovementResult.IN_PROGRESS;

            case RUNNING:
                Vec3 targetPos = MovementHelper.calculateTargetPosition(ctx, src, dest);

                // Check if we've landed at destination
                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ) && ctx.isOnGround() && hasJumped) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Keep moving towards destination while in air
                MovementHelper.moveTowards(ctx.getEntity(), targetPos, SPRINT_SPEED_MULT);
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
