package dcs.jagermeistars.talesmaker.client.interact;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.TalesMakerClient;
import dcs.jagermeistars.talesmaker.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Optional;
import java.util.function.Predicate;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class InteractHintOverlay {

    private static final int PADDING_H = 12;
    private static final int PADDING_V = 6;
    private static final int MARGIN_BOTTOM = 10;
    private static final int MARGIN_RIGHT = 10;
    private static final int FADE_TICKS = 10;

    private static NpcEntity lastTargetNpc = null;
    private static int fadeInTicks = 0;
    private static int fadeOutTicks = 0;
    private static boolean wasVisible = false;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            updateFadeOut();
            renderHint(mc, event.getGuiGraphics());
            return;
        }

        NpcEntity targetNpc = findLookedAtInteractableNpc(mc);

        if (targetNpc != null) {
            lastTargetNpc = targetNpc;
            fadeOutTicks = 0;
            if (fadeInTicks < FADE_TICKS) {
                fadeInTicks++;
            }
            wasVisible = true;
        } else {
            updateFadeOut();
        }

        renderHint(mc, event.getGuiGraphics());
    }

    private static void updateFadeOut() {
        if (wasVisible) {
            fadeInTicks = 0;
            if (fadeOutTicks < FADE_TICKS) {
                fadeOutTicks++;
            } else {
                wasVisible = false;
                lastTargetNpc = null;
            }
        }
    }

    private static void renderHint(Minecraft mc, GuiGraphics graphics) {
        if (lastTargetNpc == null || (!wasVisible && fadeOutTicks >= FADE_TICKS)) {
            return;
        }

        // Calculate alpha based on fade state
        float alpha;
        if (fadeInTicks < FADE_TICKS && fadeOutTicks == 0) {
            // Fading in
            alpha = (float) fadeInTicks / FADE_TICKS;
        } else if (fadeOutTicks > 0) {
            // Fading out
            alpha = 1.0f - ((float) fadeOutTicks / FADE_TICKS);
        } else {
            alpha = 1.0f;
        }

        if (alpha <= 0) {
            return;
        }

        int alphaInt = (int) (alpha * 255);

        // Get the interact key name
        String keyName = TalesMakerClient.INTERACT_KEY.getTranslatedKeyMessage().getString().toUpperCase();

        // Get hint text based on NPC custom ID with key substitution
        String npcId = lastTargetNpc.getCustomId();
        String translationKey = "gui.talesmaker.interact." + npcId;
        Component hintText = Component.translatable(translationKey, keyName);

        // Check if translation exists, fallback to default
        String translatedText = hintText.getString();
        if (translatedText.equals(translationKey)) {
            // No custom translation, use default with key substitution
            hintText = Component.translatable("gui.talesmaker.interact.default", keyName);
        }

        Font font = mc.font;
        int textWidth = font.width(hintText);
        int textHeight = font.lineHeight;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int boxWidth = textWidth + PADDING_H * 2;
        int boxHeight = textHeight + PADDING_V * 2;

        int x = screenWidth - boxWidth - MARGIN_RIGHT;
        int y = screenHeight - boxHeight - MARGIN_BOTTOM;

        // Draw semi-transparent background
        int bgColor = (alphaInt / 2) << 24; // Semi-transparent black
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Draw text
        int textColor = (alphaInt << 24) | 0xFFFFFF;
        graphics.drawString(font, hintText, x + PADDING_H, y + PADDING_V, textColor, false);
    }

    private static NpcEntity findLookedAtInteractableNpc(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        double reachDistance = 6.0;
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        AABB searchBox = mc.player.getBoundingBox().expandTowards(lookVec.scale(reachDistance)).inflate(1.0);

        Predicate<Entity> filter = entity -> entity instanceof NpcEntity npc
                && npc.hasInteractScript()
                && !npc.isInteractUsed()
                && !entity.isSpectator()
                && entity.isPickable();

        double closestDistance = reachDistance;
        NpcEntity closestNpc = null;

        for (Entity entity : mc.level.getEntities(mc.player, searchBox, filter)) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitResult = entityBox.clip(eyePos, endPos);

            if (hitResult.isPresent()) {
                double distance = eyePos.distanceTo(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestNpc = (NpcEntity) entity;
                }
            }
        }

        return closestNpc;
    }
}
