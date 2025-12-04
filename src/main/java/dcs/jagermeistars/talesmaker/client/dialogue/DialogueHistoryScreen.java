package dcs.jagermeistars.talesmaker.client.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
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

    // Custom background texture - the texture should match the panel size (e.g., 210x280 or use 256x256 and stretch)
    private static final ResourceLocation CUSTOM_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("talesmaker", "textures/gui/history.png");
    // Texture dimensions (should match your PNG file)
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int ENTRY_SPACING = 4;
    private static final int ICON_SIZE = 16;
    private static final int PADDING_H = 6;
    private static final int ENTRY_PADDING = 4;  // Padding inside entry panel

    private int scrollOffset = 0;
    private List<DialogueHistory.HistoryEntry> entries;
    private List<RenderedEntry> renderedEntries;
    private int totalContentHeight = 0;

    // Panel dimensions
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

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

        // Panel centered, compact width, taller
        panelWidth = Math.min(210, width - 40);
        panelHeight = Math.min(280, height - 40);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        // Pre-render entries with word wrapping
        rebuildRenderedEntries();

        // Scroll to bottom to show latest entries
        int contentAreaHeight = panelHeight - 30 - PADDING;
        scrollOffset = Math.max(0, totalContentHeight - contentAreaHeight);
    }

    private void rebuildRenderedEntries() {
        renderedEntries = new ArrayList<>();
        totalContentHeight = 0;

        // Width for text inside entry panel (must match render calculation)
        int entryPanelWidth = panelWidth - PADDING * 2 - 20;  // 10px margin on each side
        int textWidth = entryPanelWidth - ICON_SIZE - PADDING_H - ENTRY_PADDING * 2;

        for (DialogueHistory.HistoryEntry entry : entries) {
            Component npcName = DialogueHistory.parseNpcName(entry);
            Component message = DialogueHistory.parseMessage(entry);
            ResourceLocation icon = DialogueHistory.parseIcon(entry);

            Component fullText = Component.empty()
                    .append(Component.literal("[").withStyle(npcName.getStyle()))
                    .append(npcName)
                    .append(Component.literal("]").withStyle(npcName.getStyle()))
                    .append(Component.literal(": "))
                    .append(message);

            List<FormattedCharSequence> lines = font.split(fullText, textWidth);
            int entryHeight = Math.max(ICON_SIZE, lines.size() * LINE_HEIGHT) + ENTRY_PADDING * 2;
            renderedEntries.add(new RenderedEntry(icon, lines, entryHeight));
            totalContentHeight += entryHeight + ENTRY_SPACING;
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

        // Panel background using texture (single image, not tiled)
        if (CUSTOM_BACKGROUND != null) {
            // Custom texture: blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)
            // Use the full texture (0,0 to textureWidth,textureHeight) and stretch it to panel size
            RenderSystem.enableBlend();
            graphics.blit(CUSTOM_BACKGROUND, panelLeft, panelTop, panelWidth, panelHeight, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            RenderSystem.disableBlend();
        } else {
            // Fallback to tiled menu background
            graphics.blit(Screen.MENU_BACKGROUND, panelLeft, panelTop, panelLeft, panelTop, panelWidth, panelHeight, 32, 32);
        }

        // Inner border (darker)
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF000000);
        graphics.fill(panelLeft, panelTop + panelHeight - 1, panelLeft + panelWidth, panelTop + panelHeight, 0xFF000000);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, 0xFF000000);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF000000);

        // Title - "История"
        graphics.drawCenteredString(font, title, panelLeft + panelWidth / 2, panelTop + 7, 0xFFFFFF);

        int contentTop = panelTop + 25;
        int contentBottom = panelTop + panelHeight - PADDING;
        int contentHeight = contentBottom - contentTop;

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talesmaker.no_history"),
                    panelLeft + panelWidth / 2, panelTop + panelHeight / 2, 0x888888);
            return;
        }

        // Enable scissor to clip entries
        graphics.enableScissor(panelLeft + PADDING, contentTop, panelLeft + panelWidth - PADDING, contentBottom);

        // Entry panel: centered in the entire panel (ignoring scrollbar for centering)
        int entryPanelWidth = panelWidth - PADDING * 2 - 20;  // 10px margin on each side
        int entryPanelLeft = panelLeft + (panelWidth - entryPanelWidth) / 2;

        int y = contentTop - scrollOffset;
        for (int i = 0; i < renderedEntries.size(); i++) {
            RenderedEntry rendered = renderedEntries.get(i);

            if (y + rendered.height > contentTop - rendered.height && y < contentBottom) {
                // Draw semi-transparent dark background for entry
                graphics.fill(entryPanelLeft, y, entryPanelLeft + entryPanelWidth, y + rendered.height, 0x88000000);

                // Icon on the left (inside entry panel with padding)
                int contentX = entryPanelLeft + ENTRY_PADDING;
                if (rendered.icon != null) {
                    RenderSystem.enableBlend();
                    int iconY = y + (rendered.height - ICON_SIZE) / 2;
                    graphics.blit(rendered.icon, contentX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                    RenderSystem.disableBlend();
                }
                contentX += ICON_SIZE + PADDING_H;

                // Render wrapped lines left-aligned, vertically centered
                int textBlockHeight = rendered.lines.size() * LINE_HEIGHT;
                int lineY = y + (rendered.height - textBlockHeight) / 2;
                for (FormattedCharSequence line : rendered.lines) {
                    graphics.drawString(font, line, contentX, lineY, 0xFFFFFF);
                    lineY += LINE_HEIGHT;
                }
            }
            y += rendered.height + ENTRY_SPACING;
        }

        graphics.disableScissor();

        // Scrollbar
        if (totalContentHeight > contentHeight) {
            int scrollbarX = panelLeft + panelWidth - PADDING;
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
        int contentHeight = panelHeight - 30 - PADDING;
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
}
