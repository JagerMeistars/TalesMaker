package dcs.jagermeistars.talesmaker.client.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class DialogueOverlay {

    private static final int FADE_DURATION_MS = 200;
    private static final int ICON_SIZE = 16;
    private static final int PADDING_H = 6;
    private static final int PADDING_V = 4;
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
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
        float uiScale = calculateUiScale(screenWidth, screenHeight);
        int iconSize = scaleInt(ICON_SIZE, uiScale);
        int paddingH = scaleInt(PADDING_H, uiScale);
        int paddingV = scaleInt(PADDING_V, uiScale);

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

        // Build the text: [Name]: message (multi-line with automatic wrap)
        Component npcName = dialogue.npcName();
        Component nameWithBrackets = Component.empty()
                .append(Component.literal("[").withStyle(npcName.getStyle()))
                .append(npcName)
                .append(Component.literal("]").withStyle(npcName.getStyle()))
                .append(Component.literal(": "));
        Component fullText = nameWithBrackets.copy().append(dialogue.message());
        String fullTextPlain = fullText.getString();

        int iconSpace = dialogue.icon() != null ? iconSize + paddingH : 0;
        int targetTextWidth = scaleInt(200, uiScale);
        int maxTextWidth = Math.max(1, Math.min(screenWidth - iconSpace - paddingH * 2 - scaleInt(8, uiScale),
                targetTextWidth));
        List<String> lines = wrapWithHyphen(font, fullTextPlain, maxTextWidth);

        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        int boxWidth = iconSpace + maxLineWidth + paddingH * 2;
        int lineHeight = font.lineHeight + scaleInt(1, uiScale);
        int boxHeight = Math.max(iconSize, lines.size() * lineHeight) + paddingV * 2;

        // Position: just above hotbar, centered
        int x = (screenWidth - boxWidth) / 2;
        int y = screenHeight - scaleInt(48, uiScale) - boxHeight;

        // Draw semi-transparent background
        int bgColor = (alphaInt * 140 / 255 << 24) | 0x000000;
        graphics.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        // Draw icon if available
        int textStartX = x + paddingH;
        if (dialogue.icon() != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            int iconY = y + (boxHeight - iconSize) / 2;
            graphics.blit(dialogue.icon(), x + paddingH, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
            textStartX = x + paddingH + iconSize + paddingH;
        }

        // Draw wrapped text lines
        int textColor = 0xFFFFFF | (alphaInt << 24);
        int textBlockHeight = lines.size() * lineHeight;
        int textY = y + (boxHeight - textBlockHeight) / 2;
        for (String line : lines) {
            graphics.drawString(font, line, textStartX, textY, textColor, false);
            textY += lineHeight;
        }
    }

    private static float calculateUiScale(int width, int height) {
        float scaleW = width / (float) BASE_WIDTH;
        float scaleH = height / (float) BASE_HEIGHT;
        return Math.max(1.0f, Math.min(scaleW, scaleH));
    }

    private static int scaleInt(int value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    private static List<String> wrapWithHyphen(Font font, String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        if (font.width(text) <= maxWidth) {
            return List.of(text);
        }

        int hyphenWidth = font.width("-");
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            if (current.length() == 0) {
                if (font.width(word) <= maxWidth) {
                    current.append(word);
                } else {
                    splitLongWord(font, word, maxWidth, hyphenWidth, lines, current);
                }
                continue;
            }

            String withSpace = current + " " + word;
            if (font.width(withSpace) <= maxWidth) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                if (font.width(word) <= maxWidth) {
                    current.append(word);
                } else {
                    splitLongWord(font, word, maxWidth, hyphenWidth, lines, current);
                }
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static void splitLongWord(Font font, String word, int maxWidth, int hyphenWidth,
                                      List<String> lines, StringBuilder current) {
        int index = 0;
        while (index < word.length()) {
            StringBuilder part = new StringBuilder();
            int width = 0;
            int limit = Math.max(1, maxWidth - hyphenWidth);

            while (index < word.length()) {
                char ch = word.charAt(index);
                int chWidth = font.width(String.valueOf(ch));
                if (part.length() > 0 && width + chWidth > limit) {
                    break;
                }
                part.append(ch);
                width += chWidth;
                index++;
                if (width >= limit) {
                    break;
                }
            }

            if (index < word.length()) {
                lines.add(part + "-");
            } else {
                current.append(part);
            }
        }
    }
}
