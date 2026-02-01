package dcs.jagermeistars.talesmaker.client.tutorial;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class TutorialClientEvents {

    private static boolean lastAdvanceDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step == null) {
            return;
        }

        if (TutorialClientManager.isForcedActive()) {
            if (!step.allowInteraction() && !(mc.screen instanceof TutorialScreen)) {
                mc.setScreen(new TutorialScreen(TutorialClientManager.getCurrentPacket().tutorialId()));
            }
            if (step.allowInteraction() && mc.screen instanceof TutorialScreen) {
                mc.setScreen(null);
            }
        }

        if (mc.screen == null && step.allowInteraction()) {
            boolean down = TutorialInputUtil.isAdvanceKeyDown(step.advanceKey());
            if (down && !lastAdvanceDown) {
                TutorialClientManager.advanceStep();
            }
            lastAdvanceDown = down;
        } else {
            lastAdvanceDown = false;
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return;
        }
        if (event.getNewScreen() == null) {
            return;
        }
        if (event.getNewScreen() instanceof TutorialScreen
                || event.getNewScreen() instanceof TutorialPromptScreen
                || event.getNewScreen() instanceof TutorialOverlayScreen) {
            return;
        }
        event.setNewScreen(new TutorialOverlayScreen(event.getNewScreen()));
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.SUBTITLE_OVERLAY)) {
            return;
        }
        if (TutorialClientManager.getCurrentPacket() == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step == null || !step.allowInteraction()) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        TutorialOverlayRenderer.render(event.getGuiGraphics(),
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                partial);
    }
}
