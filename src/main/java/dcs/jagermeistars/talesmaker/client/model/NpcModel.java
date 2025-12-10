package dcs.jagermeistars.talesmaker.client.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.notification.ResourceErrorManager;
import dcs.jagermeistars.talesmaker.data.NpcAnimationConfig;
import dcs.jagermeistars.talesmaker.data.NpcPreset;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * GeckoLib model for NPC entities with dynamic resource loading.
 * Supports custom models, textures, and animations per entity instance.
 */
public class NpcModel extends GeoModel<NpcEntity> {

    private static final ResourceLocation PLACEHOLDER_MODEL =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "geo/npc/placeholder.geo.json");
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/npc/placeholder.png");
    private static final ResourceLocation PLACEHOLDER_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "animations/npc/placeholder.animation.json");
    private static final ResourceLocation ERROR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/npc/error.png");

    // Track which entities have already reported errors (by UUID + resource type)
    // This prevents spam every frame but allows different entities to report the same error
    private static final Set<String> REPORTED_ENTITIES = Collections.synchronizedSet(new HashSet<>());

    public static void clearValidationCache() {
        REPORTED_ENTITIES.clear();
    }

    private String getReportKey(NpcEntity entity, String resourceType) {
        return entity.getUUID().toString() + ":" + resourceType;
    }

    private boolean isModelMissing(NpcEntity animatable) {
        ResourceLocation modelPath = animatable.getModelPath();
        return modelPath == null || !resourceExists(modelPath);
    }

    @Override
    public ResourceLocation getModelResource(NpcEntity animatable) {
        ResourceLocation modelPath = animatable.getModelPath();
        // If model path is null, data not yet synced - use placeholder silently
        if (modelPath == null) {
            return PLACEHOLDER_MODEL;
        }
        if (resourceExists(modelPath)) {
            return modelPath;
        }
        // Report error once per entity (until /reload)
        String key = getReportKey(animatable, "model");
        if (REPORTED_ENTITIES.add(key)) {
            ResourceErrorManager.addError(ResourceErrorManager.ResourceType.MODEL, modelPath);
        }
        return PLACEHOLDER_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NpcEntity animatable) {
        // If model is missing, use placeholder texture
        if (isModelMissing(animatable)) {
            return PLACEHOLDER_TEXTURE;
        }

        ResourceLocation texturePath = animatable.getTexturePath();
        // If texture path is null, data not yet synced - use placeholder silently
        if (texturePath == null) {
            return PLACEHOLDER_TEXTURE;
        }
        if (resourceExists(texturePath)) {
            return texturePath;
        }
        // Report error once per entity (until /reload)
        String key = getReportKey(animatable, "texture");
        if (REPORTED_ENTITIES.add(key)) {
            ResourceErrorManager.addError(ResourceErrorManager.ResourceType.TEXTURE, texturePath);
        }
        // Texture missing but model exists - use error texture
        return ERROR_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NpcEntity animatable) {
        // If model is missing, use placeholder animation
        if (isModelMissing(animatable)) {
            return PLACEHOLDER_ANIMATION;
        }

        ResourceLocation animationPath = animatable.getAnimationPath();
        // If animation path is null, data not yet synced - use placeholder silently
        if (animationPath == null) {
            return PLACEHOLDER_ANIMATION;
        }
        if (resourceExists(animationPath)) {
            return animationPath;
        }
        // Report error once per entity (until /reload)
        String key = getReportKey(animatable, "animation");
        if (REPORTED_ENTITIES.add(key)) {
            ResourceErrorManager.addError(ResourceErrorManager.ResourceType.ANIMATION, animationPath);
        }
        return PLACEHOLDER_ANIMATION;
    }

    @Override
    public void setCustomAnimations(NpcEntity animatable, long instanceId, AnimationState<NpcEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        // Check if head tracking is blocked by current animation
        if (animatable.getAnimationManager().isHeadBlocked()) {
            return;
        }

        // Get head tracking config from preset
        NpcAnimationConfig.HeadTrackingConfig headConfig = getHeadTrackingConfig(animatable);
        if (headConfig == null || !headConfig.enabled()) {
            return;
        }

        // Use configured bone name or fallback to entity's head setting
        String headBoneName = headConfig.bone();
        if (headBoneName == null || headBoneName.isEmpty()) {
            headBoneName = animatable.getHead();
        }
        if (headBoneName == null || headBoneName.isEmpty()) {
            return;
        }

        GeoBone headBone = getAnimationProcessor().getBone(headBoneName);
        if (headBone == null) {
            return;
        }

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (entityData == null) {
            return;
        }

        // Get raw head angles - these are relative to body rotation
        // headPitch: looking up/down (-90 to 90)
        // netHeadYaw: head rotation relative to body yaw
        float rawPitch = entityData.headPitch();
        float rawYaw = entityData.netHeadYaw();

        // Clamp rotation to configured limits
        float pitch = Mth.clamp(rawPitch, -headConfig.maxPitch(), headConfig.maxPitch());
        float yaw = Mth.clamp(rawYaw, -headConfig.maxYaw(), headConfig.maxYaw());

        // Convert to radians
        float pitchRad = pitch * Mth.DEG_TO_RAD;
        float yawRad = yaw * Mth.DEG_TO_RAD;

        if (headConfig.isAdditive()) {
            // ADDITIVE mode: add head rotation to current animation pose
            // GeckoLib applies animation values to bones before setCustomAnimations is called
            // We add head tracking rotation on top of the animation
            // Note: getRotX/Y returns the current bone rotation including animation
            headBone.setRotX(headBone.getRotX() + pitchRad);
            headBone.setRotY(headBone.getRotY() + yawRad);
        } else {
            // REPLACE mode: completely override animation rotation with head tracking
            headBone.setRotX(pitchRad);
            headBone.setRotY(yawRad);
        }
    }

    /**
     * Get head tracking configuration from NPC preset.
     */
    private NpcAnimationConfig.HeadTrackingConfig getHeadTrackingConfig(NpcEntity entity) {
        ResourceLocation presetId = entity.getPresetResourceLocation();
        if (presetId == null) {
            return NpcAnimationConfig.HeadTrackingConfig.DEFAULT;
        }

        NpcPreset preset = TalesMaker.PRESET_MANAGER.getPreset(presetId).orElse(null);
        if (preset == null) {
            return NpcAnimationConfig.HeadTrackingConfig.DEFAULT;
        }

        NpcAnimationConfig animConfig = preset.animations();
        if (animConfig == null) {
            return NpcAnimationConfig.HeadTrackingConfig.DEFAULT;
        }

        return animConfig.headTracking();
    }

    private boolean resourceExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
}
