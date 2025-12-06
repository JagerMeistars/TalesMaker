package dcs.jagermeistars.talesmaker.pathfinding.movement.movements;

import dcs.jagermeistars.talesmaker.pathfinding.calc.ActionCosts;
import dcs.jagermeistars.talesmaker.pathfinding.movement.Movement;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementContext;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementHelper;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementResult;
import dcs.jagermeistars.talesmaker.pathfinding.movement.MovementState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Movement through a door (open if needed, walk through).
 */
public class MovementDoor extends Movement {

    private static final double REACH_THRESHOLD_SQ = 0.25;
    private static final int MAX_TICKS = 60;
    private static final double DOOR_INTERACT_DISTANCE_SQ = 4.0; // 2 blocks

    private final BlockPos doorPos;
    private boolean doorOpened = false;

    public MovementDoor(BlockPos src, BlockPos dest, BlockPos doorPos) {
        super(src, dest);
        this.doorPos = doorPos;
    }

    public MovementDoor(BlockPos src, BlockPos dest) {
        super(src, dest);
        // Assume door is between src and dest
        this.doorPos = new BlockPos(
                (src.getX() + dest.getX()) / 2,
                src.getY(),
                (src.getZ() + dest.getZ()) / 2
        );
    }

    @Override
    public double calculateCost(MovementContext ctx) {
        return ActionCosts.DOOR;
    }

    @Override
    public boolean canExecute(MovementContext ctx) {
        BlockState doorState = ctx.getBlockState(doorPos);

        // Check if it's actually a door
        if (!(doorState.getBlock() instanceof DoorBlock)) {
            return false;
        }

        // Check destination is passable (after door opens)
        BlockState destState = ctx.getBlockState(dest);
        if (destState.isSolid() && !(destState.getBlock() instanceof DoorBlock)) {
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
                setState(MovementState.WAITING);
                return MovementResult.IN_PROGRESS;

            case WAITING:
                // Move towards door and interact to open
                Vec3 currentPos = ctx.getPosition();
                Vec3 doorCenter = Vec3.atCenterOf(doorPos);

                double distToDoorSq = currentPos.distanceToSqr(doorCenter);

                // Check if door is already open
                BlockState doorState = ctx.getBlockState(doorPos);
                if (doorState.getBlock() instanceof DoorBlock door) {
                    boolean isOpen = doorState.getValue(DoorBlock.OPEN);

                    if (isOpen) {
                        doorOpened = true;
                        setState(MovementState.RUNNING);
                        return MovementResult.IN_PROGRESS;
                    }

                    // Open the door when close enough
                    if (distToDoorSq < DOOR_INTERACT_DISTANCE_SQ) {
                        // Interact with door
                        var entity = ctx.getEntity();
                        var level = ctx.getLevel();

                        // Toggle door state
                        door.setOpen(entity, level, doorState, doorPos, true);
                        doorOpened = true;
                        setState(MovementState.RUNNING);
                        return MovementResult.IN_PROGRESS;
                    }
                }

                // Move towards door
                MovementHelper.moveTowards(ctx.getEntity(), doorCenter);
                return MovementResult.IN_PROGRESS;

            case RUNNING:
                Vec3 targetPos = Vec3.atBottomCenterOf(dest);

                // Check if we've reached the destination
                if (MovementHelper.hasReachedXZ(ctx.getEntity(), targetPos, REACH_THRESHOLD_SQ)) {
                    setState(MovementState.COMPLETE);
                    return MovementResult.SUCCESS;
                }

                // Walk through the door
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
