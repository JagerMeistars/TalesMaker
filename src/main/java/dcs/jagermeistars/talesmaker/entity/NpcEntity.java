package dcs.jagermeistars.talesmaker.entity;

import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.pathfinding.NpcPathingBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NpcEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> CUSTOM_ID = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PRESET_ID = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> EMISSIVE_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMATION_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> IDLE_ANIM_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> WALK_ANIM_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DEATH_ANIM_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> HEAD = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> HITBOX_WIDTH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HITBOX_HEIGHT = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEATH_DURATION = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.INT);

    // Script system fields
    private static final EntityDataAccessor<String> SCRIPT_TYPE = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SCRIPT_COMMAND = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> INTERACT_USED = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);

    // AI and invulnerability fields
    private static final EntityDataAccessor<Boolean> AI_ENABLED = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> INVULNERABLE = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);

    // Player nearby event fields
    private static final EntityDataAccessor<Float> PLAYER_NEARBY_RADIUS = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PLAYER_NEARBY_TRIGGERED = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);

    // Look-at system fields
    private static final EntityDataAccessor<Boolean> LOOK_AT_ACTIVE = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LOOK_AT_CONTINUOUS = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> LOOK_AT_TARGET_X = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOOK_AT_TARGET_Y = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOOK_AT_TARGET_Z = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LOOK_AT_ENTITY_ID = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.INT);

    // View range limit fields (for restricting look rotation)
    private static final EntityDataAccessor<Float> VIEW_RANGE_MIN_YAW = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIEW_RANGE_MAX_YAW = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIEW_RANGE_MIN_PITCH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIEW_RANGE_MAX_PITCH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);

    // Movement system fields
    private static final EntityDataAccessor<String> MOVEMENT_STATE = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING); // idle, goto, patrol, follow, wander, directional
    private static final EntityDataAccessor<Float> MOVEMENT_TARGET_X = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOVEMENT_TARGET_Y = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOVEMENT_TARGET_Z = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MOVEMENT_TARGET_ENTITY_ID = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> PATROL_POINTS = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING); // JSON array of points
    private static final EntityDataAccessor<Integer> PATROL_INDEX = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> WANDER_POLYGON = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING); // JSON array of polygon vertices
    private static final EntityDataAccessor<Float> WANDER_Y = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTIONAL_REMAINING = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTIONAL_VEC_X = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTIONAL_VEC_Z = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.FLOAT);

    // Custom animation fields
    private static final EntityDataAccessor<String> CUSTOM_ANIMATION = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CUSTOM_ANIMATION_MODE = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING); // once, loop, hold

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String lastPlayedOnceAnimation = ""; // Track which "once" animation was played to detect completion

    // Handlers
    private final NpcLookHandler lookHandler;
    private final NpcPathingBehavior pathingBehavior;

    public NpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.lookHandler = new NpcLookHandler(this);
        this.pathingBehavior = new NpcPathingBehavior(this);
        // AI is disabled by default
        this.setNoAi(true);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HITBOX_WIDTH.equals(key) || HITBOX_HEIGHT.equals(key)) {
            this.refreshDimensions();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CUSTOM_ID, "");
        builder.define(PRESET_ID, "");
        builder.define(NPC_NAME, "NPC");
        builder.define(MODEL_PATH, "");
        builder.define(TEXTURE_PATH, "");
        builder.define(EMISSIVE_PATH, "");
        builder.define(ANIMATION_PATH, "");
        builder.define(IDLE_ANIM_NAME, "idle");
        builder.define(WALK_ANIM_NAME, "walk");
        builder.define(DEATH_ANIM_NAME, "");
        builder.define(HEAD, "head");
        builder.define(HITBOX_WIDTH, 0.6f);
        builder.define(HITBOX_HEIGHT, 1.8f);
        builder.define(DEATH_DURATION, 40);
        // Script system
        builder.define(SCRIPT_TYPE, "");
        builder.define(SCRIPT_COMMAND, "");
        builder.define(INTERACT_USED, false);
        // AI and invulnerability (AI disabled by default)
        builder.define(AI_ENABLED, false);
        builder.define(INVULNERABLE, false);
        // Player nearby event
        builder.define(PLAYER_NEARBY_RADIUS, 0.0f);
        builder.define(PLAYER_NEARBY_TRIGGERED, false);
        // Look-at system
        builder.define(LOOK_AT_ACTIVE, false);
        builder.define(LOOK_AT_CONTINUOUS, false);
        builder.define(LOOK_AT_TARGET_X, 0.0f);
        builder.define(LOOK_AT_TARGET_Y, 0.0f);
        builder.define(LOOK_AT_TARGET_Z, 0.0f);
        builder.define(LOOK_AT_ENTITY_ID, -1);
        // View range limits (default: no limits = -180 to 180 for yaw, -90 to 90 for pitch)
        builder.define(VIEW_RANGE_MIN_YAW, -180.0f);
        builder.define(VIEW_RANGE_MAX_YAW, 180.0f);
        builder.define(VIEW_RANGE_MIN_PITCH, -90.0f);
        builder.define(VIEW_RANGE_MAX_PITCH, 90.0f);
        // Movement system
        builder.define(MOVEMENT_STATE, "idle");
        builder.define(MOVEMENT_TARGET_X, 0.0f);
        builder.define(MOVEMENT_TARGET_Y, 0.0f);
        builder.define(MOVEMENT_TARGET_Z, 0.0f);
        builder.define(MOVEMENT_TARGET_ENTITY_ID, -1);
        builder.define(PATROL_POINTS, "");
        builder.define(PATROL_INDEX, 0);
        builder.define(WANDER_POLYGON, "");
        builder.define(WANDER_Y, 0.0f);
        builder.define(DIRECTIONAL_REMAINING, 0.0f);
        builder.define(DIRECTIONAL_VEC_X, 0.0f);
        builder.define(DIRECTIONAL_VEC_Z, 0.0f);
        // Custom animation
        builder.define(CUSTOM_ANIMATION, "");
        builder.define(CUSTOM_ANIMATION_MODE, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        nav.setCanFloat(true);
        return nav;
    }

    public void setPreset(NpcPreset preset) {
        this.entityData.set(PRESET_ID, preset.id().toString());
        String nameJson = Component.Serializer.toJson(preset.name(), this.level().registryAccess());
        this.entityData.set(NPC_NAME, nameJson);
        this.entityData.set(MODEL_PATH, preset.model().toString());
        this.entityData.set(TEXTURE_PATH, preset.texture().toString());
        this.entityData.set(EMISSIVE_PATH, preset.emissive() != null ? preset.emissive().toString() : "");
        this.entityData.set(ANIMATION_PATH, preset.animations().path().toString());
        this.entityData.set(IDLE_ANIM_NAME, preset.animations().idle());
        this.entityData.set(WALK_ANIM_NAME, preset.animations().walk());
        this.entityData.set(DEATH_ANIM_NAME, preset.animations().death().name());
        this.entityData.set(DEATH_DURATION, preset.animations().death().durationTicks());
        this.entityData.set(HEAD, preset.head());
        this.entityData.set(HITBOX_WIDTH, preset.hitbox().width());
        this.entityData.set(HITBOX_HEIGHT, preset.hitbox().height());
        this.refreshDimensions();
        this.setCustomName(preset.name());
        this.setCustomNameVisible(false);
    }

    public void setCustomId(String customId) {
        this.entityData.set(CUSTOM_ID, customId != null ? customId : "");
    }

    public String getCustomId() {
        return this.entityData.get(CUSTOM_ID);
    }

    public String getPresetId() {
        return this.entityData.get(PRESET_ID);
    }

    public ResourceLocation getPresetResourceLocation() {
        String presetId = getPresetId();
        if (presetId.isEmpty()) return null;
        try {
            return ResourceLocation.parse(presetId);
        } catch (Exception e) {
            return null;
        }
    }

    public String getHead() {
        return this.entityData.get(HEAD);
    }

    public Component getNpcName() {
        String nameJson = this.entityData.get(NPC_NAME);
        try {
            return Component.Serializer.fromJson(nameJson, this.level().registryAccess());
        } catch (Exception e) {
            return Component.literal("NPC");
        }
    }

    public ResourceLocation getModelPath() {
        String path = this.entityData.get(MODEL_PATH);
        if (path.isEmpty()) return null;
        try {
            return ResourceLocation.parse(path);
        } catch (Exception e) {
            return null;
        }
    }

    public ResourceLocation getTexturePath() {
        String path = this.entityData.get(TEXTURE_PATH);
        if (path.isEmpty()) return null;
        try {
            return ResourceLocation.parse(path);
        } catch (Exception e) {
            return null;
        }
    }

    public ResourceLocation getEmissivePath() {
        String path = this.entityData.get(EMISSIVE_PATH);
        if (path.isEmpty()) return null;
        try {
            return ResourceLocation.parse(path);
        } catch (Exception e) {
            return null;
        }
    }

    public ResourceLocation getAnimationPath() {
        String path = this.entityData.get(ANIMATION_PATH);
        if (path.isEmpty()) return null;
        try {
            return ResourceLocation.parse(path);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("CustomId", getCustomId());
        compound.putString("PresetId", getPresetId());
        compound.putString("NpcName", this.entityData.get(NPC_NAME));
        compound.putString("ModelPath", this.entityData.get(MODEL_PATH));
        compound.putString("TexturePath", this.entityData.get(TEXTURE_PATH));
        compound.putString("EmissivePath", this.entityData.get(EMISSIVE_PATH));
        compound.putString("AnimationPath", this.entityData.get(ANIMATION_PATH));
        compound.putString("IdleAnim", this.entityData.get(IDLE_ANIM_NAME));
        compound.putString("WalkAnim", this.entityData.get(WALK_ANIM_NAME));
        compound.putString("DeathAnim", this.entityData.get(DEATH_ANIM_NAME));
        compound.putInt("DeathDuration", this.entityData.get(DEATH_DURATION));
        compound.putString("Head", this.entityData.get(HEAD));
        compound.putFloat("HitboxWidth", this.entityData.get(HITBOX_WIDTH));
        compound.putFloat("HitboxHeight", this.entityData.get(HITBOX_HEIGHT));
        // Script system
        compound.putString("ScriptType", this.entityData.get(SCRIPT_TYPE));
        compound.putString("ScriptCommand", this.entityData.get(SCRIPT_COMMAND));
        compound.putBoolean("InteractUsed", this.entityData.get(INTERACT_USED));
        // AI and invulnerability
        compound.putBoolean("AiEnabled", this.entityData.get(AI_ENABLED));
        compound.putBoolean("NpcInvulnerable", this.entityData.get(INVULNERABLE));
        // Player nearby
        compound.putFloat("PlayerNearbyRadius", this.entityData.get(PLAYER_NEARBY_RADIUS));
        compound.putBoolean("PlayerNearbyTriggered", this.entityData.get(PLAYER_NEARBY_TRIGGERED));
        // Look-at system
        compound.putBoolean("LookAtActive", this.entityData.get(LOOK_AT_ACTIVE));
        compound.putBoolean("LookAtContinuous", this.entityData.get(LOOK_AT_CONTINUOUS));
        compound.putFloat("LookAtTargetX", this.entityData.get(LOOK_AT_TARGET_X));
        compound.putFloat("LookAtTargetY", this.entityData.get(LOOK_AT_TARGET_Y));
        compound.putFloat("LookAtTargetZ", this.entityData.get(LOOK_AT_TARGET_Z));
        compound.putInt("LookAtEntityId", this.entityData.get(LOOK_AT_ENTITY_ID));
        // View range limits
        compound.putFloat("ViewRangeMinYaw", this.entityData.get(VIEW_RANGE_MIN_YAW));
        compound.putFloat("ViewRangeMaxYaw", this.entityData.get(VIEW_RANGE_MAX_YAW));
        compound.putFloat("ViewRangeMinPitch", this.entityData.get(VIEW_RANGE_MIN_PITCH));
        compound.putFloat("ViewRangeMaxPitch", this.entityData.get(VIEW_RANGE_MAX_PITCH));
        // Movement system
        compound.putString("MovementState", this.entityData.get(MOVEMENT_STATE));
        compound.putFloat("MovementTargetX", this.entityData.get(MOVEMENT_TARGET_X));
        compound.putFloat("MovementTargetY", this.entityData.get(MOVEMENT_TARGET_Y));
        compound.putFloat("MovementTargetZ", this.entityData.get(MOVEMENT_TARGET_Z));
        compound.putInt("MovementTargetEntityId", this.entityData.get(MOVEMENT_TARGET_ENTITY_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CustomId")) {
            this.entityData.set(CUSTOM_ID, compound.getString("CustomId"));
        }
        if (compound.contains("PresetId")) {
            this.entityData.set(PRESET_ID, compound.getString("PresetId"));
        }
        if (compound.contains("NpcName")) {
            String nameJson = compound.getString("NpcName");
            this.entityData.set(NPC_NAME, nameJson);
            try {
                this.setCustomName(Component.Serializer.fromJson(nameJson, this.level().registryAccess()));
                this.setCustomNameVisible(false);
            } catch (Exception e) {
                this.setCustomName(Component.literal("NPC"));
            }
        }
        if (compound.contains("ModelPath")) {
            this.entityData.set(MODEL_PATH, compound.getString("ModelPath"));
        }
        if (compound.contains("TexturePath")) {
            this.entityData.set(TEXTURE_PATH, compound.getString("TexturePath"));
        }
        if (compound.contains("EmissivePath")) {
            this.entityData.set(EMISSIVE_PATH, compound.getString("EmissivePath"));
        }
        if (compound.contains("AnimationPath")) {
            this.entityData.set(ANIMATION_PATH, compound.getString("AnimationPath"));
        }
        if (compound.contains("IdleAnim")) {
            this.entityData.set(IDLE_ANIM_NAME, compound.getString("IdleAnim"));
        }
        if (compound.contains("WalkAnim")) {
            this.entityData.set(WALK_ANIM_NAME, compound.getString("WalkAnim"));
        }
        if (compound.contains("DeathAnim")) {
            this.entityData.set(DEATH_ANIM_NAME, compound.getString("DeathAnim"));
        }
        if (compound.contains("DeathDuration")) {
            this.entityData.set(DEATH_DURATION, compound.getInt("DeathDuration"));
        }
        if (compound.contains("Head")) {
            this.entityData.set(HEAD, compound.getString("Head"));
        }
        if (compound.contains("HitboxWidth")) {
            this.entityData.set(HITBOX_WIDTH, compound.getFloat("HitboxWidth"));
        }
        if (compound.contains("HitboxHeight")) {
            this.entityData.set(HITBOX_HEIGHT, compound.getFloat("HitboxHeight"));
        }
        // Script system
        if (compound.contains("ScriptType")) {
            this.entityData.set(SCRIPT_TYPE, compound.getString("ScriptType"));
        }
        if (compound.contains("ScriptCommand")) {
            this.entityData.set(SCRIPT_COMMAND, compound.getString("ScriptCommand"));
        }
        if (compound.contains("InteractUsed")) {
            this.entityData.set(INTERACT_USED, compound.getBoolean("InteractUsed"));
        }
        // AI and invulnerability
        if (compound.contains("AiEnabled")) {
            boolean aiEnabled = compound.getBoolean("AiEnabled");
            this.entityData.set(AI_ENABLED, aiEnabled);
            this.setNoAi(!aiEnabled);
        }
        if (compound.contains("NpcInvulnerable")) {
            this.entityData.set(INVULNERABLE, compound.getBoolean("NpcInvulnerable"));
        }
        // Player nearby
        if (compound.contains("PlayerNearbyRadius")) {
            this.entityData.set(PLAYER_NEARBY_RADIUS, compound.getFloat("PlayerNearbyRadius"));
        }
        if (compound.contains("PlayerNearbyTriggered")) {
            this.entityData.set(PLAYER_NEARBY_TRIGGERED, compound.getBoolean("PlayerNearbyTriggered"));
        }
        // Look-at system
        if (compound.contains("LookAtActive")) {
            this.entityData.set(LOOK_AT_ACTIVE, compound.getBoolean("LookAtActive"));
        }
        if (compound.contains("LookAtContinuous")) {
            this.entityData.set(LOOK_AT_CONTINUOUS, compound.getBoolean("LookAtContinuous"));
        }
        if (compound.contains("LookAtTargetX")) {
            this.entityData.set(LOOK_AT_TARGET_X, compound.getFloat("LookAtTargetX"));
        }
        if (compound.contains("LookAtTargetY")) {
            this.entityData.set(LOOK_AT_TARGET_Y, compound.getFloat("LookAtTargetY"));
        }
        if (compound.contains("LookAtTargetZ")) {
            this.entityData.set(LOOK_AT_TARGET_Z, compound.getFloat("LookAtTargetZ"));
        }
        if (compound.contains("LookAtEntityId")) {
            this.entityData.set(LOOK_AT_ENTITY_ID, compound.getInt("LookAtEntityId"));
        }
        // View range limits
        if (compound.contains("ViewRangeMinYaw")) {
            this.entityData.set(VIEW_RANGE_MIN_YAW, compound.getFloat("ViewRangeMinYaw"));
        }
        if (compound.contains("ViewRangeMaxYaw")) {
            this.entityData.set(VIEW_RANGE_MAX_YAW, compound.getFloat("ViewRangeMaxYaw"));
        }
        if (compound.contains("ViewRangeMinPitch")) {
            this.entityData.set(VIEW_RANGE_MIN_PITCH, compound.getFloat("ViewRangeMinPitch"));
        }
        if (compound.contains("ViewRangeMaxPitch")) {
            this.entityData.set(VIEW_RANGE_MAX_PITCH, compound.getFloat("ViewRangeMaxPitch"));
        }
        // Movement system
        if (compound.contains("MovementState")) {
            this.entityData.set(MOVEMENT_STATE, compound.getString("MovementState"));
        }
        if (compound.contains("MovementTargetX")) {
            this.entityData.set(MOVEMENT_TARGET_X, compound.getFloat("MovementTargetX"));
        }
        if (compound.contains("MovementTargetY")) {
            this.entityData.set(MOVEMENT_TARGET_Y, compound.getFloat("MovementTargetY"));
        }
        if (compound.contains("MovementTargetZ")) {
            this.entityData.set(MOVEMENT_TARGET_Z, compound.getFloat("MovementTargetZ"));
        }
        if (compound.contains("MovementTargetEntityId")) {
            this.entityData.set(MOVEMENT_TARGET_ENTITY_ID, compound.getInt("MovementTargetEntityId"));
        }
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float width = this.entityData.get(HITBOX_WIDTH);
        float height = this.entityData.get(HITBOX_HEIGHT);
        return EntityDimensions.scalable(width, height);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 10, this::predicate));
    }

    private PlayState predicate(AnimationState<NpcEntity> state) {
        String deathAnim = this.entityData.get(DEATH_ANIM_NAME);

        // Play death animation if dead
        if (this.isDeadOrDying() && !deathAnim.isEmpty()) {
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold(deathAnim));
            return PlayState.CONTINUE;
        }

        // Check for custom animation
        String customAnim = this.entityData.get(CUSTOM_ANIMATION);
        String customMode = this.entityData.get(CUSTOM_ANIMATION_MODE);
        if (!customAnim.isEmpty() && !customMode.isEmpty()) {
            switch (customMode) {
                case "loop":
                    state.getController().setAnimation(RawAnimation.begin().thenLoop(customAnim));
                    break;
                case "hold":
                    state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold(customAnim));
                    break;
                case "once":
                    // Only set the animation if it's different from what we last played
                    if (!customAnim.equals(lastPlayedOnceAnimation)) {
                        state.getController().setAnimation(RawAnimation.begin().thenPlay(customAnim));
                        lastPlayedOnceAnimation = customAnim;
                    } else if (state.getController().getAnimationState() == AnimationController.State.STOPPED) {
                        // Animation finished playing - call stopAnimation to properly clean up
                        lastPlayedOnceAnimation = "";
                        stopAnimation();
                        // Fall through to normal animations below
                        break;
                    }
                    break;
            }
            // Only return if we're still playing custom animation
            if (!this.entityData.get(CUSTOM_ANIMATION).isEmpty()) {
                return PlayState.CONTINUE;
            }
        }

        // Normal movement animations
        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(this.entityData.get(WALK_ANIM_NAME)));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(this.entityData.get(IDLE_ANIM_NAME)));
        }
        return PlayState.CONTINUE;
    }

    public String getDeathAnimName() {
        return this.entityData.get(DEATH_ANIM_NAME);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        int deathDuration = this.entityData.get(DEATH_DURATION);
        // Use at least 1 tick to prevent instant removal
        if (deathDuration <= 0) deathDuration = 1;
        if (this.deathTime >= deathDuration && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    public int getDeathDuration() {
        return this.entityData.get(DEATH_DURATION);
    }

    // Script system methods
    public void setScript(String type, String command) {
        this.entityData.set(SCRIPT_TYPE, type != null ? type : "");
        this.entityData.set(SCRIPT_COMMAND, command != null ? command : "");
        // Reset interact used flag when script is set/updated
        this.entityData.set(INTERACT_USED, false);
    }

    public String getScriptType() {
        return this.entityData.get(SCRIPT_TYPE);
    }

    public String getScriptCommand() {
        return this.entityData.get(SCRIPT_COMMAND);
    }

    public boolean hasInteractScript() {
        return "interact".equals(getScriptType()) && !getScriptCommand().isEmpty();
    }

    public boolean hasDefaultScript() {
        return "default".equals(getScriptType()) && !getScriptCommand().isEmpty();
    }

    public boolean isInteractUsed() {
        return this.entityData.get(INTERACT_USED);
    }

    public void setInteractUsed(boolean used) {
        this.entityData.set(INTERACT_USED, used);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // AI control methods
    public boolean isAiEnabled() {
        return this.entityData.get(AI_ENABLED);
    }

    public void setAiEnabled(boolean enabled) {
        this.entityData.set(AI_ENABLED, enabled);
        this.setNoAi(!enabled);
    }

    // Invulnerability methods
    public boolean isNpcInvulnerable() {
        return this.entityData.get(INVULNERABLE);
    }

    public void setNpcInvulnerable(boolean invulnerable) {
        this.entityData.set(INVULNERABLE, invulnerable);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.entityData.get(INVULNERABLE)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    // Player nearby event methods
    public float getPlayerNearbyRadius() {
        return this.entityData.get(PLAYER_NEARBY_RADIUS);
    }

    public void setPlayerNearbyRadius(float radius) {
        this.entityData.set(PLAYER_NEARBY_RADIUS, radius);
        // Reset triggered flag when radius is changed
        this.entityData.set(PLAYER_NEARBY_TRIGGERED, false);
    }

    public boolean isPlayerNearbyTriggered() {
        return this.entityData.get(PLAYER_NEARBY_TRIGGERED);
    }

    public void setPlayerNearbyTriggered(boolean triggered) {
        this.entityData.set(PLAYER_NEARBY_TRIGGERED, triggered);
    }

    public boolean hasPlayerNearbyScript() {
        String scriptType = getScriptType();
        return scriptType.startsWith("player_nearby") && !getScriptCommand().isEmpty();
    }

    public float getPlayerNearbyScriptRadius() {
        String scriptType = getScriptType();
        if (scriptType.startsWith("player_nearby")) {
            // Parse radius from "player_nearby 5" format
            String[] parts = scriptType.split(" ");
            if (parts.length >= 2) {
                try {
                    return Float.parseFloat(parts[1]);
                } catch (NumberFormatException e) {
                    return 5.0f; // default radius
                }
            }
            return 5.0f; // default radius
        }
        return 0.0f;
    }

    // ===== Movement system methods =====

    public String getMovementState() {
        return this.entityData.get(MOVEMENT_STATE);
    }

    public void setMovementState(String state) {
        this.entityData.set(MOVEMENT_STATE, state != null ? state : "idle");
    }

    // Movement target accessors
    public float getMovementTargetX() {
        return this.entityData.get(MOVEMENT_TARGET_X);
    }

    public float getMovementTargetY() {
        return this.entityData.get(MOVEMENT_TARGET_Y);
    }

    public float getMovementTargetZ() {
        return this.entityData.get(MOVEMENT_TARGET_Z);
    }

    public void setMovementTarget(double x, double y, double z) {
        this.entityData.set(MOVEMENT_TARGET_X, (float) x);
        this.entityData.set(MOVEMENT_TARGET_Y, (float) y);
        this.entityData.set(MOVEMENT_TARGET_Z, (float) z);
    }

    public int getMovementTargetEntityId() {
        return this.entityData.get(MOVEMENT_TARGET_ENTITY_ID);
    }

    public void setMovementTargetEntityId(int id) {
        this.entityData.set(MOVEMENT_TARGET_ENTITY_ID, id);
    }

    // Patrol accessors
    public String getPatrolPoints() {
        return this.entityData.get(PATROL_POINTS);
    }

    public void setPatrolPoints(String points) {
        this.entityData.set(PATROL_POINTS, points != null ? points : "");
    }

    public int getPatrolIndex() {
        return this.entityData.get(PATROL_INDEX);
    }

    public void setPatrolIndex(int index) {
        this.entityData.set(PATROL_INDEX, index);
    }

    // Wander accessors
    public String getWanderPolygon() {
        return this.entityData.get(WANDER_POLYGON);
    }

    public void setWanderPolygon(String polygon) {
        this.entityData.set(WANDER_POLYGON, polygon != null ? polygon : "");
    }

    public float getWanderY() {
        return this.entityData.get(WANDER_Y);
    }

    public void setWanderY(float y) {
        this.entityData.set(WANDER_Y, y);
    }

    // Directional movement accessors
    public float getDirectionalRemaining() {
        return this.entityData.get(DIRECTIONAL_REMAINING);
    }

    public void setDirectionalRemaining(float remaining) {
        this.entityData.set(DIRECTIONAL_REMAINING, remaining);
    }

    public void setDirectionalVector(float x, float z) {
        this.entityData.set(DIRECTIONAL_VEC_X, x);
        this.entityData.set(DIRECTIONAL_VEC_Z, z);
    }

    // ===== Movement system methods (using NpcPathingBehavior) =====

    public boolean isMovementActive() {
        return pathingBehavior.isActive();
    }

    public void moveToPosition(double x, double y, double z) {
        setMovementState("goto");
        setMovementTarget(x, y, z);
        pathingBehavior.moveToPosition(new BlockPos((int) x, (int) y, (int) z));
    }

    public void stopMovement() {
        setMovementState("idle");
        pathingBehavior.stop();
        getNavigation().stop();
    }

    public void startPatrol(java.util.List<BlockPos> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        setMovementState("patrol");
        pathingBehavior.startPatrol(points);
    }

    public void startFollow(net.minecraft.world.entity.Entity target) {
        if (target == null) {
            return;
        }
        setMovementState("follow");
        setMovementTargetEntityId(target.getId());
        pathingBehavior.startFollow(target);
    }

    public void startFollow(net.minecraft.world.entity.Entity target, int minDistance, int maxDistance) {
        if (target == null) {
            return;
        }
        setMovementState("follow");
        setMovementTargetEntityId(target.getId());
        pathingBehavior.startFollow(target, minDistance, maxDistance);
    }

    public void startDirectionalMovement(String direction, float distance) {
        // Calculate target position based on direction
        double x = getX();
        double z = getZ();
        float yaw = getYRot();

        switch (direction.toLowerCase()) {
            case "forward":
                x -= Math.sin(Math.toRadians(yaw)) * distance;
                z += Math.cos(Math.toRadians(yaw)) * distance;
                break;
            case "backward":
                x += Math.sin(Math.toRadians(yaw)) * distance;
                z -= Math.cos(Math.toRadians(yaw)) * distance;
                break;
            case "left":
                x -= Math.sin(Math.toRadians(yaw - 90)) * distance;
                z += Math.cos(Math.toRadians(yaw - 90)) * distance;
                break;
            case "right":
                x -= Math.sin(Math.toRadians(yaw + 90)) * distance;
                z += Math.cos(Math.toRadians(yaw + 90)) * distance;
                break;
            default:
                return;
        }

        setMovementState("directional");
        moveToPosition(x, getY(), z);
    }

    public void startWander(double centerX, double centerY, double centerZ, float radius) {
        setMovementState("wander");
        setWanderY((float) centerY);
        pathingBehavior.startWander(new BlockPos((int) centerX, (int) centerY, (int) centerZ), (int) radius);
    }

    /**
     * Get the pathfinding behavior controller.
     */
    public NpcPathingBehavior getPathingBehavior() {
        return pathingBehavior;
    }

    // ===== Custom animation methods =====

    /**
     * Plays a custom animation on the NPC.
     * @param animationName The name of the animation to play
     * @param mode The play mode: "once" (play once then return to idle), "loop" (loop forever), "hold" (play and hold last frame)
     */
    public void playAnimation(String animationName, String mode) {
        // Reset tracking to allow replaying the same animation
        this.lastPlayedOnceAnimation = "";
        this.entityData.set(CUSTOM_ANIMATION, animationName != null ? animationName : "");
        this.entityData.set(CUSTOM_ANIMATION_MODE, mode != null ? mode : "once");
    }

    /**
     * Stops the current custom animation and returns to normal idle/walk animations.
     */
    public void stopAnimation() {
        this.entityData.set(CUSTOM_ANIMATION, "");
        this.entityData.set(CUSTOM_ANIMATION_MODE, "");
    }

    /**
     * Gets the currently playing custom animation name.
     */
    public String getCustomAnimation() {
        return this.entityData.get(CUSTOM_ANIMATION);
    }

    /**
     * Gets the current custom animation mode.
     */
    public String getCustomAnimationMode() {
        return this.entityData.get(CUSTOM_ANIMATION_MODE);
    }

    // ===== Look-at system methods =====

    public boolean isLookAtActive() {
        return this.entityData.get(LOOK_AT_ACTIVE);
    }

    public void setLookAtActive(boolean active) {
        this.entityData.set(LOOK_AT_ACTIVE, active);
    }

    public boolean isLookAtContinuous() {
        return this.entityData.get(LOOK_AT_CONTINUOUS);
    }

    public void setLookAtContinuous(boolean continuous) {
        this.entityData.set(LOOK_AT_CONTINUOUS, continuous);
    }

    public int getLookAtEntityId() {
        return this.entityData.get(LOOK_AT_ENTITY_ID);
    }

    public void setLookAtEntityId(int entityId) {
        this.entityData.set(LOOK_AT_ENTITY_ID, entityId);
    }

    // Look-at target position accessors
    public float getLookAtTargetX() {
        return this.entityData.get(LOOK_AT_TARGET_X);
    }

    public float getLookAtTargetY() {
        return this.entityData.get(LOOK_AT_TARGET_Y);
    }

    public float getLookAtTargetZ() {
        return this.entityData.get(LOOK_AT_TARGET_Z);
    }

    public void setLookAtTargetPosition(float x, float y, float z) {
        this.entityData.set(LOOK_AT_TARGET_X, x);
        this.entityData.set(LOOK_AT_TARGET_Y, y);
        this.entityData.set(LOOK_AT_TARGET_Z, z);
    }

    /**
     * Sets up the look-at target to specific coordinates.
     */
    public void setLookAtTarget(double x, double y, double z, boolean continuous) {
        this.entityData.set(LOOK_AT_TARGET_X, (float) x);
        this.entityData.set(LOOK_AT_TARGET_Y, (float) y);
        this.entityData.set(LOOK_AT_TARGET_Z, (float) z);
        this.entityData.set(LOOK_AT_ENTITY_ID, -1); // No entity target
        this.entityData.set(LOOK_AT_CONTINUOUS, continuous);
        this.entityData.set(LOOK_AT_ACTIVE, true);
    }

    /**
     * Sets up the look-at target to follow an entity.
     */
    public void setLookAtTarget(net.minecraft.world.entity.Entity target, boolean continuous) {
        this.entityData.set(LOOK_AT_TARGET_X, (float) target.getX());
        this.entityData.set(LOOK_AT_TARGET_Y, (float) target.getEyeY());
        this.entityData.set(LOOK_AT_TARGET_Z, (float) target.getZ());
        this.entityData.set(LOOK_AT_ENTITY_ID, target.getId());
        this.entityData.set(LOOK_AT_CONTINUOUS, continuous);
        this.entityData.set(LOOK_AT_ACTIVE, true);
    }

    /**
     * Stops the look-at behavior.
     */
    public void stopLookAt() {
        this.entityData.set(LOOK_AT_ACTIVE, false);
        this.entityData.set(LOOK_AT_ENTITY_ID, -1);
    }

    // ===== View range methods =====

    public float getViewRangeMinYaw() {
        return this.entityData.get(VIEW_RANGE_MIN_YAW);
    }

    public float getViewRangeMaxYaw() {
        return this.entityData.get(VIEW_RANGE_MAX_YAW);
    }

    public float getViewRangeMinPitch() {
        return this.entityData.get(VIEW_RANGE_MIN_PITCH);
    }

    public float getViewRangeMaxPitch() {
        return this.entityData.get(VIEW_RANGE_MAX_PITCH);
    }

    /**
     * Sets the view range limits for the NPC.
     * @param minYaw Minimum yaw angle (-180 to 180)
     * @param maxYaw Maximum yaw angle (-180 to 180)
     * @param minPitch Minimum pitch angle (-90 to 90)
     * @param maxPitch Maximum pitch angle (-90 to 90)
     */
    public void setViewRange(float minYaw, float maxYaw, float minPitch, float maxPitch) {
        this.entityData.set(VIEW_RANGE_MIN_YAW, minYaw);
        this.entityData.set(VIEW_RANGE_MAX_YAW, maxYaw);
        this.entityData.set(VIEW_RANGE_MIN_PITCH, minPitch);
        this.entityData.set(VIEW_RANGE_MAX_PITCH, maxPitch);
    }

    /**
     * Resets view range to default (no limits).
     */
    public void resetViewRange() {
        this.entityData.set(VIEW_RANGE_MIN_YAW, -180.0f);
        this.entityData.set(VIEW_RANGE_MAX_YAW, 180.0f);
        this.entityData.set(VIEW_RANGE_MIN_PITCH, -90.0f);
        this.entityData.set(VIEW_RANGE_MAX_PITCH, 90.0f);
    }

    @Override
    public void tick() {
        super.tick();

        // Check player_nearby event (server side only)
        if (!this.level().isClientSide() && hasPlayerNearbyScript() && !isPlayerNearbyTriggered()) {
            float radius = getPlayerNearbyScriptRadius();
            if (radius > 0) {
                Player nearestPlayer = this.level().getNearestPlayer(this, radius);
                if (nearestPlayer != null) {
                    // Mark as triggered to prevent repeated execution
                    setPlayerNearbyTriggered(true);
                    // Execute the script
                    executeScript(nearestPlayer);
                }
            }
        }

        // Process look-at behavior (server side only)
        if (!this.level().isClientSide()) {
            lookHandler.tick();
        }

        // Process pathfinding behavior (server side only)
        if (!this.level().isClientSide()) {
            pathingBehavior.tick();
        }
    }

    @Override
    public void aiStep() {
        // Always call super to process physics, gravity, and movement
        // even when noAi is true (which normally skips most of aiStep)
        super.aiStep();

        // When AI is disabled but we're pathfinding, we need to manually
        // apply movement since vanilla skips it for noAi entities
        if (this.isNoAi() && pathingBehavior.isActive()) {
            // Apply gravity
            if (!this.onGround() && !this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));
            }

            // Apply friction/drag
            Vec3 velocity = this.getDeltaMovement();
            if (this.onGround()) {
                velocity = velocity.multiply(0.91, 1.0, 0.91);
            } else {
                velocity = velocity.multiply(0.91, 0.98, 0.91);
            }
            this.setDeltaMovement(velocity);

            // Actually move the entity
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        }
    }

    /**
     * Executes the NPC's script command with placeholder replacements.
     */
    public void executeScript(@Nullable Player triggeringPlayer) {
        String command = getScriptCommand();
        if (command == null || command.isEmpty()) {
            return;
        }

        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        net.minecraft.server.MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }

        // Replace placeholders
        command = command
                .replace("{npc}", getCustomId())
                .replace("{x}", String.valueOf((int) getX()))
                .replace("{y}", String.valueOf((int) getY()))
                .replace("{z}", String.valueOf((int) getZ()));

        if (triggeringPlayer != null) {
            command = command.replace("{player}", triggeringPlayer.getName().getString());
        }

        // Execute command from server console
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    command
            );
        } catch (Exception e) {
            dcs.jagermeistars.talesmaker.TalesMaker.LOGGER.error("Failed to execute NPC script for '{}': {}", getCustomId(), command, e);
        }
    }
}
