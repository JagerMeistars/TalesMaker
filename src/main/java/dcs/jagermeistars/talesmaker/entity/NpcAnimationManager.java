package dcs.jagermeistars.talesmaker.entity;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.data.NpcAnimationConfig;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Server-side animation manager for NPC entities.
 * Handles animation selection logic based on NPC state.
 */
public class NpcAnimationManager {

    private final NpcEntity entity;

    // Run detection threshold (blocks/tick)
    private static final float DEFAULT_RUN_THRESHOLD = 0.15f;

    // Body turn detection
    private float previousBodyYaw = 0;
    private int turnCooldown = 0;
    private int currentTurnDirection = 0; // 1 = right, -1 = left, 0 = none
    private static final int TURN_COOLDOWN_TICKS = 10;

    public NpcAnimationManager(NpcEntity entity) {
        this.entity = entity;
    }

    // Track if death animation was started
    private boolean deathAnimStarted = false;

    /**
     * Called every tick on server side to update animation state.
     */
    public void tick() {
        if (entity.level().isClientSide()) {
            return;
        }

        NpcAnimationState state = entity.getAnimationState();
        NpcAnimationConfig config = getAnimationConfig();

        // Handle death animation - highest priority
        if (entity.isDeadOrDying() && !deathAnimStarted) {
            if (startOverride("death")) {
                deathAnimStarted = true;
            }
            // Even if override not configured, mark as started to prevent repeated attempts
            deathAnimStarted = true;
            return;
        }

        // Skip other updates if dead
        if (entity.isDeadOrDying()) {
            return;
        }

        // Update base animation based on movement
        updateBaseAnimation(state, config);

        // Handle body turn detection
        if (config != null && config.bodyTurn() != null && config.bodyTurn().enabled()) {
            updateBodyTurn(state, config);
        }

        // Decrement turn cooldown
        if (turnCooldown > 0) {
            turnCooldown--;
        }
    }

    /**
     * Update base layer animation based on movement state.
     */
    private void updateBaseAnimation(NpcAnimationState state, @Nullable NpcAnimationConfig config) {
        // Skip if override is active
        if (state.getActiveLayer() == NpcAnimationState.LAYER_OVERRIDE) {
            return;
        }

        boolean isMoving = isEntityMoving();
        boolean isRunning = false;

        if (isMoving && config != null) {
            float runThreshold = config.getRunThreshold();
            if (runThreshold > 0) {
                float speed = getMovementSpeed();
                isRunning = speed >= runThreshold;
            }
        }

        state.updateBaseAnimation(isMoving, isRunning);
    }

    /**
     * Detect body turn and trigger turn animation.
     */
    private void updateBodyTurn(NpcAnimationState state, NpcAnimationConfig config) {
        // Skip if not idle or already turning
        if (isEntityMoving() || state.isBodyTurning() || turnCooldown > 0) {
            previousBodyYaw = entity.yBodyRot;
            return;
        }

        NpcAnimationConfig.BodyTurnConfig turnConfig = config.bodyTurn();
        float currentYaw = entity.yBodyRot;
        float yawDelta = currentYaw - previousBodyYaw;

        // Normalize to handle wrap-around
        if (yawDelta > 180) {
            yawDelta -= 360;
        } else if (yawDelta < -180) {
            yawDelta += 360;
        }

        if (Math.abs(yawDelta) >= turnConfig.threshold()) {
            // Trigger turn animation based on direction
            state.setBodyTurning(true);
            // Store turn direction: positive = right, negative = left
            currentTurnDirection = yawDelta > 0 ? 1 : -1;
            turnCooldown = TURN_COOLDOWN_TICKS;
        }

        previousBodyYaw = currentYaw;
    }

    /**
     * Called when body turn animation completes.
     */
    public void onBodyTurnComplete() {
        entity.getAnimationState().setBodyTurning(false);
        currentTurnDirection = 0;
    }

    /**
     * Get current turn direction.
     * @return 1 for right, -1 for left, 0 for no turn
     */
    public int getTurnDirection() {
        return currentTurnDirection;
    }

    /**
     * Get the turn animation name based on direction.
     */
    public String getTurnAnimationName() {
        NpcAnimationConfig config = getAnimationConfig();
        if (config == null || config.bodyTurn() == null) {
            return null;
        }

        String baseAnim = config.bodyTurn().animation();
        if (baseAnim == null || baseAnim.isEmpty()) {
            return null;
        }

        // Check if directional animations exist (turn_left, turn_right)
        // Otherwise use base animation name
        if (currentTurnDirection > 0) {
            return baseAnim + "_right";
        } else if (currentTurnDirection < 0) {
            return baseAnim + "_left";
        }
        return baseAnim;
    }

    /**
     * Check if entity is currently moving.
     */
    private boolean isEntityMoving() {
        // For noAi entities, check movement state
        if (entity.isNoAi()) {
            String movementState = entity.getMovementState();
            return !"idle".equals(movementState) && !movementState.isEmpty();
        }

        // For AI entities, check velocity
        double dx = entity.getX() - entity.xOld;
        double dz = entity.getZ() - entity.zOld;
        return (dx * dx + dz * dz) > 0.0001;
    }

    /**
     * Get current movement speed in blocks/tick.
     */
    private float getMovementSpeed() {
        double dx = entity.getX() - entity.xOld;
        double dz = entity.getZ() - entity.zOld;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Get the animation config from preset.
     */
    @Nullable
    private NpcAnimationConfig getAnimationConfig() {
        ResourceLocation presetId = entity.getPresetResourceLocation();
        if (presetId == null) {
            return null;
        }

        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);
        if (preset == null) {
            return null;
        }

        return preset.animations();
    }

    // ===== Animation Trigger Methods (called from external code) =====

    /**
     * Start an action animation by ID.
     * @param actionId The action ID (e.g., "attack", "wave")
     * @return true if the action exists and was started
     */
    public boolean startAction(String actionId) {
        NpcAnimationConfig config = getAnimationConfig();
        if (config == null) {
            return false;
        }

        NpcAnimationConfig.ActionEntry action = config.getAction(actionId);
        if (action == null) {
            return false;
        }

        entity.getAnimationState().startAction(actionId);
        return true;
    }

    /**
     * Start an override animation by ID.
     * @param overrideId The override ID (e.g., "death", "stun")
     * @return true if the override exists and was started
     */
    public boolean startOverride(String overrideId) {
        NpcAnimationConfig config = getAnimationConfig();
        if (config == null) {
            return false;
        }

        NpcAnimationConfig.OverrideEntry override = config.getOverride(overrideId);
        if (override == null) {
            return false;
        }

        entity.getAnimationState().startOverride(overrideId, override.blockHead());
        return true;
    }

    /**
     * Stop current action/override and return to base animation.
     */
    public void stopLayeredAnimation() {
        entity.getAnimationState().stopLayeredAnimation();
    }

    /**
     * Play a custom animation by name (command-triggered).
     * @param animationName The raw animation name
     * @param mode The play mode: "once", "loop", "hold"
     */
    public void playCustomAnimation(String animationName, String mode) {
        entity.getAnimationState().startCustom(animationName, mode);
    }

    /**
     * Stop the current custom animation and return to base layer.
     */
    public void stopCustomAnimation() {
        entity.getAnimationState().stopLayeredAnimation();
    }

    // ===== Condition Management =====

    /**
     * Add a condition for animation variant selection.
     * @param condition The condition name (e.g., "injured", "angry")
     */
    public void addCondition(String condition) {
        entity.getAnimationState().addCondition(condition);
    }

    /**
     * Remove a condition.
     */
    public void removeCondition(String condition) {
        entity.getAnimationState().removeCondition(condition);
    }

    /**
     * Check if a condition is active.
     */
    public boolean hasCondition(String condition) {
        return entity.getAnimationState().hasCondition(condition);
    }

    /**
     * Clear all conditions.
     */
    public void clearConditions() {
        entity.getAnimationState().clearConditions();
    }

    // ===== Query Methods =====

    /**
     * Get the current animation name for the given layer state.
     * Used by client-side animation controller.
     */
    public String getCurrentAnimationName() {
        NpcAnimationState state = entity.getAnimationState();
        NpcAnimationConfig config = getAnimationConfig();

        byte activeLayer = state.getActiveLayer();

        // Check custom layer first (highest priority for command-triggered animations)
        if (activeLayer == NpcAnimationState.LAYER_CUSTOM) {
            String animData = state.getCurrentAnimId();
            // Format: "animName:mode"
            int colonIndex = animData.lastIndexOf(':');
            if (colonIndex > 0) {
                return animData.substring(0, colonIndex);
            }
            return animData;
        }

        if (config == null) {
            return "idle";
        }

        // Check override layer
        if (activeLayer == NpcAnimationState.LAYER_OVERRIDE) {
            String overrideId = state.getCurrentAnimId();
            NpcAnimationConfig.OverrideEntry override = config.getOverride(overrideId);
            if (override != null) {
                return override.name();
            }
        }

        // Check action layer
        if (activeLayer == NpcAnimationState.LAYER_ACTION) {
            String actionId = state.getCurrentAnimId();
            NpcAnimationConfig.ActionEntry action = config.getAction(actionId);
            if (action != null) {
                return action.name();
            }
        }

        // Check body turn animation
        if (state.isBodyTurning()) {
            String turnAnim = getTurnAnimationName();
            if (turnAnim != null) {
                return turnAnim;
            }
        }

        // Base layer
        byte baseType = state.getBaseAnimationType();
        var conditions = state.getConditions();

        return switch (baseType) {
            case NpcAnimationState.BASE_WALK -> config.getWalkAnimation(conditions);
            case NpcAnimationState.BASE_RUN -> config.getRunAnimation(conditions);
            default -> config.getIdleAnimation(conditions);
        };
    }

    /**
     * Get the animation mode for current animation.
     * @return "loop", "once", or "hold"
     */
    public String getCurrentAnimationMode() {
        NpcAnimationState state = entity.getAnimationState();
        NpcAnimationConfig config = getAnimationConfig();

        byte activeLayer = state.getActiveLayer();

        // Check custom layer first
        if (activeLayer == NpcAnimationState.LAYER_CUSTOM) {
            String animData = state.getCurrentAnimId();
            // Format: "animName:mode"
            int colonIndex = animData.lastIndexOf(':');
            if (colonIndex > 0 && colonIndex < animData.length() - 1) {
                return animData.substring(colonIndex + 1);
            }
            return "once"; // Default mode for custom animations
        }

        if (config == null) {
            return "loop";
        }

        if (activeLayer == NpcAnimationState.LAYER_OVERRIDE) {
            NpcAnimationConfig.OverrideEntry override = config.getOverride(state.getCurrentAnimId());
            if (override != null) {
                return override.mode();
            }
        }

        if (activeLayer == NpcAnimationState.LAYER_ACTION) {
            NpcAnimationConfig.ActionEntry action = config.getAction(state.getCurrentAnimId());
            if (action != null) {
                return action.mode();
            }
        }

        // Body turn plays once
        if (state.isBodyTurning()) {
            return "once";
        }

        // Base animations always loop
        return "loop";
    }

    /**
     * Get the default transition ticks from config.
     */
    public int getDefaultTransitionTicks() {
        NpcAnimationConfig config = getAnimationConfig();
        if (config == null || config.transitions() == null) {
            return 5; // Default
        }
        return config.transitions().defaultTransition();
    }

    /**
     * Check if head tracking should be blocked.
     */
    public boolean isHeadBlocked() {
        NpcAnimationState state = entity.getAnimationState();

        // Check animation state flag
        if (state.isHeadBlocked()) {
            return true;
        }

        // Also check config for current override
        if (state.getActiveLayer() == NpcAnimationState.LAYER_OVERRIDE) {
            NpcAnimationConfig config = getAnimationConfig();
            if (config != null) {
                return config.isHeadBlockedBy(state.getCurrentAnimId());
            }
        }

        return false;
    }
}
