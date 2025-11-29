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
    private static final int SLIDE_DISTANCE = 30; // Pixels to slide up when fading

    // Unicode icons
    private static final String ICON_SUCCESS = "\u2714"; // ✔ checkmark
    private static final String ICON_WARNING = "\u26A0"; // ⚠ warning triangle
    private static final String ICON_ERROR = "\u2716";   // ✖ cross

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
        int baseY = PADDING;

        for (NotificationManager.Notification notification : notifications) {
            float fadeProgress = notification.getFadeProgress();

            // Calculate Y offset for slide-up animation
            int slideOffset = (int) (fadeProgress * SLIDE_DISTANCE);
            int y = baseY - slideOffset;

            // Calculate alpha for fade out
            int alpha = (int) ((1f - fadeProgress) * 255);
            if (alpha <= 0) continue;

            int bgColor = (notification.type().color & 0x00FFFFFF) | (alpha << 24);
            // Warning uses black text, others use white
            int textColor = notification.type() == NotificationManager.NotificationType.WARNING
                    ? (0x000000 | (alpha << 24))
                    : (0xFFFFFF | (alpha << 24));

            // Get icon based on type
            String icon = switch (notification.type()) {
                case SUCCESS -> ICON_SUCCESS;
                case WARNING -> ICON_WARNING;
                case ERROR -> ICON_ERROR;
            };

            // Calculate height based on message
            String[] lines = splitMessage(notification.message(), font, NOTIFICATION_WIDTH - PADDING * 2 - ICON_WIDTH);
            int contentHeight = (lines.length * LINE_HEIGHT) + MARGIN * 2;

            int x = screenWidth - NOTIFICATION_WIDTH - PADDING;

            // Draw background
            graphics.fill(x, y, x + NOTIFICATION_WIDTH, y + contentHeight, bgColor);

            // Calculate vertical centering
            int textHeight = lines.length * LINE_HEIGHT;
            int startY = y + (contentHeight - textHeight) / 2;

            // Draw icon centered vertically
            graphics.drawString(font, icon, x + PADDING, startY, textColor, false);

            // Draw message lines centered vertically
            int lineY = startY;
            for (String line : lines) {
                graphics.drawString(font, line, x + PADDING + ICON_WIDTH, lineY, textColor, false);
                lineY += LINE_HEIGHT;
            }

            baseY += contentHeight + MARGIN;
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
}
