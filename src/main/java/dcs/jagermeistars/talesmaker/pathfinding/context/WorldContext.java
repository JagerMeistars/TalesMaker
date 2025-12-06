package dcs.jagermeistars.talesmaker.pathfinding.context;

import dcs.jagermeistars.talesmaker.pathfinding.config.PathingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thread-safe world context for async pathfinding.
 * Captures a region of the world before pathfinding starts.
 */
public class WorldContext {
    private final BlockStateCache cache;
    private final PathingConfig config;

    public WorldContext(PathingConfig config) {
        this.cache = new BlockStateCache();
        this.config = config;
    }

    /**
     * Capture a cubic region of the world into the cache.
     * MUST be called from the main thread before async pathfinding.
     *
     * @param level  the world
     * @param center center of the region
     * @param radius radius in blocks
     */
    public void captureRegion(Level level, BlockPos center, int radius) {
        cache.clear();
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + radius);
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    cache.put(x, y, z, level.getBlockState(pos));
                }
            }
        }
    }

    /**
     * Capture a region between two points.
     */
    public void captureRegion(Level level, BlockPos from, BlockPos to, int padding) {
        cache.clear();
        int minX = Math.min(from.getX(), to.getX()) - padding;
        int maxX = Math.max(from.getX(), to.getX()) + padding;
        int minY = Math.max(level.getMinBuildHeight(), Math.min(from.getY(), to.getY()) - padding);
        int maxY = Math.min(level.getMaxBuildHeight(), Math.max(from.getY(), to.getY()) + padding);
        int minZ = Math.min(from.getZ(), to.getZ()) - padding;
        int maxZ = Math.max(from.getZ(), to.getZ()) + padding;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cache.put(x, y, z, level.getBlockState(new BlockPos(x, y, z)));
                }
            }
        }
    }

    /**
     * Get block state from cache.
     * Returns STONE for uncached blocks (safe default - treated as impassable).
     */
    public BlockState getBlockState(BlockPos pos) {
        return cache.getOrDefault(pos, Blocks.STONE.defaultBlockState());
    }

    /**
     * Get block state from cache using coordinates.
     */
    public BlockState getBlockState(int x, int y, int z) {
        BlockState state = cache.get(x, y, z);
        return state != null ? state : Blocks.STONE.defaultBlockState();
    }

    /**
     * Check if position is in the cached region.
     */
    public boolean isCached(int x, int y, int z) {
        return cache.contains(x, y, z);
    }

    /**
     * Check if a position is passable (entity can occupy this block).
     */
    public boolean isPassable(BlockPos pos) {
        return isPassable(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if a position is passable.
     */
    public boolean isPassable(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);

        // Air is always passable
        if (state.isAir()) {
            return true;
        }

        // Check for fluids - water is passable if swimming allowed
        if (!state.getFluidState().isEmpty()) {
            return config.canSwim() || state.getFluidState().is(FluidTags.WATER);
        }

        // Check collision shape
        // Note: We can't use level here, so we approximate
        if (state.getBlock() instanceof DoorBlock) {
            return config.canOpenDoors() || state.getValue(DoorBlock.OPEN);
        }

        if (state.getBlock() instanceof TrapDoorBlock) {
            return config.canOpenDoors() || state.getValue(TrapDoorBlock.OPEN);
        }

        // Climbable blocks are passable
        if (state.is(BlockTags.CLIMBABLE)) {
            return true;
        }

        // Use getCollisionShape to check if block has collision
        // Empty collision shape = passable
        // Note: Using VoxelShapes.empty() check since we don't have level context
        return !state.blocksMotion();
    }

    /**
     * Check if a position is solid (can stand on).
     */
    public boolean isSolid(BlockPos pos) {
        return isSolid(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if a position is solid.
     */
    public boolean isSolid(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        return state.isSolid();
    }

    /**
     * Check if there's solid ground below the position.
     */
    public boolean hasSolidGround(int x, int y, int z) {
        return isSolid(x, y - 1, z);
    }

    /**
     * Check if a position contains water.
     */
    public boolean isWater(BlockPos pos) {
        return isWater(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if a position contains water.
     */
    public boolean isWater(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        return state.getFluidState().is(FluidTags.WATER);
    }

    /**
     * Check if a position contains lava.
     */
    public boolean isLava(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        return state.getFluidState().is(FluidTags.LAVA);
    }

    /**
     * Check if a position is climbable (ladder, vine, etc.).
     */
    public boolean isClimbable(BlockPos pos) {
        return isClimbable(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Check if a position is climbable.
     */
    public boolean isClimbable(int x, int y, int z) {
        if (!config.canClimb()) {
            return false;
        }
        BlockState state = getBlockState(x, y, z);
        return state.is(BlockTags.CLIMBABLE);
    }

    /**
     * Check if a position is a door.
     */
    public boolean isDoor(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        return state.getBlock() instanceof DoorBlock;
    }

    /**
     * Check if a door at position is open.
     */
    public boolean isDoorOpen(int x, int y, int z) {
        BlockState state = getBlockState(x, y, z);
        if (state.getBlock() instanceof DoorBlock) {
            return state.getValue(DoorBlock.OPEN);
        }
        return false;
    }

    /**
     * Check if there's enough headroom at a position.
     * Takes into account partial blocks like slabs.
     *
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     * @param requiredHeight height in blocks needed (can be fractional, e.g. 1.8)
     * @return true if enough headroom
     */
    public boolean hasHeadroom(int x, int y, int z, float requiredHeight) {
        int fullBlocks = (int) requiredHeight;
        float remainder = requiredHeight - fullBlocks;

        for (int dy = 0; dy < fullBlocks; dy++) {
            if (!isPassable(x, y + dy, z)) {
                return false;
            }
        }

        // Check partial block at top if needed
        if (remainder > 0.01f) {
            BlockState topState = getBlockState(x, y + fullBlocks, z);
            // If the top block is not passable, check its collision shape
            if (!topState.isAir() && topState.blocksMotion()) {
                // Get the minimum Y of the collision shape
                // For top slabs, this would be 0.5, for bottom slabs it's 0
                double minY = getCollisionMinY(topState);
                // If our remainder height extends into the solid part, we can't fit
                if (remainder > minY) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get the minimum Y coordinate of a block's collision shape.
     * Returns 0 for full blocks, 0.5 for top slabs, etc.
     */
    private double getCollisionMinY(BlockState state) {
        // For slabs, check if it's a top slab
        if (state.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            net.minecraft.world.level.block.state.properties.SlabType type =
                state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE);
            if (type == net.minecraft.world.level.block.state.properties.SlabType.TOP) {
                return 0.5; // Top slab starts at Y=0.5
            } else if (type == net.minecraft.world.level.block.state.properties.SlabType.BOTTOM) {
                return 0.0; // Bottom slab starts at Y=0
            }
            // DOUBLE type is full block
        }
        // Default: full block starts at 0
        return 0.0;
    }

    /**
     * Legacy method for integer height.
     */
    public boolean hasHeadroom(int x, int y, int z, int requiredHeight) {
        return hasHeadroom(x, y, z, (float) requiredHeight);
    }

    /**
     * Check if entity can stand at position (passable + solid ground below).
     * Takes into account both entity height and width.
     */
    public boolean canStandAt(int x, int y, int z) {
        float height = config.getEntityHeight();
        float width = config.getEntityWidth();

        // For entities wider than 1 block, check multiple positions
        if (width > 1.0f) {
            return hasFootprint(x, y, z, width, height);
        }

        // Standard check for normal-sized entities
        return hasHeadroom(x, y, z, height) && hasSolidGround(x, y, z);
    }

    /**
     * Check if a wide entity can fit at position.
     * Checks a footprint based on entity width.
     *
     * @param x center X coordinate
     * @param y Y coordinate
     * @param z center Z coordinate
     * @param width entity width
     * @param height entity height
     * @return true if entity can fit
     */
    public boolean hasFootprint(int x, int y, int z, float width, float height) {
        // Entity is centered on block (x+0.5, z+0.5). We need to check blocks the entity overlaps.
        // Calculate half-width from center
        float halfWidth = width / 2.0f;

        // Entity centered at (x+0.5, z+0.5) extends from (x+0.5-halfWidth) to (x+0.5+halfWidth)
        // Convert to block coordinates the entity occupies:
        // - minBlock: the leftmost/backmost block the entity touches
        // - maxBlock: the rightmost/frontmost block the entity touches
        //
        // For width 1.5: center at 0.5, extends from -0.25 to 1.25
        //   -> occupies blocks 0 and 1 (2 blocks)
        // For width 1.8: center at 0.5, extends from -0.4 to 1.4
        //   -> occupies blocks 0 and 1 (2 blocks)
        // For width 2.5: center at 0.5, extends from -0.75 to 1.75
        //   -> occupies blocks -1, 0, and 1 (3 blocks)

        double minEdge = 0.5 - halfWidth;  // e.g., -0.25 for width 1.5
        double maxEdge = 0.5 + halfWidth;  // e.g., 1.25 for width 1.5

        // Entity occupies a block if it overlaps significantly (more than just touching)
        // Use ceil for min and floor for max, with small epsilon to handle exact boundaries
        int minBlockX = (int) Math.ceil(minEdge - 0.001);   // ceil(-0.25) = 0 for width 1.5
        int maxBlockX = (int) Math.floor(maxEdge - 0.001);  // floor(1.249) = 1 for width 1.5
        int minBlockZ = minBlockX;
        int maxBlockZ = maxBlockX;

        // Check all blocks in the footprint for headroom
        for (int dx = minBlockX; dx <= maxBlockX; dx++) {
            for (int dz = minBlockZ; dz <= maxBlockZ; dz++) {
                if (!hasHeadroom(x + dx, y, z + dz, height)) {
                    return false;
                }
            }
        }

        // Check that there's at least some solid ground under the entity
        for (int dx = minBlockX; dx <= maxBlockX; dx++) {
            for (int dz = minBlockZ; dz <= maxBlockZ; dz++) {
                if (hasSolidGround(x + dx, y, z + dz)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find how far an entity would fall from a position.
     *
     * @return fall distance, or Integer.MAX_VALUE if too far/deadly
     */
    public int getFallDistance(int x, int y, int z) {
        int fallDistance = 0;
        int checkY = y - 1;

        while (fallDistance <= config.getMaxFallDistance() + 1) {
            if (isSolid(x, checkY, z)) {
                return fallDistance;
            }
            if (isWater(x, checkY, z)) {
                return fallDistance; // Water breaks fall
            }
            if (isLava(x, checkY, z)) {
                return Integer.MAX_VALUE; // Lava is deadly
            }
            fallDistance++;
            checkY--;
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Get the config.
     */
    public PathingConfig getConfig() {
        return config;
    }

    /**
     * Get the cache (for advanced usage).
     */
    public BlockStateCache getCache() {
        return cache;
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        cache.clear();
    }
}