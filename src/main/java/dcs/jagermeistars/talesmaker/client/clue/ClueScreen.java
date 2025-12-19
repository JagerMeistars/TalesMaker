package dcs.jagermeistars.talesmaker.client.clue;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.network.DiscoverCluePacket;
import dcs.jagermeistars.talesmaker.network.OpenCluePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Screen for inspecting clue items and finding hidden clues.
 * UI style inspired by Tomb Raider.
 *
 * Layout:
 * - Left side: 3D model with rotation/zoom
 * - Right side: Info panel with name, belonging, description, and clue list
 */
public class ClueScreen extends Screen {

    // Colors
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFF888888;
    private static final int COLOR_DARK_GRAY = 0xFF444444;
    private static final int COLOR_PANEL_BG = 0xDD000000;
    private static final int COLOR_DIVIDER = 0xFF555555;
    private static final int COLOR_GREEN = 0xFF44AA44;

    // Panel dimensions
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_PADDING = 15;

    // Model rendering
    private static final float MODEL_SCALE = 50.0f;
    private static final float ROTATION_SPEED = 0.5f;
    private static final float ZOOM_SPEED = 0.1f;

    // Cursor texture
    private static final ResourceLocation CURSOR_MAGNIFIER =
            ResourceLocation.fromNamespaceAndPath(TalesMaker.MODID, "textures/gui/clue/cursor_magnifier.png");
    private static final int CURSOR_SIZE = 32;

    // Data
    private final ResourceLocation presetId;
    private final Component itemName;
    private final Component belonging; // Can be null
    private final Component description;
    private final ResourceLocation modelPath;
    private final ResourceLocation texturePath;
    private final ResourceLocation soundPath;
    private final List<OpenCluePacket.ClientClueData> clues;
    private final String onComplete; // Can be null

    // State
    private final Set<String> discoveredBones = new HashSet<>();
    private ClueRenderEntity renderEntity;
    private ClueModelRenderer modelRenderer;
    private List<ClueModelParser.BoneData> hotspots;

    // Camera state
    private float rotationX = 15;
    private float rotationY = 0;
    private float zoom = 1.0f;
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;

    // Hover state
    private String hoveredBone = null;

    // Animation
    private String lastDiscoveredBone = null;
    private long discoveryAnimTime = 0;
    private static final int DISCOVERY_ANIM_DURATION = 500; // ms

    // Layout
    private int modelCenterX;
    private int modelCenterY;
    private int panelX;
    private int panelY;
    private int panelHeight;

    public ClueScreen(ResourceLocation presetId, Component itemName, Component belonging,
                      Component description, ResourceLocation modelPath, ResourceLocation texturePath,
                      ResourceLocation soundPath, List<OpenCluePacket.ClientClueData> clues,
                      String onComplete) {
        super(itemName);
        this.presetId = presetId;
        this.itemName = itemName;
        this.belonging = belonging;
        this.description = description;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.soundPath = soundPath;
        this.clues = clues;
        this.onComplete = onComplete;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        // Create render entity and renderer
        renderEntity = new ClueRenderEntity(modelPath, texturePath);
        modelRenderer = new ClueModelRenderer();

        // Parse model for hotspots
        List<String> boneNames = clues.stream()
                .map(OpenCluePacket.ClientClueData::bone)
                .toList();
        hotspots = ClueModelParser.getBonesData(modelPath, boneNames);

        // Calculate layout
        panelX = width - PANEL_WIDTH - 20;
        panelY = 20;
        panelHeight = height - 40;

        // Model center is in the left area
        modelCenterX = (panelX - 20) / 2;
        modelCenterY = height / 2;

        // Set screen params for ray-picking
        modelRenderer.setScreenParams(width, height, modelCenterX, modelCenterY, MODEL_SCALE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        graphics.fill(0, 0, width, height, 0xCC000000);

        // Update hover state
        updateHoverState(mouseX, mouseY);

        // Render 3D model
        renderModel(graphics, partialTick);

        // Render info panel
        renderInfoPanel(graphics);

        // Render custom cursor if hovering over hotspot
        if (hoveredBone != null && !discoveredBones.contains(hoveredBone)) {
            renderMagnifierCursor(graphics, mouseX, mouseY);
        }

        // Render hint at bottom
        renderHint(graphics);
    }

    private void updateHoverState(int mouseX, int mouseY) {
        modelRenderer.setRotation(rotationX, rotationY);
        modelRenderer.setZoom(zoom);

        // Only pick undiscovered hotspots
        List<ClueModelParser.BoneData> undiscoveredHotspots = hotspots.stream()
                .filter(h -> !discoveredBones.contains(h.name()))
                .toList();

        hoveredBone = modelRenderer.pickHotspot(mouseX, mouseY, undiscoveredHotspots).orElse(null);
    }

    private void renderModel(GuiGraphics graphics, float partialTick) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Move to model center
        poseStack.translate(modelCenterX, modelCenterY, 100);

        // Setup lighting
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Get buffer source
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        // Render the model
        modelRenderer.setRotation(rotationX, rotationY);
        modelRenderer.setZoom(zoom);
        modelRenderer.render(renderEntity, poseStack, bufferSource, 15728880);

        bufferSource.endBatch();

        poseStack.popPose();
    }

    private void renderInfoPanel(GuiGraphics graphics) {
        // Panel background
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, COLOR_PANEL_BG);

        int y = panelY + PANEL_PADDING;
        int textX = panelX + PANEL_PADDING;
        int textWidth = PANEL_WIDTH - PANEL_PADDING * 2;

        // Item name (large)
        graphics.drawString(font, itemName, textX, y, COLOR_WHITE);
        y += font.lineHeight + 4;

        // Belonging (if present)
        if (belonging != null) {
            graphics.drawString(font, belonging, textX, y, COLOR_GRAY);
            y += font.lineHeight + 8;
        } else {
            y += 4;
        }

        // Divider
        graphics.fill(textX, y, panelX + PANEL_WIDTH - PANEL_PADDING, y + 1, COLOR_DIVIDER);
        y += 10;

        // Description (wrapped)
        List<FormattedCharSequence> descLines = font.split(description, textWidth);
        for (FormattedCharSequence line : descLines) {
            graphics.drawString(font, line, textX, y, COLOR_WHITE);
            y += font.lineHeight + 1;
        }
        y += 15;

        // Double divider
        graphics.fill(textX, y, panelX + PANEL_WIDTH - PANEL_PADDING, y + 2, COLOR_DIVIDER);
        y += 15;

        // Clue list
        for (OpenCluePacket.ClientClueData clue : clues) {
            boolean discovered = discoveredBones.contains(clue.bone());

            // Checkbox
            String checkbox = discovered ? "☑ " : "☐ ";
            int checkColor = discovered ? COLOR_GREEN : COLOR_DARK_GRAY;
            graphics.drawString(font, checkbox, textX, y, checkColor, false);

            // Clue name or ???
            int nameX = textX + font.width(checkbox);
            if (discovered) {
                graphics.drawString(font, clue.name(), nameX, y, COLOR_WHITE);
                y += font.lineHeight + 2;

                // Description (smaller, wrapped)
                List<FormattedCharSequence> clueDescLines = font.split(clue.description(), textWidth - 20);
                for (FormattedCharSequence line : clueDescLines) {
                    graphics.drawString(font, line, textX + 15, y, COLOR_GRAY);
                    y += font.lineHeight;
                }
                y += 8;
            } else {
                graphics.drawString(font, "???", nameX, y, COLOR_DARK_GRAY);
                y += font.lineHeight + 10;
            }
        }

        // Counter at bottom
        int found = discoveredBones.size();
        int total = clues.size();
        String counter = "Найдено: " + found + "/" + total;
        int counterY = panelY + panelHeight - PANEL_PADDING - font.lineHeight;

        graphics.fill(textX, counterY - 5, panelX + PANEL_WIDTH - PANEL_PADDING, counterY - 4, COLOR_DIVIDER);
        graphics.drawString(font, counter, textX, counterY, found == total ? COLOR_GREEN : COLOR_WHITE);
    }

    private void renderMagnifierCursor(GuiGraphics graphics, int mouseX, int mouseY) {
        // Hide default cursor when showing magnifier
        // Draw magnifier cursor centered on mouse
        RenderSystem.enableBlend();
        graphics.blit(CURSOR_MAGNIFIER, mouseX - CURSOR_SIZE / 2, mouseY - CURSOR_SIZE / 2,
                CURSOR_SIZE, CURSOR_SIZE, 0, 0, 32, 32, 32, 32);
        RenderSystem.disableBlend();
    }

    private void renderHint(GuiGraphics graphics) {
        String hint;
        if (discoveredBones.size() < clues.size()) {
            hint = "Осмотрите предмет чтобы найти все улики";
        } else {
            hint = "Нажмите ESC чтобы закрыть";
        }

        int hintWidth = font.width(hint);
        int hintX = (width - PANEL_WIDTH - 20) / 2 - hintWidth / 2;
        int hintY = height - 30;

        graphics.drawString(font, hint, hintX, hintY, COLOR_GRAY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Left click - check for hotspot or start drag
            if (hoveredBone != null && !discoveredBones.contains(hoveredBone)) {
                discoverClue(hoveredBone);
                return true;
            }

            // Start rotation drag
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            rotationY += (float) (mouseX - lastMouseX) * ROTATION_SPEED;
            rotationX += (float) (mouseY - lastMouseY) * ROTATION_SPEED;

            // Clamp X rotation
            rotationX = Math.max(-80, Math.min(80, rotationX));

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        zoom += (float) scrollY * ZOOM_SPEED;
        zoom = Math.max(0.5f, Math.min(3.0f, zoom));
        modelRenderer.setZoom(zoom);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (discoveredBones.size() < clues.size()) {
                // Show message that all clues must be found
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("Найдите все улики прежде чем закрыть осмотр!").withStyle(style -> style.withColor(0xFF5555)),
                            true
                    );
                }
                return true; // Block ESC
            }
            // All clues found, allow close
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void discoverClue(String boneId) {
        if (discoveredBones.contains(boneId)) {
            return;
        }

        discoveredBones.add(boneId);
        lastDiscoveredBone = boneId;
        discoveryAnimTime = System.currentTimeMillis();

        // Play discovery sound
        playDiscoverySound();

        // Send packet to server
        PacketDistributor.sendToServer(new DiscoverCluePacket(presetId, boneId));
    }

    private void playDiscoverySound() {
        if (minecraft.player != null && soundPath != null) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundPath);
            minecraft.player.playSound(soundEvent, 1.0f, 1.0f);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // We handle ESC manually
    }
}
