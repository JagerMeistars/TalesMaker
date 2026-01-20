package dcs.jagermeistars.talesmaker.client.choice;

import dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistory;
import dcs.jagermeistars.talesmaker.data.choice.ChoiceWindow;
import dcs.jagermeistars.talesmaker.network.CloseChoicePacket;
import dcs.jagermeistars.talesmaker.network.OpenChoicePacket;
import dcs.jagermeistars.talesmaker.network.SelectChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Screen that displays dialogue choices for the player to select.
 * Supports two display modes: "dialog" and "full".
 */
public class ChoiceScreen extends Screen {

    // Colors
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFF888888;
    private static final int COLOR_PANEL_BG = 0xB3000000;
    private static final int COLOR_BUTTON_BG = 0x99000000;
    private static final int COLOR_BUTTON_HOVER = 0xCC2A2A2A;
    private static final int COLOR_BUTTON_LOCKED = 0x66000000;
    private static final int COLOR_TIMER_BAR = 0xFF44AA44;
    private static final int COLOR_TIMER_BG = 0xAA000000;
    private static final int COLOR_BORDER = 0xFF444444;

    // Text limits (base values, scaled at runtime)
    private static final int BUTTON_PADDING_X = 14;

    // Button render sizes (base on-screen values)
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT_SMALL = 28;   // 1 line
    private static final int BUTTON_HEIGHT_MEDIUM = 36;  // 2 lines
    private static final int BUTTON_HEIGHT_LARGE = 44;   // 3 lines

    // Dialog panel render size (base on-screen values)
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_PADDING_X = 16;
    private static final int PANEL_PADDING_TOP = 10;
    private static final int PANEL_PADDING_BOTTOM = 12;
    private static final int PANEL_TEXT_GAP = 6;
    private static final int PANEL_MIN_HEIGHT = 60;

    // Timer (simple box)
    private static final int TIMER_SIZE = 54;  // Base render size on screen
    private static final int TIMER_BAR_HEIGHT = 4;

    // Button size enum
    private enum ButtonSize {
        SMALL(BUTTON_HEIGHT_SMALL, 1),
        MEDIUM(BUTTON_HEIGHT_MEDIUM, 2),
        LARGE(BUTTON_HEIGHT_LARGE, 3);

        final int baseHeight;
        final int lines;

        ButtonSize(int baseHeight, int lines) {
            this.baseHeight = baseHeight;
            this.lines = lines;
        }

        int getHeight(float scale) {
            return Math.max(1, Math.round(baseHeight * scale));
        }
    }

    /**
     * Determines button size based on text length.
     */
    private static ButtonSize getButtonSize(int lineCount) {
        if (lineCount <= 1) return ButtonSize.SMALL;
        if (lineCount <= 2) return ButtonSize.MEDIUM;
        return ButtonSize.LARGE;
    }

    private final ResourceLocation windowId;
    private final Entity speaker;
    private final Component speakerName;
    private final Component dialogueText;
    private final List<OpenChoicePacket.ClientChoice> choices;
    private final String mode;
    private final int timerSeconds;
    private final int timeoutChoice;

    private List<ChoiceButton> choiceButtons;
    private int hoveredChoice = -1;

    // Timer
    private long timerStartTime;
    private boolean hasTimer;

    // Dialog mode panel
    private int dialogPanelTop;
    private int dialogPanelHeight;
    private List<FormattedCharSequence> wrappedDialogue;
    private float uiScale = 1.0f;
    private int scaledPanelWidth;
    private int scaledPanelHeight;
    private int scaledButtonWidth;
    private int scaledButtonPaddingX;
    private int scaledTimerSize;
    private int scaledMargin;
    private int scaledSpacing;
    private int scaledPanelPaddingX;
    private int scaledPanelPaddingTop;
    private int scaledPanelPaddingBottom;
    private int scaledPanelTextGap;
    private int scaledTimerBarHeight;

    public ChoiceScreen(ResourceLocation windowId, Entity speaker, Component speakerName,
                        Component dialogueText, List<OpenChoicePacket.ClientChoice> choices,
                        String mode, int timerSeconds, int timeoutChoice) {
        super(speakerName);
        this.windowId = windowId;
        this.speaker = speaker;
        this.speakerName = speakerName;
        this.dialogueText = dialogueText;
        this.choices = choices;
        this.mode = mode;
        this.timerSeconds = timerSeconds;
        this.timeoutChoice = timeoutChoice;
        this.hasTimer = timerSeconds > 0;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        timerStartTime = System.currentTimeMillis();
        uiScale = calculateUiScale();
        scaledMargin = scaleInt(14);
        int maxPanelWidth = Math.max(1, width - scaledMargin * 2);
        int minPanelWidth = Math.min(scaleInt(200), maxPanelWidth);
        scaledPanelWidth = Math.max(minPanelWidth, Math.min(scaleInt(PANEL_WIDTH), maxPanelWidth));
        int maxButtonWidth = Math.max(1, width - scaledMargin * 2);
        int minButtonWidth = Math.min(scaleInt(120), maxButtonWidth);
        scaledButtonWidth = Math.max(minButtonWidth, Math.min(scaleInt(BUTTON_WIDTH), maxButtonWidth));
        scaledButtonPaddingX = scaleInt(BUTTON_PADDING_X);
        scaledTimerSize = scaleInt(TIMER_SIZE);
        scaledTimerBarHeight = scaleInt(TIMER_BAR_HEIGHT);
        scaledSpacing = scaleInt(5);
        scaledPanelPaddingX = scaleInt(PANEL_PADDING_X);
        scaledPanelPaddingTop = scaleInt(PANEL_PADDING_TOP);
        scaledPanelPaddingBottom = scaleInt(PANEL_PADDING_BOTTOM);
        scaledPanelTextGap = scaleInt(PANEL_TEXT_GAP);

        if (isDialogMode()) {
            initDialogMode();
        } else {
            initFullMode();
        }
    }

    private boolean isDialogMode() {
        return ChoiceWindow.MODE_DIALOG.equals(mode);
    }

    /**
     * Initialize dialog mode: panel at bottom, choices at top-left.
     */
    private void initDialogMode() {
        // Wrap dialogue text for panel
        int textWidth = Math.max(1, scaledPanelWidth - scaledPanelPaddingX * 2);
        wrappedDialogue = font.split(dialogueText, textWidth);
        int textHeight = wrappedDialogue.size() * font.lineHeight
                + Math.max(0, wrappedDialogue.size() - 1) * scaleInt(1);

        // Dialog panel at bottom with content-based height
        dialogPanelHeight = scaledPanelPaddingTop + font.lineHeight + scaledPanelTextGap
                + textHeight + scaledPanelPaddingBottom;
        dialogPanelHeight = Math.max(dialogPanelHeight, scaleInt(PANEL_MIN_HEIGHT));
        int maxPanelHeight = Math.max(1, height - scaledMargin * 2);
        dialogPanelHeight = Math.min(dialogPanelHeight, maxPanelHeight);
        dialogPanelTop = height - dialogPanelHeight;

        // Choice buttons at top-left with dynamic sizing
        choiceButtons = new ArrayList<>();
        int buttonX = scaledMargin;
        int buttonY = scaledMargin + scaleInt(4);
        int spacing = scaledSpacing;

        for (int i = 0; i < choices.size(); i++) {
            OpenChoicePacket.ClientChoice choice = choices.get(i);
            int maxTextWidth = Math.max(1, scaledButtonWidth - scaledButtonPaddingX * 2);
            List<FormattedCharSequence> lines = font.split(choice.text(), maxTextWidth);
            ButtonSize size = getButtonSize(lines.size());

            // Limit lines to max for size
            if (lines.size() > size.lines) {
                lines = lines.subList(0, size.lines);
            }

            choiceButtons.add(new ChoiceButton(
                    buttonX, buttonY, scaledButtonWidth, size.getHeight(uiScale),
                    i, choice.text(), choice.locked(), choice.lockedMessage(),
                    size, lines
            ));
            buttonY += size.getHeight(uiScale) + spacing;
        }
    }

    /**
     * Initialize full mode: question at top-left, choices below as simple text.
     */
    private void initFullMode() {
        choiceButtons = new ArrayList<>();
        int startX = scaledMargin;
        int questionY = scaleInt(40);
        int startY = questionY + font.lineHeight + scaleInt(20); // Below question text
        int buttonHeight = font.lineHeight + scaleInt(8);
        int spacing = scaledSpacing;

        for (int i = 0; i < choices.size(); i++) {
            OpenChoicePacket.ClientChoice choice = choices.get(i);
            String textStr = choice.text().getString();
            int textWidth = font.width(textStr) + scaleInt(30); // Extra for lock icon
            List<FormattedCharSequence> lines = List.of();
            choiceButtons.add(new ChoiceButton(
                    startX, startY, Math.max(textWidth, scaleInt(150)), buttonHeight,
                    i, choice.text(), choice.locked(), choice.lockedMessage(),
                    ButtonSize.SMALL, lines
            ));
            startY += buttonHeight + spacing;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Update hover state
        hoveredChoice = -1;
        for (int i = 0; i < choiceButtons.size(); i++) {
            if (choiceButtons.get(i).isMouseOver(mouseX, mouseY)) {
                hoveredChoice = i;
                break;
            }
        }

        if (isDialogMode()) {
            renderDialogMode(graphics, mouseX, mouseY, partialTick);
        } else {
            renderFullMode(graphics, mouseX, mouseY, partialTick);
        }

        // Render timer if present
        if (hasTimer) {
            renderTimer(graphics);
        }

        // Render tooltip for locked choice
        if (hoveredChoice >= 0) {
            ChoiceButton hovered = choiceButtons.get(hoveredChoice);
            if (hovered.locked && hovered.lockedMessage != null) {
                graphics.renderTooltip(font, hovered.lockedMessage, mouseX, mouseY);
            }
        }
    }

    /**
     * Render dialog mode: panel at bottom (centered), choices at top-left.
     */
    private void renderDialogMode(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dialog panel - centered at bottom with fixed size
        int panelLeft = (width - scaledPanelWidth) / 2;
        drawPanelBackground(graphics, panelLeft, dialogPanelTop, scaledPanelWidth, dialogPanelHeight);

        // Speaker name (uses color from Component)
        int nameX = panelLeft + scaledPanelPaddingX;
        int nameY = dialogPanelTop + scaledPanelPaddingTop;
        graphics.drawString(font, speakerName, nameX, nameY, COLOR_WHITE);

        // Dialogue text (below speaker name with small gap)
        int textY = nameY + font.lineHeight + scaledPanelTextGap;
        for (FormattedCharSequence line : wrappedDialogue) {
            graphics.drawString(font, line, panelLeft + scaledPanelPaddingX, textY, COLOR_WHITE);
            textY += font.lineHeight + scaleInt(1);
        }

        // Render choice buttons (dialog style with background)
        for (ChoiceButton button : choiceButtons) {
            renderDialogButton(graphics, button, hoveredChoice == button.index);
        }
    }

    /**
     * Render a dialog-style button with fixed-size background texture.
     */
    private void renderDialogButton(GuiGraphics graphics, ChoiceButton button, boolean hovered) {
        int bgColor;
        if (button.locked) {
            bgColor = COLOR_BUTTON_LOCKED;
        } else {
            bgColor = hovered ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        }
        drawPanelBackground(graphics, button.x, button.y, button.width, button.height, bgColor);

        // Text color
        int textColor = button.locked ? COLOR_GRAY : COLOR_WHITE;

        // Calculate vertical centering for all lines
        int totalTextHeight = button.lines.size() * font.lineHeight;
        int textY = button.y + (button.height - totalTextHeight) / 2;

        // Render each line of text (centered horizontally, no shadow)
        for (FormattedCharSequence line : button.lines) {
            int lineWidth = font.width(line);
            int textX = button.x + (button.width - lineWidth) / 2;

            // Draw text without shadow
            graphics.drawString(font, line, textX, textY, textColor, false);

            // Strikethrough for locked buttons
            if (button.locked) {
                int strikeY = textY + font.lineHeight / 2;
                graphics.fill(textX, strikeY, textX + lineWidth, strikeY + 1, COLOR_GRAY);
            }

            textY += font.lineHeight;
        }
    }

    /**
     * Render full mode: question at top, choices as simple text below.
     */
    private void renderFullMode(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Question text at top-left
        graphics.drawString(font, dialogueText, scaledMargin, scaleInt(40), COLOR_WHITE);

        // Render choice buttons (simple text style)
        for (ChoiceButton button : choiceButtons) {
            renderFullButton(graphics, button, hoveredChoice == button.index);
        }
    }

    /**
     * Render a full-mode button (simple text, no background).
     */
    private void renderFullButton(GuiGraphics graphics, ChoiceButton button, boolean hovered) {
        int textColor = button.locked ? COLOR_GRAY : COLOR_WHITE;
        int textY = button.y + (button.height - font.lineHeight) / 2;
        int textX = button.x;

        // Choice text (no shadow)
        graphics.drawString(font, button.text, textX, textY, textColor, false);

        // Strikethrough for locked buttons
        if (button.locked) {
            int textWidth = font.width(button.text);
            int strikeY = textY + font.lineHeight / 2;
            graphics.fill(textX, strikeY, textX + textWidth, strikeY + 1, COLOR_GRAY);
        }
    }

    /**
     * Render timer with animated texture frames.
     */
    private void renderTimer(GuiGraphics graphics) {
        long elapsed = System.currentTimeMillis() - timerStartTime;
        float remainingSeconds = timerSeconds - (elapsed / 1000f);

        if (remainingSeconds <= 0) {
            // Timer expired - select random or specified choice
            onTimerExpired();
            return;
        }

        float progress = remainingSeconds / timerSeconds;

        // Timer position (top-right corner)
        int timerX = width - scaledTimerSize - scaledMargin;
        int timerY = scaledMargin;

        drawPanelBackground(graphics, timerX, timerY, scaledTimerSize, scaledTimerSize, COLOR_TIMER_BG);
        int barWidth = (int) (scaledTimerSize * progress);
        int barY = timerY + scaledTimerSize - scaledTimerBarHeight;
        graphics.fill(timerX, barY, timerX + barWidth, barY + scaledTimerBarHeight, COLOR_TIMER_BAR);

        // Draw seconds number in center (scaled 2x, without shadow)
        String timeText = String.valueOf((int) Math.ceil(remainingSeconds));
        int textWidth = font.width(timeText);
        int textHeight = font.lineHeight;

        // Scale text by 2x
        float scale = Math.max(1.0f, 2.0f * uiScale);
        int scaledTextWidth = (int) (textWidth * scale);
        int scaledTextHeight = (int) (textHeight * scale);

        int textX = timerX + (scaledTimerSize - scaledTextWidth) / 2;
        int textY = timerY + (scaledTimerSize - scaledTextHeight) / 2;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, timeText, 0, 0, COLOR_WHITE, false);
        graphics.pose().popPose();
    }

    private void onTimerExpired() {
        hasTimer = false; // Prevent multiple triggers

        // Find valid (unlocked) choices
        List<Integer> validChoices = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            if (!choices.get(i).locked()) {
                validChoices.add(i);
            }
        }

        if (validChoices.isEmpty()) {
            // No valid choices, just close
            PacketDistributor.sendToServer(new CloseChoicePacket(windowId));
            onClose();
            return;
        }

        int selectedIndex;
        if (timeoutChoice >= 0 && timeoutChoice < choices.size() && !choices.get(timeoutChoice).locked()) {
            selectedIndex = timeoutChoice;
        } else {
            // Random selection from valid choices
            selectedIndex = validChoices.get(new Random().nextInt(validChoices.size()));
        }

        DialogueHistory.addChoiceEntry(choices.get(selectedIndex).text());
        // Send selection
        PacketDistributor.sendToServer(new SelectChoicePacket(windowId, selectedIndex, speaker.getId()));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredChoice >= 0) {
            ChoiceButton clicked = choiceButtons.get(hoveredChoice);
            if (!clicked.locked) {
                DialogueHistory.addChoiceEntry(clicked.text);
                PacketDistributor.sendToServer(new SelectChoicePacket(
                        windowId, hoveredChoice, speaker.getId()
                ));
                onClose();
                return true;
            } else {
                // Locked feedback sound
                if (minecraft.player != null) {
                    minecraft.player.playSound(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                            0.5f, 0.5f
                    );
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            PacketDistributor.sendToServer(new CloseChoicePacket(windowId));
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        super.onClose();
        ChoiceCameraController.stopCinematic();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /**
     * Internal button representation.
     */
    private class ChoiceButton {
        final int x, y, width, height;
        final int index;
        final Component text;
        final boolean locked;
        final Component lockedMessage;
        final ButtonSize size;
        final List<FormattedCharSequence> lines;

        ChoiceButton(int x, int y, int width, int height, int index,
                     Component text, boolean locked, Component lockedMessage,
                     ButtonSize size, List<FormattedCharSequence> lines) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.index = index;
            this.text = text;
            this.locked = locked;
            this.lockedMessage = lockedMessage;
            this.size = size;
            this.lines = lines;
        }

        boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private void drawPanelBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        drawPanelBackground(graphics, x, y, width, height, COLOR_PANEL_BG);
    }

    private void drawPanelBackground(GuiGraphics graphics, int x, int y, int width, int height, int fillColor) {
        graphics.fill(x, y, x + width, y + height, fillColor);
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
    }

    private int scaleInt(int value) {
        return Math.max(1, Math.round(value * uiScale));
    }

    private float calculateUiScale() {
        float baseWidth = 960.0f;
        float baseHeight = 540.0f;
        float scaleW = width / baseWidth;
        float scaleH = height / baseHeight;
        return Math.max(1.0f, Math.min(scaleW, scaleH));
    }
}
