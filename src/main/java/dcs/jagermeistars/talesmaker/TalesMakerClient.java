package dcs.jagermeistars.talesmaker;

import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistory;
import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistoryScreen;
import dcs.jagermeistars.talesmaker.client.model.NpcModel;
import dcs.jagermeistars.talesmaker.client.notification.ResourceErrorManager;
import dcs.jagermeistars.talesmaker.client.renderer.NpcRenderer;
import dcs.jagermeistars.talesmaker.init.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side initialization for TalesMaker mod.
 */
@Mod(value = TalesMaker.MODID, dist = Dist.CLIENT)
public class TalesMakerClient {

    public static final KeyMapping HISTORY_KEY = new KeyMapping(
            "key.talesmaker.history",
            GLFW.GLFW_KEY_H,
            "key.categories.talesmaker"
    );

    public TalesMakerClient() {
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    @EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(ModEntities.NPC.get(), NpcRenderer::new);
        }

        @SubscribeEvent
        static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(HISTORY_KEY);
        }
    }

    private void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Clear cache when joining a world
        NpcModel.clearValidationCache();
        // Clear resource error cache to allow new errors to be displayed
        ResourceErrorManager.clearCache();

        // Load dialogue history for this world
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            DialogueHistory.onWorldJoin(mc.getCurrentServer().ip.replace(":", "_"));
        } else if (mc.getSingleplayerServer() != null) {
            DialogueHistory.onWorldJoin(mc.getSingleplayerServer().getWorldData().getLevelName());
        }
    }

    private void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DialogueHistory.onWorldLeave();
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (HISTORY_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new DialogueHistoryScreen());
            }
        }
    }
}
