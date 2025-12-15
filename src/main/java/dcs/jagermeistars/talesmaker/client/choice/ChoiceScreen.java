package dcs.jagermeistars.talesmaker.client.choice;

import com.mojang.blaze3d.systems.RenderSystem;
import dcs.jagermeistars.talesmaker.TalesMaker;
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
    private static final int COLOR_PANEL_BG = 0xDD000000;
    private static final int COLOR_BUTTON_BG = 0x88000000;
    private static final int COLOR_BUTTON_HOVER = 0xAA333333;
    private static final int COLOR_TIMER_BAR = 0xFF44AA44;
    private static final int COLOR_TIMER_BG = 0xFF222222;
    private static final int COLOR_BORDER = 0xFF555555;

    // Text limits
    private static final int MAX_CHARS_PER_LINE = 28;
    private static final int BUTTON_PADDING_X = 15;

    // Scale factor for textures (2x scale for consistent pixel appearance)
    private static final int TEXTURE_SCALE = 2;

    // Button render sizes (on screen)
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT_SMALL = 36;   // 1 line
    private static final int BUTTON_HEIGHT_MEDIUM = 48;  // 2 lines
    private static final int BUTTON_HEIGHT_LARGE = 60;   // 3 lines

    // Button texture sizes (actual texture dimensions = render size / scale)
    private static final int BUTTON_TEX_WIDTH = BUTTON_WIDTH / TEXTURE_SCALE;           // 100
    private static final int BUTTON_TEX_HEIGHT_SMALL = BUTTON_HEIGHT_SMALL / TEXTURE_SCALE;   // 10
    private static final int BUTTON_TEX_HEIGHT_MEDIUM = BUTTON_HEIGHT_MEDIUM / TEXTURE_SCALE; // 18
    private static final int BUTTON_TEX_HEIGHT_LARGE = BUTTON_HEIGHT_LARGE / TEXTURE_SCALE;   // 26

    // Dialog panel render size (on screen)
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 84;

    // Dialog panel texture size (actual texture dimensions)
    private static final int PANEL_TEX_WIDTH = PANEL_WIDTH / TEXTURE_SCALE;   // 320
    private static final int PANEL_TEX_HEIGHT = PANEL_HEIGHT / TEXTURE_SCALE; // 36

    // Button textures - Small (1 line)
    private static final ResourceLocation BUTTON_SMALL =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_small.png");
    private static final ResourceLocation BUTTON_SMALL_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_small_hover.png");
    private static final ResourceLocation BUTTON_SMALL_LOCKED =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_small_locked.png");
    private static final ResourceLocation BUTTON_SMALL_LOCKED_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_small_locked_hover.png");

    // Button textures - Medium (2 lines)
    private static final ResourceLocation BUTTON_MEDIUM =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_medium.png");
    private static final ResourceLocation BUTTON_MEDIUM_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_medium_hover.png");
    private static final ResourceLocation BUTTON_MEDIUM_LOCKED =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_medium_locked.png");
    private static final ResourceLocation BUTTON_MEDIUM_LOCKED_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_medium_locked_hover.png");

    // Button textures - Large (3 lines)
    private static final ResourceLocation BUTTON_LARGE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_large.png");
    private static final ResourceLocation BUTTON_LARGE_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_large_hover.png");
    private static final ResourceLocation BUTTON_LARGE_LOCKED =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_large_locked.png");
    private static final ResourceLocation BUTTON_LARGE_LOCKED_HOVER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/button_large_locked_hover.png");

    // Dialog panel texture
    private static final ResourceLocation DIALOG_PANEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/choice/dialog_panel.png");

    // Timer textures (12 frames)
    private static final int TIMER_SIZE = 64;  // Render size on screen
    private static final int TIMER_TEX_SIZE = TIMER_SIZE / TEXTURE_SCALE;  // Texture size (32x32)
    private static final ResourceLocation[] TIMER_TEXTURES = new ResourceLocation[12];
    static {
        for (int i = 0; i < 12; i++) {
            TIMER_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID,
                    "textures/gui/choice/timer_" + (i + 1) + ".png");
        }
    }

    // Button size enum
    private enum ButtonSize {
        SMALL(BUTTON_HEIGHT_SMALL, 1),
        MEDIUM(BUTTON_HEIGHT_MEDIUM, 2),
        LARGE(BUTTON_HEIGHT_LARGE, 3);

        final int height;
        final int lines;

        ButtonSize(int height, int lines) {
            this.height = height;
            this.lines = lines;
        }
    }

    /**
     * Determines button size based on text length.
     */
    private static ButtonSize getButtonSize(String text) {
        int len = text.length();
        if (len <= MAX_CHARS_PER_LINE) return ButtonSize.SMALL;
        if (len <= MAX_CHARS_PER_LINE * 2) return ButtonSize.MEDIUM;
        return ButtonSize.LARGE;
    }

    /**
     * Splits text into lines of maximum maxChars characters, breaking at word boundaries.
     */
    private static List<String> splitText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= maxChars) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxChars) {
                currentLine.append(" ").append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
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
        // Dialog panel at bottom (fixed size, width multiple of 16)
        dialogPanelHeight = PANEL_HEIGHT;
        dialogPanelTop = height - dialogPanelHeight;

        // Wrap dialogue text for panel
        int textWidth = PANEL_WIDTH - 30;
        wrappedDialogue = font.split(dialogueText, textWidth);

        // Choice buttons at top-left with dynamic sizing
        choiceButtons = new ArrayList<>();
        int buttonX = 20;
        int buttonY = 20;
        int spacing = 4;

        for (int i = 0; i < choices.size(); i++) {
            OpenChoicePacket.ClientChoice choice = choices.get(i);
            String textStr = choice.text().getString();
            ButtonSize size = getButtonSize(textStr);
            List<String> lines = splitText(textStr, MAX_CHARS_PER_LINE);

            // Limit lines to max for size
            if (lines.size() > size.lines) {
                lines = lines.subList(0, size.lines);
            }

            choiceButtons.add(new ChoiceButton(
                    buttonX, buttonY, BUTTON_WIDTH, size.height,
                    i, choice.text(), choice.locked(), choice.lockedMessage(),
                    size, lines
            ));
            buttonY += size.height + spacing;
        }
    }

    /**
     * Initialize full mode: question at top-left, choices below as simple text.
     */
    private void initFullMode() {
        choiceButtons = new ArrayList<>();
        int startX = 20;
        int startY = 40 + font.lineHeight + 20; // Below question text
        int buttonHeight = font.lineHeight + 8;
        int spacing = 4;

        for (int i = 0; i < choices.size(); i++) {
            OpenChoicePacket.ClientChoice choice = choices.get(i);
            String textStr = choice.text().getString();
            int textWidth = font.width(textStr) + 30; // Extra for lock icon
            List<String> lines = List.of(textStr); // Single line for full mode
            choiceButtons.add(new ChoiceButton(
                    startX, startY, Math.max(textWidth, 150), buttonHeight,
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
        int panelLeft = (width - PANEL_WIDTH) / 2;

        // Dialog panel background with fixed texture (scaled 2x)
        // blit(texture, x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight)
        RenderSystem.enableBlend();
        graphics.blit(DIALOG_PANEL_TEXTURE, panelLeft, dialogPanelTop, PANEL_WIDTH, PANEL_HEIGHT, 0, 0, PANEL_TEX_WIDTH, PANEL_TEX_HEIGHT, PANEL_TEX_WIDTH, PANEL_TEX_HEIGHT);
        RenderSystem.disableBlend();

        // Speaker name (uses color from Component)
        graphics.drawString(font, speakerName, panelLeft + 32, dialogPanelTop + 11, 0xFFFFFFFF);

        // Dialogue text (below speaker name with small gap)
        int textY = dialogPanelTop + 18 + font.lineHeight + 7;
        for (FormattedCharSequence line : wrappedDialogue) {
            graphics.drawString(font, line, panelLeft + 12, textY, COLOR_WHITE);
            textY += font.lineHeight + 1;
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
        // Select texture based on size and state
        ResourceLocation buttonTexture = getButtonTexture(button.size, button.locked, hovered);

        // Render button background (scaled 2x from texture size)
        // blit(texture, x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight)
        int texHeight = getButtonTextureHeight(button.size);
        RenderSystem.enableBlend();
        graphics.blit(buttonTexture, button.x, button.y, button.width, button.height, 0, 0, BUTTON_TEX_WIDTH, texHeight, BUTTON_TEX_WIDTH, texHeight);
        RenderSystem.disableBlend();

        // Text color
        int textColor = button.locked ? COLOR_GRAY : COLOR_WHITE;

        // Calculate vertical centering for all lines
        int totalTextHeight = button.lines.size() * font.lineHeight;
        int textY = button.y + (button.height - totalTextHeight) / 2;

        // Render each line of text (centered horizontally, no shadow)
        for (String line : button.lines) {
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
     * Get the appropriate texture for button based on size and state.
     */
    private ResourceLocation getButtonTexture(ButtonSize size, boolean locked, boolean hovered) {
        if (size == ButtonSize.SMALL) {
            if (locked) {
                return hovered ? BUTTON_SMALL_LOCKED_HOVER : BUTTON_SMALL_LOCKED;
            }
            return hovered ? BUTTON_SMALL_HOVER : BUTTON_SMALL;
        } else if (size == ButtonSize.MEDIUM) {
            if (locked) {
                return hovered ? BUTTON_MEDIUM_LOCKED_HOVER : BUTTON_MEDIUM_LOCKED;
            }
            return hovered ? BUTTON_MEDIUM_HOVER : BUTTON_MEDIUM;
        } else {
            if (locked) {
                return hovered ? BUTTON_LARGE_LOCKED_HOVER : BUTTON_LARGE_LOCKED;
            }
            return hovered ? BUTTON_LARGE_HOVER : BUTTON_LARGE;
        }
    }

    /**
     * Get the texture height for a button size.
     */
    private int getButtonTextureHeight(ButtonSize size) {
        if (size == ButtonSize.SMALL) {
            return BUTTON_TEX_HEIGHT_SMALL;
        } else if (size == ButtonSize.MEDIUM) {
            return BUTTON_TEX_HEIGHT_MEDIUM;
        } else {
            return BUTTON_TEX_HEIGHT_LARGE;
        }
    }

    /**
     * Render full mode: question at top, choices as simple text below.
     */
    private void renderFullMode(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Question text at top-left
        graphics.drawString(font, dialogueText, 20, 40, COLOR_WHITE);

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

        // Calculate which frame to show (0-11 based on progress)
        float progress = remainingSeconds / timerSeconds;
        int frameIndex = Math.min(11, (int) ((1.0f - progress) * 12));

        // Timer position (top-right corner)
        int timerX = width - TIMER_SIZE - 20;
        int timerY = 20;

        // Render timer texture
        RenderSystem.enableBlend();
        graphics.blit(TIMER_TEXTURES[frameIndex], timerX, timerY, TIMER_SIZE, TIMER_SIZE,
                0, 0, TIMER_TEX_SIZE, TIMER_TEX_SIZE, TIMER_TEX_SIZE, TIMER_TEX_SIZE);
        RenderSystem.disableBlend();

        // Draw seconds number in center (scaled 2x, without shadow)
        String timeText = String.valueOf((int) Math.ceil(remainingSeconds));
        int textWidth = font.width(timeText);
        int textHeight = font.lineHeight;

        // Scale text by 2x
        float scale = 2.0f;
        int scaledTextWidth = (int) (textWidth * scale);
        int scaledTextHeight = (int) (textHeight * scale);

        int textX = timerX + (TIMER_SIZE - scaledTextWidth) / 2;
        int textY = timerY + (TIMER_SIZE - scaledTextHeight) / 2;

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

        // Send selection
        PacketDistributor.sendToServer(new SelectChoicePacket(windowId, selectedIndex, speaker.getId()));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredChoice >= 0) {
            ChoiceButton clicked = choiceButtons.get(hoveredChoice);
            if (!clicked.locked) {
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
        final List<String> lines;

        ChoiceButton(int x, int y, int width, int height, int index,
                     Component text, boolean locked, Component lockedMessage,
                     ButtonSize size, List<String> lines) {
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
}
