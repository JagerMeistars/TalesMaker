package dcs.jagermeistars.talesmaker.client.clue;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for clue inspection with dynamic resource loading.
 * Model and texture are provided by the ClueRenderEntity.
 * No animations are used.
 */
public class ClueGeoModel extends GeoModel<ClueRenderEntity> {

    private static final ResourceLocation PLACEHOLDER_MODEL =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "geo/clue/placeholder.geo.json");
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/clue/placeholder.png");
    private static final ResourceLocation EMPTY_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "animations/empty.animation.json");

    @Override
    public ResourceLocation getModelResource(ClueRenderEntity animatable) {
        ResourceLocation modelPath = animatable.getModelPath();
        if (modelPath == null) {
            return PLACEHOLDER_MODEL;
        }
        return modelPath;
    }

    @Override
    public ResourceLocation getTextureResource(ClueRenderEntity animatable) {
        ResourceLocation texturePath = animatable.getTexturePath();
        if (texturePath == null) {
            return PLACEHOLDER_TEXTURE;
        }
        return texturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(ClueRenderEntity animatable) {
        // No animations for clue models
        return EMPTY_ANIMATION;
    }
}
