package dcs.jagermeistars.talesmaker.pathfinding.movement.movements;

import dcs.jagermeistars.talesmaker.pathfinding.calc.ActionCosts;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementHelper;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementResult;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Swimming movement through water.
 */
public class MovementSwim extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.36;
    private static final int MAX_TICKS = 80;

    public MovementSwim(BlockPos src, BlockPos dest) {
        super(src, dest);
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.SWIM;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        // Either src or dest should be in water
        boolean srcWater = ctx.getBlockState(src).getFluidState().is(FluidTags.WATER);
        boolean destWater = ctx.getBlockState(dest).getFluidState().is(FluidTags.WATER);

        return srcWater || destWater;
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
                Vec3 targetPos = Vec3.atCenterOf(dest);

                // Check if we've reached the destination
                if (MovementHelper.hasReached(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ)) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Swim towards destination
                swim(ctx, targetPos);
                return MovementResult.IN_PROGRESS;

            case COMPLETE:
                return MovementResult.SUCCESS;

            case FAILED:
                return MovementResult.FAILED;

            default:
                return MovementResult.IN_PROGRESS;
        }
    }

    private void swim(MovementContext ctx, Vec3 target) {
        Mob entity = ctx.getEntity();
        Vec3 currentPos = entity.position();

        // Calculate direction
        double dx = target.x - currentPos.x;
        double dy = target.y - currentPos.y;
        double dz = target.z - currentPos.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.01) {
            // Get swim speed (slower than walk)
            double speed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.5;

            // Normalize and apply
            double nx = dx / dist;
            double ny = dy / dist;
            double nz = dz / dist;

            // Apply velocity directly
            entity.setDeltaMovement(nx * speed, ny * speed, nz * speed);

            // Look towards movement direction
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
            float pitch = (float) (Math.atan2(-dy, horizontalDist) * (180.0 / Math.PI));

            entity.setYRot(yaw);
            entity.setXRot(pitch);
            entity.yBodyRot = yaw;
        }
    }
}
