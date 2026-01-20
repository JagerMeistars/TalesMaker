package dcs.jagermeistars.talesmaker.client.notification;

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

import java.util.List;

/**
 * GMod-style resource error overlay.
 * Displays errors in the top-left corner with slide-in animation.
 * Single line format with stacking support (2x, 3x, etc.)
 */
@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class ResourceErrorOverlay {

    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int MARGIN = 4;
    private static final int ICON_WIDTH = 14;
    private static final int SLIDE_DISTANCE = 40; // Pixels to slide when fading
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;

    // Warning triangle icon
    private static final String ICON_WARNING = "\u26A0";

    // GMod-style yellow-orange color
    private static final int BG_COLOR = 0xFFAA00;
    private static final int TEXT_COLOR = 0x000000; // Black text for contrast

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        List<ResourceErrorManager.ResourceError> errors = ResourceErrorManager.getActiveErrors();
        if (errors.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float uiScale = calculateUiScale(screenWidth, screenHeight);
        int padding = scaleInt(PADDING, uiScale);
        int margin = scaleInt(MARGIN, uiScale);
        int iconWidth = scaleInt(ICON_WIDTH, uiScale);
        int lineHeight = Math.max(font.lineHeight, scaleInt(LINE_HEIGHT, uiScale));
        int slideDistance = scaleInt(SLIDE_DISTANCE, uiScale);
        int baseY = padding;

        for (ResourceErrorManager.ResourceError error : errors) {
            float appearProgress = error.getAppearProgress();
            float fadeProgress = error.getFadeProgress();

            // Smooth easing for appear animation (ease out - fast start, slow end)
            float easedAppear = 1f - (1f - appearProgress) * (1f - appearProgress);
            // Smooth easing for fade animation (ease in - slow start, fast end)
            float easedFade = fadeProgress * fadeProgress;

            // Calculate Y offset for slide-up animation when fading
            int slideUpOffset = (int) (easedFade * slideDistance);

            int y = baseY - slideUpOffset;

            // Combine alpha from appear and fade
            float alpha = easedAppear * (1f - easedFade);
            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) continue;

            int bgColor = (BG_COLOR & 0x00FFFFFF) | (alphaInt << 24);
            int textColor = (TEXT_COLOR & 0x00FFFFFF) | (alphaInt << 24);

            // Build translated text using Component
            String translatedType = Component.translatable(error.type().getTranslationKey()).getString();

            // Build single line: "⚠ [2x] Missing model: talesmaker:geo/entity/xxx.geo.json"
            StringBuilder sb = new StringBuilder();
            if (error.count() > 1) {
                sb.append(error.count()).append("x ");
            }
            sb.append(translatedType).append(": ").append(error.getDisplayPath());
            String displayText = sb.toString();

            // Calculate width based on text
            int textWidth = font.width(ICON_WARNING) + padding + font.width(displayText);
            int errorWidth = textWidth + padding * 2 + iconWidth;

            // Calculate X with slide-in from left (based on this error's width)
            int appearSlide = errorWidth + padding;
            int slideInOffset = (int) ((1f - easedAppear) * appearSlide);
            int x = padding - slideInOffset;

            // Calculate height (single line)
            int contentHeight = lineHeight + margin * 2;

            // Draw background
            graphics.fill(x, y, x + errorWidth, y + contentHeight, bgColor);

            // Draw icon and text centered vertically
            int textY = y + margin + (lineHeight - font.lineHeight) / 2;
            graphics.drawString(font, ICON_WARNING, x + padding, textY, textColor, false);
            graphics.drawString(font, displayText, x + padding + iconWidth, textY, textColor, false);

            baseY += contentHeight + margin;
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
}
