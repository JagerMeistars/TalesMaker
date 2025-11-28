package dcs.jagermeistars.talesmaker;

import dcs.jagermeistars.talesmaker.client.renderer.NpcRenderer;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TalesMaker.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class TalesMakerClient {
    public TalesMakerClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TalesMaker.LOGGER.info("HELLO FROM CLIENT SETUP");
        TalesMaker.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // Register entity renderers
        EntityRenderers.register(ModEntities.NPC.get(), NpcRenderer::new);
    }
}
