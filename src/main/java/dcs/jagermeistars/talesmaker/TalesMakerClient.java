package dcs.jagermeistars.talesmaker;

import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.client.renderer.NpcRenderer;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side initialization for TalesMaker mod.
 */
@Mod(value = TalesMaker.MODID, dist = Dist.CLIENT)
public class TalesMakerClient {

    public TalesMakerClient() {
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    @EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.NPC.get(), NpcRenderer::new);
        }
    }

    private void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Clear cache when joining a world
        NpcModel.clearValidationCache();
    }
}
