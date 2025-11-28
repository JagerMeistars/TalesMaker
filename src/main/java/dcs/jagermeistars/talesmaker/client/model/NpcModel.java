package dcs.jagermeistars.talesmaker.client.model;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NpcModel extends GeoModel<NpcEntity> {

    private static final ResourceLocation PLACEHOLDER_MODEL = ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID,
            "geo/entity/placeholder.geo.json");
    private static final ResourceLocation PLACEHOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID,
            "textures/entity/placeholder.png");
    private static final ResourceLocation PLACEHOLDER_ANIMATION = ResourceLocation
            .fromNamespaceAndPath(TalesMaker.MODID, "animations/entity/placeholder.animation.json");
    private static final ResourceLocation ERROR_TEXTURE = ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID,
            "textures/entity/error.png");

    private static final java.util.Set<ResourceLocation> MISSING_RESOURCES = java.util.Collections
            .synchronizedSet(new java.util.HashSet<>());

    @Override
    public ResourceLocation getModelResource(NpcEntity animatable) {
        ResourceLocation modelPath = animatable.getModelPath();
        if (modelPath != null) {
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(modelPath).isPresent()) {
                return modelPath;
            }
            if (MISSING_RESOURCES.add(modelPath)) {
                TalesMaker.LOGGER.error("Missing model resource: {}", modelPath);
            }
        }
        return PLACEHOLDER_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NpcEntity animatable) {
        ResourceLocation texturePath = animatable.getTexturePath();
        if (texturePath != null) {
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(texturePath)
                    .isPresent()) {
                return texturePath;
            }
            if (MISSING_RESOURCES.add(texturePath)) {
                TalesMaker.LOGGER.error("Missing texture resource: {}", texturePath);
            }
            // Check if error texture exists, otherwise use placeholder
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(ERROR_TEXTURE)
                    .isPresent()) {
                return ERROR_TEXTURE;
            }
        }
        return PLACEHOLDER_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(NpcEntity animatable) {
        // If model is missing, force placeholder animation to avoid crash/glitches
        ResourceLocation modelPath = animatable.getModelPath();
        if (modelPath != null && !net.minecraft.client.Minecraft.getInstance().getResourceManager()
                .getResource(modelPath).isPresent()) {
            return PLACEHOLDER_ANIMATION;
        }

        ResourceLocation animationPath = animatable.getAnimationPath();
        if (animationPath != null) {
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(animationPath)
                    .isPresent()) {
                return animationPath;
            }
            if (MISSING_RESOURCES.add(animationPath)) {
                TalesMaker.LOGGER.error("Missing animation resource: {}", animationPath);
            }
        }
        return PLACEHOLDER_ANIMATION;
    }

    @Override
    public void setCustomAnimations(NpcEntity animatable, long instanceId,
            software.bernie.geckolib.animation.AnimationState<NpcEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        software.bernie.geckolib.cache.object.GeoBone headBone = getAnimationProcessor().getBone(animatable.getHead());
        if (headBone != null) {
            software.bernie.geckolib.model.data.EntityModelData entityData = animationState
                    .getData(software.bernie.geckolib.constant.DataTickets.ENTITY_MODEL_DATA);

            // Add the look-at rotation to the existing animation rotation
            headBone.setRotX(headBone.getRotX() + entityData.headPitch() * net.minecraft.util.Mth.DEG_TO_RAD);
            headBone.setRotY(headBone.getRotY() + entityData.netHeadYaw() * net.minecraft.util.Mth.DEG_TO_RAD);
        }
    }
}
