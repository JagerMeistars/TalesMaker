package dcs.jagermeistars.talesmaker.pathfinding.movement;

import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Context for movement execution, providing access to entity and world.
 * Forward declaration - will be fully implemented in Phase 3.
 */
public class MovementContext {
    private final Mob entity;
    private final Level level;
    private final PathingConfig config;

    public MovementContext(Mob entity, PathingConfig config) {
        this.entity = entity;
        this.level = entity.level();
        this.config = config;
    }

    public MovementContext(Mob entity) {
        this(entity, PathingConfig.defaultNpc());
    }

    public Mob getEntity() {
        return entity;
    }

    public Level getLevel() {
        return level;
    }

    public PathingConfig getConfig() {
        return config;
    }

    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    public boolean isOnGround() {
        return entity.onGround();
    }

    public Vec3 getPosition() {
        return entity.position();
    }

    public BlockPos getBlockPosition() {
        return entity.blockPosition();
    }

    /**
     * Check if a position is passable (not blocking movement).
     */
    public boolean isPassable(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return !state.blocksMotion();
    }

    /**
     * Check if a block position is passable.
     */
    public boolean isPassable(BlockPos pos) {
        return !level.getBlockState(pos).blocksMotion();
    }
}