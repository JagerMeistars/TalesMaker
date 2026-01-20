package dcs.jagermeistars.talesmaker.client.dialogue;

import dcs.jagermeistars.talesmaker.TalesMakerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class DialogueHistoryScreen extends Screen {

    private static final int PADDING = 6;
    private static final int ENTRY_SPACING = 4;
    private static final int ICON_SIZE = 16;
    private static final int PADDING_H = 6;
    private static final int ENTRY_PADDING = 4;  // Padding inside entry panel
    private static final int BASE_WIDTH = 960;
    private static final int BASE_HEIGHT = 540;
    private static final float PANEL_SCALE = 0.85f;

    private int scrollOffset = 0;
    private List<DialogueHistory.HistoryEntry> entries;
    private List<RenderedEntry> renderedEntries;
    private int totalContentHeight = 0;

    // Panel dimensions
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private float uiScale = 1.0f;
    private int scaledPadding;
    private int scaledEntrySpacing;
    private int scaledIconSize;
    private int scaledPaddingH;
    private int scaledEntryPadding;

    private record RenderedEntry(ResourceLocation icon, List<FormattedCharSequence> lines, int height) {}

    public DialogueHistoryScreen() {
        super(Component.translatable("screen.talesmaker.history_title"));
        // Pre-initialize minecraft to prevent NPE in narration methods before init() is called
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        entries = DialogueHistory.getHistory();
        uiScale = Math.max(0.7f, calculateUiScale() * PANEL_SCALE);
        scaledPadding = scaleInt(PADDING);
        scaledEntrySpacing = scaleInt(ENTRY_SPACING);
        scaledIconSize = scaleInt(ICON_SIZE);
        scaledPaddingH = scaleInt(PADDING_H);
        scaledEntryPadding = scaleInt(ENTRY_PADDING);

        // Panel centered, compact width, taller
        int maxPanelWidth = Math.max(1, width - scaleInt(40));
        int minPanelWidth = Math.min(scaleInt(160), maxPanelWidth);
        panelWidth = Math.max(minPanelWidth, Math.min(scaleInt(210), maxPanelWidth));
        int maxPanelHeight = Math.max(1, height - scaleInt(40));
        int minPanelHeight = Math.min(scaleInt(200), maxPanelHeight);
        panelHeight = Math.max(minPanelHeight, Math.min(scaleInt(280), maxPanelHeight));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        // Pre-render entries with word wrapping
        rebuildRenderedEntries();

        // Scroll to bottom to show latest entries
        int contentAreaHeight = panelHeight - scaleInt(30) - scaledPadding;
        scrollOffset = Math.max(0, totalContentHeight - contentAreaHeight);
    }

    private void rebuildRenderedEntries() {
        renderedEntries = new ArrayList<>();
        totalContentHeight = 0;

        // Width for text inside entry panel (must match render calculation)
        int entryPanelWidth = panelWidth - scaledPadding * 2 - scaleInt(20);  // 10px margin on each side
        int textWidth = entryPanelWidth - scaledIconSize - scaledPaddingH - scaledEntryPadding * 2;

        for (DialogueHistory.HistoryEntry entry : entries) {
            Component npcName = DialogueHistory.parseNpcName(entry);
            Component message = DialogueHistory.parseMessage(entry);
            ResourceLocation icon = DialogueHistory.parseIcon(entry);

            Component fullText;
            if (DialogueHistory.isChoiceEntry(entry)) {
                fullText = message;
            } else {
                fullText = Component.empty()
                        .append(Component.literal("[").withStyle(npcName.getStyle()))
                        .append(npcName)
                        .append(Component.literal("]").withStyle(npcName.getStyle()))
                        .append(Component.literal(": "))
                        .append(message);
            }

            List<FormattedCharSequence> lines = font.split(fullText, textWidth);
            int entryHeight = Math.max(scaledIconSize, lines.size() * font.lineHeight) + scaledEntryPadding * 2;
            renderedEntries.add(new RenderedEntry(icon, lines, entryHeight));
            totalContentHeight += entryHeight + scaledEntrySpacing;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Guard against render being called before init or minecraft not set
        if (minecraft == null || font == null || entries == null) {
            return;
        }

        // Render darkened world background
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Panel background (no texture)
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xCC000000);

        // Inner border (darker)
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF000000);
        graphics.fill(panelLeft, panelTop + panelHeight - 1, panelLeft + panelWidth, panelTop + panelHeight, 0xFF000000);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, 0xFF000000);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF000000);

        // Title - "История"
        graphics.drawCenteredString(font, title, panelLeft + panelWidth / 2, panelTop + 7, 0xFFFFFF);

        int contentTop = panelTop + scaleInt(25);
        int contentBottom = panelTop + panelHeight - scaledPadding;
        int contentHeight = contentBottom - contentTop;

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talesmaker.no_history"),
                    panelLeft + panelWidth / 2, panelTop + panelHeight / 2, 0x888888);
            return;
        }

        // Enable scissor to clip entries
        graphics.enableScissor(panelLeft + scaledPadding, contentTop, panelLeft + panelWidth - scaledPadding, contentBottom);

        // Entry panel: centered in the entire panel (ignoring scrollbar for centering)
        int entryPanelWidth = panelWidth - scaledPadding * 2 - scaleInt(20);  // 10px margin on each side
        int entryPanelLeft = panelLeft + (panelWidth - entryPanelWidth) / 2;

        int y = contentTop - scrollOffset;
        for (int i = 0; i < renderedEntries.size(); i++) {
            RenderedEntry rendered = renderedEntries.get(i);

            if (y + rendered.height > contentTop - rendered.height && y < contentBottom) {
                // Draw semi-transparent dark background for entry
                graphics.fill(entryPanelLeft, y, entryPanelLeft + entryPanelWidth, y + rendered.height, 0x88000000);

                // Icon on the left (inside entry panel with padding)
                int contentX = entryPanelLeft + scaledEntryPadding;
                if (rendered.icon != null) {
                    int iconY = y + (rendered.height - scaledIconSize) / 2;
                    graphics.blit(rendered.icon, contentX, iconY, 0, 0, scaledIconSize, scaledIconSize, scaledIconSize, scaledIconSize);
                }
                contentX += scaledIconSize + scaledPaddingH;

                // Render wrapped lines left-aligned, vertically centered
                int textBlockHeight = rendered.lines.size() * font.lineHeight;
                int lineY = y + (rendered.height - textBlockHeight) / 2;
                for (FormattedCharSequence line : rendered.lines) {
                    graphics.drawString(font, line, contentX, lineY, 0xFFFFFF);
                    lineY += font.lineHeight;
                }
            }
            y += rendered.height + scaledEntrySpacing;
        }

        graphics.disableScissor();

        // Scrollbar
        if (totalContentHeight > contentHeight) {
            int scrollbarX = panelLeft + panelWidth - scaledPadding;
            int scrollbarHeight = Math.max(20, contentHeight * contentHeight / totalContentHeight);
            int maxScroll = totalContentHeight - contentHeight;
            int scrollbarY = contentTop + (int) ((contentHeight - scrollbarHeight) * ((float) scrollOffset / maxScroll));

            graphics.fill(scrollbarX, contentTop, scrollbarX + 4, contentBottom, 0x44FFFFFF);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarHeight, 0xAAFFFFFF);
        }

        // Render widgets
        for (var widget : this.renderables) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int contentHeight = panelHeight - scaleInt(30) - scaledPadding;
        int maxScroll = Math.max(0, totalContentHeight - contentHeight);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 20));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close screen when H is pressed again
        if (TalesMakerClient.HISTORY_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float calculateUiScale() {
        float scaleW = width / (float) BASE_WIDTH;
        float scaleH = height / (float) BASE_HEIGHT;
        return Math.max(1.0f, Math.min(scaleW, scaleH));
    }

    private int scaleInt(int value) {
        return Math.max(1, Math.round(value * uiScale));
    }
}


