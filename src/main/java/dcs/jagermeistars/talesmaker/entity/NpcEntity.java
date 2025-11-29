package dcs.jagermeistars.talesmaker.entity;

import dcs.jagermeistars.talesmaker.data.NpcPreset;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
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

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
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
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
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
        String idleAnim = this.entityData.get(IDLE_ANIM_NAME);
        String walkAnim = this.entityData.get(WALK_ANIM_NAME);
        String deathAnim = this.entityData.get(DEATH_ANIM_NAME);

        // Play death animation if dead
        if (this.isDeadOrDying() && !deathAnim.isEmpty()) {
            state.getController().setAnimation(RawAnimation.begin().thenPlayAndHold(deathAnim));
            return PlayState.CONTINUE;
        }

        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(walkAnim));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(idleAnim));
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
