package dcs.jagermeistars.talesmaker.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import dcs.jagermeistars.talesmaker.client.renderer.layer.NpcHeldItemLayer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renderer for NPC entities using GeckoLib.
 */
public class NpcRenderer extends GeoEntityRenderer<NpcEntity> {

    private static final ResourceLocation PLACEHOLDER_EMISSIVE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/entity/placeholder.png");
    private static final ResourceLocation ERROR_EMISSIVE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/entity/error.png");

    public NpcRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NpcModel());
        this.shadowRadius = 0.4F;

        // Add held item layer for rendering items in NPC hands
        addRenderLayer(new NpcHeldItemLayer(this));

        // Add emissive layer
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, NpcEntity animatable, BakedGeoModel bakedModel,
                    RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                ResourceLocation emissive = getEmissiveTexture(animatable);
                if (emissive != null) {
                    RenderType emissiveType = RenderType.eyes(emissive);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable,
                            emissiveType, bufferSource.getBuffer(emissiveType),
                            partialTick, 15728640, packedOverlay,
                            0xFFFFFFFF);
                }
            }
        });
    }

    private static ResourceLocation getEmissiveTexture(NpcEntity animatable) {
        // If model is missing, use placeholder emissive
        ResourceLocation modelPath = animatable.getModelPath();
        if (modelPath == null || !resourceExists(modelPath)) {
            return PLACEHOLDER_EMISSIVE;
        }

        // If texture is missing, use error emissive
        ResourceLocation texturePath = animatable.getTexturePath();
        if (texturePath == null || !resourceExists(texturePath)) {
            return ERROR_EMISSIVE;
        }

        // Otherwise use entity's emissive (may be null)
        return animatable.getEmissivePath();
    }

    private static boolean resourceExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    @Override
    public RenderType getRenderType(NpcEntity animatable, ResourceLocation texture,
            MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
