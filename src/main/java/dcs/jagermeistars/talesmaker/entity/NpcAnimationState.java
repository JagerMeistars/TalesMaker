package dcs.jagermeistars.talesmaker.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight runtime animation state for NPC entities.
 * Manages current animation layer states and conditions.
 * Uses packed data for efficient network synchronization.
 *
 * Note: EntityDataAccessors are defined in NpcEntity (ANIM_PACKED_STATE, ANIM_CURRENT_ID, ANIM_START_TICK, ANIM_CONDITIONS)
 * to avoid index conflicts with SynchedEntityData.
 */
public class NpcAnimationState {

    // ===== Animation Layer Constants =====

    /** No active layer (use base layer) */
    public static final byte LAYER_BASE = 0;
    /** Action layer active (attack, interact, emotes) */
    public static final byte LAYER_ACTION = 1;
    /** Override layer active (death, stun - highest priority) */
    public static final byte LAYER_OVERRIDE = 2;
    /** Custom animation layer (command-triggered, arbitrary animation name) */
    public static final byte LAYER_CUSTOM = 3;

    // ===== Base Layer Animation Types =====

    public static final byte BASE_IDLE = 0;
    public static final byte BASE_WALK = 1;
    public static final byte BASE_RUN = 2;

    // ===== Instance Fields =====

    private final NpcEntity entity;

    // Cached values for quick access
    private byte cachedPackedState = 0;
    private String cachedAnimId = "";
    private int cachedStartTick = 0;

    public NpcAnimationState(NpcEntity entity) {
        this.entity = entity;
    }

    // ===== Packed State Helpers =====

    private byte getPackedState() {
        return entity.getEntityData().get(NpcEntity.ANIM_PACKED_STATE);
    }

    private void setPackedState(byte state) {
        entity.getEntityData().set(NpcEntity.ANIM_PACKED_STATE, state);
        this.cachedPackedState = state;
    }

    /**
     * Get current active layer (BASE, ACTION, or OVERRIDE).
     */
    public byte getActiveLayer() {
        return (byte) (getPackedState() & 0x03);
    }

    /**
     * Set active layer.
     */
    public void setActiveLayer(byte layer) {
        byte state = getPackedState();
        state = (byte) ((state & ~0x03) | (layer & 0x03));
        setPackedState(state);
    }

    /**
     * Get base animation type (IDLE, WALK, or RUN).
     */
    public byte getBaseAnimationType() {
        return (byte) ((getPackedState() >> 2) & 0x03);
    }

    /**
     * Set base animation type.
     */
    public void setBaseAnimationType(byte type) {
        byte state = getPackedState();
        state = (byte) ((state & ~0x0C) | ((type & 0x03) << 2));
        setPackedState(state);
    }

    /**
     * Check if head tracking is blocked by current animation.
     */
    public boolean isHeadBlocked() {
        return (getPackedState() & 0x10) != 0;
    }

    /**
     * Set head tracking blocked state.
     */
    public void setHeadBlocked(boolean blocked) {
        byte state = getPackedState();
        if (blocked) {
            state |= 0x10;
        } else {
            state &= ~0x10;
        }
        setPackedState(state);
    }

    /**
     * Check if body turn animation is active.
     */
    public boolean isBodyTurning() {
        return (getPackedState() & 0x20) != 0;
    }

    /**
     * Set body turn animation state.
     */
    public void setBodyTurning(boolean turning) {
        byte state = getPackedState();
        if (turning) {
            state |= 0x20;
        } else {
            state &= ~0x20;
        }
        setPackedState(state);
    }

    // ===== Animation ID Methods =====

    /**
     * Get current action/override animation ID.
     */
    public String getCurrentAnimId() {
        return entity.getEntityData().get(NpcEntity.ANIM_CURRENT_ID);
    }

    /**
     * Set current animation ID.
     */
    public void setCurrentAnimId(String animId) {
        entity.getEntityData().set(NpcEntity.ANIM_CURRENT_ID, animId != null ? animId : "");
        this.cachedAnimId = animId != null ? animId : "";
    }

    // ===== Animation Timing =====

    /**
     * Get animation start tick.
     */
    public int getAnimStartTick() {
        return entity.getEntityData().get(NpcEntity.ANIM_START_TICK);
    }

    /**
     * Set animation start tick to current tick.
     */
    public void markAnimStart() {
        int tick = entity.tickCount;
        entity.getEntityData().set(NpcEntity.ANIM_START_TICK, tick);
        this.cachedStartTick = tick;
    }

    /**
     * Get ticks since animation started.
     */
    public int getAnimElapsedTicks() {
        return entity.tickCount - getAnimStartTick();
    }

    // ===== Conditions System =====
    // Conditions are stored as comma-separated string in ANIM_CONDITIONS for network sync

    /**
     * Get raw conditions string from synced data.
     */
    private String getConditionsString() {
        return entity.getEntityData().get(NpcEntity.ANIM_CONDITIONS);
    }

    /**
     * Set raw conditions string to synced data.
     */
    private void setConditionsString(String conditionsStr) {
        entity.getEntityData().set(NpcEntity.ANIM_CONDITIONS, conditionsStr != null ? conditionsStr : "");
    }

    /**
     * Get current conditions set (for variant selection).
     */
    public Set<String> getConditions() {
        String str = getConditionsString();
        if (str == null || str.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(str.split(","))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Add a condition.
     */
    public void addCondition(String condition) {
        if (condition != null && !condition.isEmpty()) {
            Set<String> current = new HashSet<>(getConditions());
            if (current.add(condition)) {
                setConditionsString(String.join(",", current));
            }
        }
    }

    /**
     * Remove a condition.
     */
    public void removeCondition(String condition) {
        Set<String> current = new HashSet<>(getConditions());
        if (current.remove(condition)) {
            setConditionsString(String.join(",", current));
        }
    }

    /**
     * Check if condition is active.
     */
    public boolean hasCondition(String condition) {
        return getConditions().contains(condition);
    }

    /**
     * Clear all conditions.
     */
    public void clearConditions() {
        setConditionsString("");
    }

    // ===== High-Level Animation Control =====

    /**
     * Start an action animation.
     * @param actionId The action ID from config (e.g., "attack", "wave")
     */
    public void startAction(String actionId) {
        setActiveLayer(LAYER_ACTION);
        setCurrentAnimId(actionId);
        markAnimStart();
    }

    /**
     * Start an override animation.
     * @param overrideId The override ID from config (e.g., "death", "stun")
     * @param blockHead Whether to block head tracking
     */
    public void startOverride(String overrideId, boolean blockHead) {
        setActiveLayer(LAYER_OVERRIDE);
        setCurrentAnimId(overrideId);
        setHeadBlocked(blockHead);
        markAnimStart();
    }

    /**
     * Start a custom animation by name (command-triggered).
     * @param animationName The raw animation name
     * @param mode The play mode: "once", "loop", "hold"
     */
    public void startCustom(String animationName, String mode) {
        setActiveLayer(LAYER_CUSTOM);
        // Store animation name with mode suffix for parsing: "animName:mode"
        setCurrentAnimId(animationName + ":" + mode);
        markAnimStart();
    }

    /**
     * Stop current action/override and return to base layer.
     */
    public void stopLayeredAnimation() {
        setActiveLayer(LAYER_BASE);
        setCurrentAnimId("");
        setHeadBlocked(false);
    }

    /**
     * Update base movement animation based on entity velocity.
     * @param isMoving Whether the entity is moving
     * @param isRunning Whether the entity is running (above run threshold)
     */
    public void updateBaseAnimation(boolean isMoving, boolean isRunning) {
        if (!isMoving) {
            setBaseAnimationType(BASE_IDLE);
        } else if (isRunning) {
            setBaseAnimationType(BASE_RUN);
        } else {
            setBaseAnimationType(BASE_WALK);
        }
    }

    // ===== Debug =====

    @Override
    public String toString() {
        return String.format("NpcAnimationState[layer=%d, base=%d, animId=%s, headBlocked=%b, turning=%b]",
                getActiveLayer(), getBaseAnimationType(), getCurrentAnimId(), isHeadBlocked(), isBodyTurning());
    }
}
