package dcs.jagermeistars.talesmaker.entity;

import dcs.jagermeistars.talesmaker.data.NpcPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
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
    private static final EntityDataAccessor<String> PRESET_ID = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> NPC_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MODEL_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> ANIMATION_PATH = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> IDLE_ANIM_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> WALK_ANIM_NAME = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> HEAD = SynchedEntityData.defineId(NpcEntity.class,
            EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
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
        builder.define(PRESET_ID, "");
        builder.define(NPC_NAME, "NPC");
        builder.define(MODEL_PATH, "");
        builder.define(TEXTURE_PATH, "");
        builder.define(ANIMATION_PATH, "");
        builder.define(IDLE_ANIM_NAME, "idle");
        builder.define(WALK_ANIM_NAME, "walk");
        builder.define(HEAD, "armor_head");
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
        this.entityData.set(ANIMATION_PATH, preset.animations().path().toString());
        this.entityData.set(IDLE_ANIM_NAME, preset.animations().idle());
        this.entityData.set(WALK_ANIM_NAME, preset.animations().walk());
        this.entityData.set(HEAD, preset.head());
        this.setCustomName(preset.name());
        this.setCustomNameVisible(false);
    }

    public String getPresetId() {
        return this.entityData.get(PRESET_ID);
    }

    public ResourceLocation getPresetResourceLocation() {
        String presetId = getPresetId();
        return presetId.isEmpty() ? null : ResourceLocation.parse(presetId);
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
        return path.isEmpty() ? null : ResourceLocation.parse(path);
    }

    public ResourceLocation getTexturePath() {
        String path = this.entityData.get(TEXTURE_PATH);
        return path.isEmpty() ? null : ResourceLocation.parse(path);
    }

    public ResourceLocation getAnimationPath() {
        String path = this.entityData.get(ANIMATION_PATH);
        return path.isEmpty() ? null : ResourceLocation.parse(path);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("PresetId", getPresetId());
        compound.putString("NpcName", this.entityData.get(NPC_NAME));
        compound.putString("ModelPath", this.entityData.get(MODEL_PATH));
        compound.putString("TexturePath", this.entityData.get(TEXTURE_PATH));
        compound.putString("AnimationPath", this.entityData.get(ANIMATION_PATH));
        compound.putString("IdleAnim", this.entityData.get(IDLE_ANIM_NAME));
        compound.putString("WalkAnim", this.entityData.get(WALK_ANIM_NAME));
        compound.putString("Head", this.entityData.get(HEAD));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
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
        if (compound.contains("AnimationPath")) {
            this.entityData.set(ANIMATION_PATH, compound.getString("AnimationPath"));
        }
        if (compound.contains("IdleAnim")) {
            this.entityData.set(IDLE_ANIM_NAME, compound.getString("IdleAnim"));
        }
        if (compound.contains("WalkAnim")) {
            this.entityData.set(WALK_ANIM_NAME, compound.getString("WalkAnim"));
        }
        if (compound.contains("Head")) {
            this.entityData.set(HEAD, compound.getString("Head"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 10, this::predicate));
    }

    private PlayState predicate(AnimationState<NpcEntity> state) {
        String idleAnim = this.entityData.get(IDLE_ANIM_NAME);
        String walkAnim = this.entityData.get(WALK_ANIM_NAME);

        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(walkAnim));
        } else {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(idleAnim));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
