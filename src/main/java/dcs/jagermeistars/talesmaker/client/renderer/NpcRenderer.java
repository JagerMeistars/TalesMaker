package dcs.jagermeistars.talesmaker.client.renderer;

import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NpcRenderer extends GeoEntityRenderer<NpcEntity> {

    public NpcRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NpcModel());
        this.shadowRadius = 0.4F;
    }

    @Override
    public net.minecraft.client.renderer.RenderType getRenderType(NpcEntity animatable,
            net.minecraft.resources.ResourceLocation texture,
            @javax.annotation.Nullable net.minecraft.client.renderer.MultiBufferSource bufferSource,
            float partialTick) {
        return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
    }
}
