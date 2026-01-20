package dcs.jagermeistars.talesmaker.client.notification;

import dcs.jagermeistars.talesmaker.TalesMaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

@EventBusSubscriber(modid = TalesMaker.MODID, value = Dist.CLIENT)
public class NotificationOverlay {

    private static final int PADDING = 8;
    private static final int NOTIFICATION_WIDTH = 280;
    private static final int LINE_HEIGHT = 12;
    private static final int MARGIN = 4;
    private static final int ICON_WIDTH = 14;
    private static final int SLIDE_DISTANCE = 40; // Pixels to slide when appearing/fading
    private static final int APPEAR_SLIDE = 20; // Pixels to slide in from right
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;

    // Unicode icons
    private static final String ICON_SUCCESS = "\u2714"; // checkmark
    private static final String ICON_WARNING = "\u26A0"; // warning triangle
    private static final String ICON_ERROR = "\u2716";   // cross

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        List<NotificationManager.Notification> notifications = NotificationManager.getNotifications();
        if (notifications.isEmpty()) {
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
        int notificationWidth = Math.max(scaleInt(180, uiScale), scaleInt(NOTIFICATION_WIDTH, uiScale));
        int lineHeight = Math.max(font.lineHeight, scaleInt(LINE_HEIGHT, uiScale));
        int slideDistance = scaleInt(SLIDE_DISTANCE, uiScale);
        int appearSlide = scaleInt(APPEAR_SLIDE, uiScale);
        int baseY = padding;

        for (NotificationManager.Notification notification : notifications) {
            float appearProgress = notification.getAppearProgress();
            float fadeProgress = notification.getFadeProgress();

            // Smooth easing for appear animation (ease out)
            float easedAppear = 1f - (1f - appearProgress) * (1f - appearProgress);
            // Smooth easing for fade animation (ease in)
            float easedFade = fadeProgress * fadeProgress;

            // Calculate X offset for slide-in animation (from right)
            int slideInOffset = (int) ((1f - easedAppear) * appearSlide);
            // Calculate Y offset for slide-up animation when fading
            int slideUpOffset = (int) (easedFade * slideDistance);

            int y = baseY - slideUpOffset;

            // Combine alpha from appear and fade
            float alpha = easedAppear * (1f - easedFade);
            int alphaInt = (int) (alpha * 255);
            if (alphaInt <= 0) continue;

            int bgColor = (notification.type().color & 0x00FFFFFF) | (alphaInt << 24);
            // Warning uses black text, others use white
            int textColor = notification.type() == NotificationManager.NotificationType.WARNING
                    ? (0x000000 | (alphaInt << 24))
                    : (0xFFFFFF | (alphaInt << 24));

            // Get icon based on type
            String icon = switch (notification.type()) {
                case SUCCESS -> ICON_SUCCESS;
                case WARNING -> ICON_WARNING;
                case ERROR -> ICON_ERROR;
            };

            // Build display message with count if > 1
            String displayMessage = notification.message();
            if (notification.count() > 1) {
                displayMessage = notification.count() + "x " + displayMessage;
            }

            // Calculate height based on message
            String[] lines = splitMessage(displayMessage, font, notificationWidth - padding * 2 - iconWidth);
            int contentHeight = (lines.length * lineHeight) + margin * 2;

            int x = screenWidth - notificationWidth - padding + slideInOffset;

            // Draw background
            graphics.fill(x, y, x + notificationWidth, y + contentHeight, bgColor);

            // Calculate vertical centering
            int textHeight = lines.length * lineHeight;
            int startY = y + (contentHeight - textHeight) / 2;

            // Draw icon centered vertically
            graphics.drawString(font, icon, x + padding, startY, textColor, false);

            // Draw message lines centered vertically (left-aligned horizontally)
            int lineY = startY;
            for (String line : lines) {
                graphics.drawString(font, line, x + padding + iconWidth, lineY, textColor, false);
                lineY += lineHeight;
            }

            baseY += contentHeight + margin;
        }
    }

    private static String[] splitMessage(String message, Font font, int maxWidth) {
        if (font.width(message) <= maxWidth) {
            return new String[]{message};
        }

        List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : message.split(" ")) {
            String test = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(test) <= maxWidth) {
                if (!currentLine.isEmpty()) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                currentLine.append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
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
