package dcs.jagermeistars.talesmaker.client.tutorial;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public final class TutorialOverlayRenderer {

    private static final int PADDING = 8;
    private static final int TEXT_BG_COLOR = 0xCC000000;
    private static final int OVERLAY_COLOR = 0xB0000000;
    private static final int CUTOUT_BORDER = 0x80FFFFFF;

    private TutorialOverlayRenderer() {}

    public static void render(GuiGraphics graphics, int screenW, int screenH, float partialTick) {
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step == null) {
            return;
        }

        int cutoutX = (int) (Mth.clamp(step.cutoutX(), 0.0f, 1.0f) * screenW);
        int cutoutY = (int) (Mth.clamp(step.cutoutY(), 0.0f, 1.0f) * screenH);
        int cutoutW = (int) (Mth.clamp(step.cutoutWidth(), 0.0f, 1.0f) * screenW);
        int cutoutH = (int) (Mth.clamp(step.cutoutHeight(), 0.0f, 1.0f) * screenH);

        cutoutW = Math.max(1, Math.min(cutoutW, screenW - cutoutX));
        cutoutH = Math.max(1, Math.min(cutoutH, screenH - cutoutY));

        renderBlurAndOverlayOutsideCutout(graphics, screenW, screenH, cutoutX, cutoutY, cutoutW, cutoutH, partialTick);

        // Cutout border
        graphics.fill(cutoutX - 1, cutoutY - 1, cutoutX + cutoutW + 1, cutoutY, CUTOUT_BORDER);
        graphics.fill(cutoutX - 1, cutoutY + cutoutH, cutoutX + cutoutW + 1, cutoutY + cutoutH + 1, CUTOUT_BORDER);
        graphics.fill(cutoutX - 1, cutoutY, cutoutX, cutoutY + cutoutH, CUTOUT_BORDER);
        graphics.fill(cutoutX + cutoutW, cutoutY, cutoutX + cutoutW + 1, cutoutY + cutoutH, CUTOUT_BORDER);

        // Text block
        Font font = Minecraft.getInstance().font;
        int maxTextWidth = Math.max(120, screenW / 3);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(step.text(), maxTextWidth);
        int lineHeight = font.lineHeight;
        int textHeight = lines.size() * lineHeight;
        int maxLineWidth = 0;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        int boxW = maxLineWidth;
        int boxH = textHeight;
        int pad = PADDING;
        int gap = pad * 2;

        int textX;
        int textY;

        // Candidate placements (below, above, right, left)
        int belowX = cutoutX;
        int belowY = cutoutY + cutoutH + gap;

        int aboveX = cutoutX;
        int aboveY = cutoutY - boxH - gap;

        int rightX = cutoutX + cutoutW + gap;
        int rightY = cutoutY;

        int leftX = cutoutX - boxW - gap;
        int leftY = cutoutY;

        textX = Mth.clamp(belowX, pad, screenW - boxW - pad);
        textY = belowY;

        if (overlapsCutout(textX, textY, boxW, boxH, cutoutX, cutoutY, cutoutW, cutoutH)
                || textY + boxH + pad > screenH) {
            textX = Mth.clamp(aboveX, pad, screenW - boxW - pad);
            textY = aboveY;
        }

        if (overlapsCutout(textX, textY, boxW, boxH, cutoutX, cutoutY, cutoutW, cutoutH)
                || textY < pad) {
            textX = rightX;
            textY = rightY;
        }

        if (overlapsCutout(textX, textY, boxW, boxH, cutoutX, cutoutY, cutoutW, cutoutH)
                || textX + boxW + pad > screenW) {
            textX = leftX;
            textY = leftY;
        }

        textX = Mth.clamp(textX, pad, screenW - boxW - pad);
        textY = Mth.clamp(textY, pad, screenH - boxH - pad);

        if (overlapsCutout(textX, textY, boxW, boxH, cutoutX, cutoutY, cutoutW, cutoutH)) {
            textX = Mth.clamp((screenW - boxW) / 2, pad, screenW - boxW - pad);
            textY = Mth.clamp(screenH - boxH - pad * 2, pad, screenH - boxH - pad);
        }

        graphics.fill(textX - pad, textY - pad, textX + boxW + pad,
                textY + boxH + pad, TEXT_BG_COLOR);

        int y = textY;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.drawString(font, line, textX, y, 0xFFFFFF, false);
            y += lineHeight;
        }

        drawStepCounter(graphics, screenW, font);
    }

    private static void drawStepCounter(GuiGraphics graphics, int screenW, Font font) {
        int idx = TutorialClientManager.getStepIndex() + 1;
        int total = TutorialClientManager.getStepCount();
        Component counter = Component.literal(idx + "/" + total);
        int width = font.width(counter);
        graphics.drawString(font, counter, screenW - width - 6, 6, 0xFFFFFF, false);
    }

    private static void renderBlurAndOverlayOutsideCutout(GuiGraphics graphics, int screenW, int screenH,
                                                          int cutoutX, int cutoutY, int cutoutW, int cutoutH,
                                                          float partialTick) {
        renderBlurRegion(graphics, 0, 0, screenW, cutoutY, partialTick);
        renderBlurRegion(graphics, 0, cutoutY, cutoutX, cutoutY + cutoutH, partialTick);
        renderBlurRegion(graphics, cutoutX + cutoutW, cutoutY, screenW, cutoutY + cutoutH, partialTick);
        renderBlurRegion(graphics, 0, cutoutY + cutoutH, screenW, screenH, partialTick);
    }

    private static void renderBlurRegion(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, float partialTick) {
        if (minX >= maxX || minY >= maxY) {
            return;
        }
        graphics.enableScissor(minX, minY, maxX, maxY);
        renderBlurredBackground(partialTick);
        graphics.fill(minX, minY, maxX, maxY, OVERLAY_COLOR);
        graphics.disableScissor();
    }

    private static boolean overlapsCutout(int x, int y, int w, int h, int ox, int oy, int ow, int oh) {
        return x < ox + ow && x + w > ox && y < oy + oh && y + h > oy;
    }

    private static void renderBlurredBackground(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.disableDepthTest();
        mc.gameRenderer.processBlurEffect(partialTick);
        mc.getMainRenderTarget().bindWrite(false);
    }
}
