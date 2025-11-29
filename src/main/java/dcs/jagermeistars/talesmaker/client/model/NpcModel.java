package dcs.jagermeistars.talesmaker.client.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.notification.NotificationManager;
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
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "geo/entity/placeholder.geo.json");
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/entity/placeholder.png");
    private static final ResourceLocation PLACEHOLDER_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "animations/entity/placeholder.animation.json");
    private static final ResourceLocation ERROR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/entity/error.png");

    private static final Set<ResourceLocation> MISSING_RESOURCES = Collections.synchronizedSet(new HashSet<>());

    public static void clearValidationCache() {
        MISSING_RESOURCES.clear();
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
        if (MISSING_RESOURCES.add(modelPath)) {
            NotificationManager.warning("Missing model: " + modelPath);
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
        if (MISSING_RESOURCES.add(texturePath)) {
            NotificationManager.warning("Missing texture: " + texturePath);
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
        if (MISSING_RESOURCES.add(animationPath)) {
            NotificationManager.warning("Missing animation: " + animationPath);
        }
        return PLACEHOLDER_ANIMATION;
    }

    @Override
    public void setCustomAnimations(NpcEntity animatable, long instanceId, AnimationState<NpcEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        String headBoneName = animatable.getHead();
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

        headBone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
        headBone.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
    }

    private boolean resourceExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }
}
