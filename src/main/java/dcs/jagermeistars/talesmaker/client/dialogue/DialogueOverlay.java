package dcs.jagermeistars.talesmaker.client.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class DialogueOverlay {

    private static final int FADE_DURATION_MS = 200;
    private static final int ICON_SIZE = 16;
    private static final int PADDING_H = 6;
    private static final int PADDING_V = 4;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }

        DialogueManager.Dialogue dialogue = DialogueManager.getCurrentDialogue();
        if (dialogue == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate animation progress
        long elapsed = System.currentTimeMillis() - DialogueManager.getDialogueStartTime();
        long durationMs = (DialogueManager.getDuration() * 1000L) / 20L;

        // Fade in at start, fade out at end
        float alpha = 1.0f;
        if (elapsed < FADE_DURATION_MS) {
            alpha = (float) elapsed / FADE_DURATION_MS;
        } else if (elapsed > durationMs - FADE_DURATION_MS) {
            alpha = (float) (durationMs - elapsed) / FADE_DURATION_MS;
        }
        alpha = Math.max(0, Math.min(1, alpha));

        if (alpha <= 0) {
            return;
        }

        int alphaInt = (int) (alpha * 255);

        // Build the text: [Name]: message (single line, brackets inherit name style, colon is white)
        Component npcName = dialogue.npcName();
        Component nameWithBrackets = Component.empty()
                .append(Component.literal("[").withStyle(npcName.getStyle()))
                .append(npcName)
                .append(Component.literal("]").withStyle(npcName.getStyle()))
                .append(Component.literal(": "));
        Component fullText = nameWithBrackets.copy().append(dialogue.message());

        int textWidth = font.width(fullText);
        int iconSpace = dialogue.icon() != null ? ICON_SIZE + PADDING_H : 0;
        int boxWidth = iconSpace + textWidth + PADDING_H * 2;
        int boxHeight = Math.max(ICON_SIZE, font.lineHeight) + PADDING_V * 2;

        // Position: just above hotbar, centered
        int x = (screenWidth - boxWidth) / 2;
        int y = screenHeight - 48 - boxHeight;

        // Draw semi-transparent background
        int bgColor = (alphaInt * 140 / 255 << 24) | 0x000000;
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Draw icon if available
        int textStartX = x + PADDING_H;
        if (dialogue.icon() != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            int iconY = y + (boxHeight - ICON_SIZE) / 2;
            graphics.blit(dialogue.icon(), x + PADDING_H, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            textStartX = x + PADDING_H + ICON_SIZE + PADDING_H;
        }

        // Draw text on single line
        int textY = y + (boxHeight - font.lineHeight) / 2;
        int textColor = 0xFFFFFF | (alphaInt << 24);
        graphics.drawString(font, fullText, textStartX, textY, textColor, false);
    }
}
