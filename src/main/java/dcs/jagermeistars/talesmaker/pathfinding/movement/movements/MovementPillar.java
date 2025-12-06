package dcs.jagermeistars.talesmaker.pathfinding.movement.movements;

import dcs.jagermeistars.talesmaker.pathfinding.calc.ActionCosts;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementHelper;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementResult;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Climb up or down using ladders/vines.
 * Vertical movement only (same X, Z).
 */
public class MovementPillar extends Movement {

    private static final double REACH_THRESHOLD = 0.5;
    private static final int MAX_TICKS = 60;

    private final int direction; // 1 = up, -1 = down

    public MovementPillar(BlockPos src, BlockPos dest) {
        super(src, dest);
        this.direction = Integer.compare(dest.getY(), src.getY());
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.CLIMB * Math.abs(dest.getY() - src.getY());
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Check that src or path has climbable blocks
        int startY = Math.min(src.getY(), dest.getY());
        int endY = Math.max(src.getY(), dest.getY());

        boolean hasClimbable = false;
        for (int y = startY; y <= endY; y++) {
            BlockPos checkPos = new BlockPos(src.getX(), y, src.getZ());
            if (ctx.getBlockState(checkPos).is(BlockTags.CLIMBABLE)) {
                hasClimbable = true;
                break;
            }
        }

        if (!hasClimbable) {
            return false;
        }

        // Check destination is passable
        return !ctx.getBlockState(dest).isSolid();
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
                Vec3 currentPos = ctx.getPosition();
                double targetY = dest.getY() + 0.1;

                // Check if we've reached the destination
                if (Math.abs(currentPos.y - targetY) < REACH_THRESHOLD) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Climb towards destination
                climb(ctx);
                return MovementResult.IN_PROGRESS;

            case COMPLETE:
                return MovementResult.SUCCESS;

            case FAILED:
                return MovementResult.FAILED;

            default:
                return MovementResult.IN_PROGRESS;
        }
    }

    private void climb(MovementContext ctx) {
        var entity = ctx.getEntity();
        Vec3 currentPos = entity.position();

        // Set target slightly towards the ladder/vine
        double targetX = src.getX() + 0.5;
        double targetZ = src.getZ() + 0.5;
        double targetY = dest.getY() + 0.5;

        // Get climb speed
        double speed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.8;

        // Calculate vertical velocity for climbing
        double dy = targetY - currentPos.y;
        double verticalSpeed = Math.signum(dy) * speed;

        // Apply velocity directly (for climbing)
        Vec3 currentVel = entity.getDeltaMovement();
        entity.setDeltaMovement(currentVel.x * 0.5, verticalSpeed, currentVel.z * 0.5);

        // Look in direction of movement
        if (direction > 0) {
            entity.setXRot(-45); // Look up when climbing
        } else {
            entity.setXRot(45); // Look down when descending
        }
    }
}
