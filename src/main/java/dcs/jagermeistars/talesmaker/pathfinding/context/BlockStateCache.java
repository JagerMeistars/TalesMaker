package dcs.jagermeistars.talesmaker.pathfinding.context;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache for BlockState.
 * Used to provide safe world access during async pathfinding.
 * No size limit - cache is cleared before each pathfinding request.
 */
public class BlockStateCache {
    private final ConcurrentHashMap<Long, BlockState> cache;

    public BlockStateCache() {
        // Initial capacity hint, but no max size limit
        this.cache = new ConcurrentHashMap<>(16384);
    }

    /**
     * Store a block state in the cache.
     */
    public void put(BlockPos pos, BlockState state) {
        cache.put(pos.asLong(), state);
    }

    /**
     * Store a block state using raw coordinates.
     */
    public void put(int x, int y, int z, BlockState state) {
        cache.put(BlockPos.asLong(x, y, z), state);
    }

    /**
     * Get a block state from the cache.
     *
     * @param pos the position
     * @return the cached state, or null if not cached
     */
    public BlockState get(BlockPos pos) {
        return cache.get(pos.asLong());
    }

    /**
     * Get a block state using raw coordinates.
     */
    public BlockState get(int x, int y, int z) {
        return cache.get(BlockPos.asLong(x, y, z));
    }

    /**
     * Get a block state, returning default if not cached.
     */
    public BlockState getOrDefault(BlockPos pos, BlockState defaultState) {
        BlockState state = get(pos);
        return state != null ? state : defaultState;
    }

    /**
     * Get a block state, returning air if not cached.
     */
    public BlockState getOrAir(BlockPos pos) {
        return getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * Get a block state, returning air if not cached.
     */
    public BlockState getOrAir(int x, int y, int z) {
        BlockState state = get(x, y, z);
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    /**
     * Check if a position is cached.
     */
    public boolean contains(BlockPos pos) {
        return cache.containsKey(pos.asLong());
    }

    /**
     * Check if a position is cached.
     */
    public boolean contains(int x, int y, int z) {
        return cache.containsKey(BlockPos.asLong(x, y, z));
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get current cache size.
     */
    public int size() {
        return cache.size();
    }
}
