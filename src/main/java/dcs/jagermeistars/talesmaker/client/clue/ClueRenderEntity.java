package dcs.jagermeistars.talesmaker.client.clue;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Fake GeoAnimatable for rendering clue models in GUI.
 * This is not a real entity, just a minimal implementation for GeckoLib rendering.
 */
public class ClueRenderEntity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ResourceLocation modelPath;
    private ResourceLocation texturePath;

    public ClueRenderEntity(ResourceLocation modelPath, ResourceLocation texturePath) {
        this.modelPath = modelPath;
        this.texturePath = texturePath;
    }

    public ResourceLocation getModelPath() {
        return modelPath;
    }

    public ResourceLocation getTexturePath() {
        return texturePath;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations needed for clue inspection
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return 0;
    }
}
