package dcs.jagermeistars.talesmaker.pathfinding.movement;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class for movement execution.
 * Manages direct entity movement without relying on vanilla AI.
 */
public final class MovementHelper {

    private MovementHelper() {
        // Utility class
    }

    /**
     * Move entity towards target position using direct velocity.
     *
     * @param entity the mob to move
     * @param target target position
     */
    public static void moveTowards(Mob entity, Vec3 target) {
        moveTowards(entity, target, 1.0);
    }

    /**
     * Move entity towards target position with speed modifier.
     * Uses direct velocity application instead of MoveControl for NPCs with AI disabled.
     *
     * @param entity        the mob to move
     * @param target        target position
     * @param speedModifier multiplier for base speed (1.0 = normal)
     */
    public static void moveTowards(Mob entity, Vec3 target, double speedModifier) {
        Vec3 currentPos = entity.position();
        double dx = target.x - currentPos.x;
        double dz = target.z - currentPos.z;
        double distSq = dx * dx + dz * dz;

        if (distSq < 0.01) {
            // Already at target
            return;
        }

        double dist = Math.sqrt(distSq);
        double speed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * speedModifier;

        // Normalize and apply speed
        double nx = dx / dist;
        double nz = dz / dist;

        // Apply movement directly via velocity
        Vec3 currentVel = entity.getDeltaMovement();
        double newVelX = nx * speed;
        double newVelZ = nz * speed;

        // Keep Y velocity for gravity
        entity.setDeltaMovement(newVelX, currentVel.y, newVelZ);
        entity.hasImpulse = true;

        // Look towards movement direction
        float yaw = (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.setYHeadRot(yaw);
    }

    /**
     * Make entity jump.
     * Uses direct velocity application.
     *
     * @param entity the mob to make jump
     */
    public static void jump(Mob entity) {
        if (entity.onGround()) {
            // Apply jump velocity directly (0.42 is vanilla jump velocity)
            Vec3 currentVel = entity.getDeltaMovement();
            entity.setDeltaMovement(currentVel.x, 0.42, currentVel.z);
            entity.hasImpulse = true;
        }
    }

    /**
     * Make entity jump with horizontal momentum towards target.
     * Uses direct velocity application.
     *
     * @param entity the mob to make jump
     * @param target target position
     */
    public static void jumpTowards(Mob entity, Vec3 target) {
        if (entity.onGround()) {
            Vec3 currentPos = entity.position();
            double dx = target.x - currentPos.x;
            double dz = target.z - currentPos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 0.01) {
                double speed = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double nx = dx / dist;
                double nz = dz / dist;

                // Jump with horizontal momentum (0.42 is vanilla jump velocity)
                entity.setDeltaMovement(nx * speed * 1.5, 0.42, nz * speed * 1.5);
                entity.hasImpulse = true;

                // Look towards target
                float yaw = (float) (Math.atan2(-dx, dz) * (180.0 / Math.PI));
                entity.setYRot(yaw);
                entity.yBodyRot = yaw;
                entity.setYHeadRot(yaw);
            }
        }
    }

    /**
     * Ensure AI is enabled for movement.
     *
     * @param entity the mob
     */
    public static void enableMovement(Mob entity) {
        if (entity.isNoAi()) {
            entity.setNoAi(false);
        }
    }

    /**
     * Disable AI after movement is complete.
     *
     * @param entity the mob
     */
    public static void disableMovement(Mob entity) {
        entity.setNoAi(true);
        // Stop any remaining movement
        entity.setDeltaMovement(Vec3.ZERO);
        entity.getMoveControl().setWantedPosition(entity.getX(), entity.getY(), entity.getZ(), 0);
    }

    /**
     * Get the movement speed of an entity.
     *
     * @param entity the mob
     * @return movement speed
     */
    public static double getSpeed(Mob entity) {
        return entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /**
     * Check if entity has reached target position (XZ only).
     *
     * @param entity         the mob
     * @param target         target position
     * @param thresholdSq    squared distance threshold
     * @return true if within threshold
     */
    public static boolean hasReachedXZ(Mob entity, Vec3 target, double thresholdSq) {
        Vec3 pos = entity.position();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        return (dx * dx + dz * dz) < thresholdSq;
    }

    /**
     * Check if entity has reached target position (XYZ).
     *
     * @param entity         the mob
     * @param target         target position
     * @param thresholdSq    squared distance threshold
     * @return true if within threshold
     */
    public static boolean hasReached(Mob entity, Vec3 target, double thresholdSq) {
        return entity.position().distanceToSqr(target) < thresholdSq;
    }

    /**
     * Check if there is enough vertical clearance for the entity at the given position.
     * Takes into account partial blocks like slabs and entity width.
     *
     * @param ctx    movement context
     * @param pos    base position to check
     * @param height entity height in blocks (can be fractional, e.g. 1.8)
     * @return true if enough clearance
     */
    public static boolean hasVerticalClearance(MovementContext ctx, net.minecraft.core.BlockPos pos, float height) {
        float width = ctx.getConfig().getEntityWidth();

        // For wide entities, check multiple positions
        if (width > 1.0f) {
            int radius = (int) Math.ceil((width - 1.0f) / 2.0f);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!hasVerticalClearanceAt(ctx, pos.offset(dx, 0, dz), height)) {
                        return false;
                    }
                }
            }
            return true;
        }

        return hasVerticalClearanceAt(ctx, pos, height);
    }

    /**
     * Check vertical clearance at a single position.
     */
    private static boolean hasVerticalClearanceAt(MovementContext ctx, net.minecraft.core.BlockPos pos, float height) {
        int fullBlocks = (int) height;
        float remainder = height - fullBlocks;

        // Check full blocks
        for (int i = 0; i < fullBlocks; i++) {
            net.minecraft.world.level.block.state.BlockState state = ctx.getBlockState(pos.above(i));
            if (state.blocksMotion()) {
                return false;
            }
        }

        // Check partial block at top if needed
        if (remainder > 0.01f) {
            net.minecraft.world.level.block.state.BlockState topState = ctx.getBlockState(pos.above(fullBlocks));
            if (topState.blocksMotion()) {
                // Check if it's a top slab (which leaves bottom half free)
                double minY = getCollisionMinY(topState);
                if (remainder > minY) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get the minimum Y coordinate of a block's collision shape.
     */
    private static double getCollisionMinY(net.minecraft.world.level.block.state.BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            net.minecraft.world.level.block.state.properties.SlabType type =
                state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);
            if (type == net.minecraft.world.level.block.state.properties.SlabType.TOP) {
                return 0.5;
            }
        }
        return 0.0;
    }
}
