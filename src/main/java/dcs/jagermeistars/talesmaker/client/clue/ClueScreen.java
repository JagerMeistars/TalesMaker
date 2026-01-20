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
import net.minecraft.world.phys.Vec3;
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
 * - Left side: 3D model with rotation
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

    // Panel dimensions (base values)
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_PADDING = 15;
    private static final int BASE_MARGIN = 20;

    // Model rendering
    private static final float MODEL_SCALE = 50.0f;
    private static final float ROTATION_SPEED = 0.5f;
    private static final float INERTIA_DAMPING = 0.9f;

    // Data
    private final ResourceLocation presetId;
    private final Component itemName;
    private final Component belonging; // Can be null
    private final Component description;
    private final ResourceLocation modelPath;
    private final ResourceLocation texturePath;
    private final ResourceLocation soundPath;
    private final Float modelScaleOverride;
    private final List<OpenCluePacket.ClientClueData> clues;
    private final String onComplete; // Can be null

    // State
    private final Set<String> discoveredBones = new HashSet<>();
    private ClueModelRenderer modelRenderer;
    private ClueModelParser.ModelData modelData;
    private List<ClueModelParser.CubeData> hotspotCubes;

    // Camera state
    private float rotationX = 15;
    private float rotationY = 0;
    private boolean isDragging = false;
    private double lastMouseX, lastMouseY;
    private float rotationVelocityX = 0.0f;
    private float rotationVelocityY = 0.0f;
    private long lastFrameTime = System.currentTimeMillis();
    private long pointerCursor = 0L;
    private boolean cursorActive = false;

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
    private int panelWidth;
    private int panelPadding;
    private int uiMargin;
    private float uiScale = 1.0f;
    private float modelScale = MODEL_SCALE;

    public ClueScreen(ResourceLocation presetId, Component itemName, Component belonging,
                      Component description, ResourceLocation modelPath, ResourceLocation texturePath,
                      ResourceLocation soundPath, Float modelScaleOverride,
                      List<OpenCluePacket.ClientClueData> clues,
                      String onComplete) {
        super(itemName);
        this.presetId = presetId;
        this.itemName = itemName;
        this.belonging = belonging;
        this.description = description;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.soundPath = soundPath;
        this.modelScaleOverride = modelScaleOverride;
        this.clues = clues;
        this.onComplete = onComplete;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void init() {
        super.init();

        // Create renderer and model data
        modelRenderer = new ClueModelRenderer();
        modelData = ClueModelParser.getModelData(modelPath);

        // Parse model for hotspots
        List<String> boneNames = clues.stream()
                .map(OpenCluePacket.ClientClueData::bone)
                .toList();
        hotspotCubes = ClueModelParser.getCubes(modelPath, boneNames);

        Set<String> hotspotNames = new HashSet<>();
        for (ClueModelParser.CubeData cube : hotspotCubes) {
            hotspotNames.add(cube.name());
        }
        for (OpenCluePacket.ClientClueData clue : clues) {
            if (!hotspotNames.contains(clue.bone())) {
                discoverClue(clue.bone(), false);
            }
        }

        // Calculate layout
        uiScale = calculateUiScale();
        uiMargin = scaleInt(BASE_MARGIN);
        int maxPanelWidth = Math.max(1, width - uiMargin * 2);
        int minPanelWidth = Math.min(scaleInt(180), maxPanelWidth);
        panelWidth = Math.max(minPanelWidth, Math.min(scaleInt(PANEL_WIDTH), maxPanelWidth));
        panelPadding = scaleInt(PANEL_PADDING);
        panelX = width - panelWidth - uiMargin;
        panelY = uiMargin;
        panelHeight = Math.max(scaleInt(120), height - uiMargin * 2);

        // Model center is in the left area
        modelCenterX = (panelX - uiMargin) / 2;
        modelCenterY = height / 2;

        modelScale = modelScaleOverride == null ? calculateModelScale() : modelScaleOverride;

        // Set screen params for ray-picking
        modelRenderer.setScreenParams(width, height, modelCenterX, modelCenterY, modelScale);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        graphics.fill(0, 0, width, height, 0xCC000000);

        updateInertia();

        // Update hover state
        updateHoverState(mouseX, mouseY);

        // Render 3D model
        renderModel(graphics, partialTick);

        // Render info panel
        renderInfoPanel(graphics);

        updateCursor();

        // Render hint at bottom
        renderHint(graphics);
    }

    private void updateHoverState(int mouseX, int mouseY) {
        modelRenderer.setRotation(rotationX, rotationY);

        // Only pick undiscovered hotspots
        List<ClueModelParser.CubeData> undiscoveredHotspots = hotspotCubes.stream()
                .filter(h -> !discoveredBones.contains(h.name()))
                .toList();

        List<ClueModelParser.CubeData> allCubes = modelData == null ? List.of() : modelData.cubes();
        hoveredBone = modelRenderer.pickHotspot(mouseX, mouseY, allCubes, undiscoveredHotspots).orElse(null);
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

        RenderSystem.enableDepthTest();

        // Render the model
        modelRenderer.setRotation(rotationX, rotationY);
        modelRenderer.render(modelData, texturePath, poseStack, bufferSource, 15728880);

        bufferSource.endBatch();

        poseStack.popPose();

        RenderSystem.disableDepthTest();
    }

    private float calculateModelScale() {
        if (modelData == null || modelData.cubes().isEmpty()) {
            return MODEL_SCALE / 16.0f;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (ClueModelParser.CubeData cube : modelData.cubes()) {
            Vec3 from = cube.from();
            Vec3 to = cube.to();

            minX = Math.min(minX, Math.min(from.x, to.x));
            minY = Math.min(minY, Math.min(from.y, to.y));
            minZ = Math.min(minZ, Math.min(from.z, to.z));
            maxX = Math.max(maxX, Math.max(from.x, to.x));
            maxY = Math.max(maxY, Math.max(from.y, to.y));
            maxZ = Math.max(maxZ, Math.max(from.z, to.z));
        }

        double sizeX = maxX - minX;
        double sizeY = maxY - minY;
        double sizeZ = maxZ - minZ;
        double maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));

        if (maxDim <= 0.001) {
            return MODEL_SCALE / 16.0f;
        }

        float maxRadius = Math.min(modelCenterX - scaleInt(30), modelCenterY - scaleInt(30));
        if (maxRadius <= 0) {
            return MODEL_SCALE / 16.0f;
        }

        return (float) (maxRadius / maxDim);
    }

    private void renderInfoPanel(GuiGraphics graphics) {
        // Panel background
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);

        int y = panelY + panelPadding;
        int textX = panelX + panelPadding;
        int textWidth = panelWidth - panelPadding * 2;

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
        graphics.fill(textX, y, panelX + panelWidth - panelPadding, y + 1, COLOR_DIVIDER);
        y += 10;

        // Description (wrapped)
        List<FormattedCharSequence> descLines = font.split(description, textWidth);
        for (FormattedCharSequence line : descLines) {
            graphics.drawString(font, line, textX, y, COLOR_WHITE);
            y += font.lineHeight + 1;
        }
        y += 15;

        // Double divider
        graphics.fill(textX, y, panelX + panelWidth - panelPadding, y + 2, COLOR_DIVIDER);
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
        int counterY = panelY + panelHeight - panelPadding - font.lineHeight;

        graphics.fill(textX, counterY - 5, panelX + panelWidth - panelPadding, counterY - 4, COLOR_DIVIDER);
        graphics.drawString(font, counter, textX, counterY, found == total ? COLOR_GREEN : COLOR_WHITE);
    }

    private void updateCursor() {
        long window = minecraft.getWindow().getWindow();
        boolean shouldShowPointer = hoveredBone != null && !discoveredBones.contains(hoveredBone);

        if (shouldShowPointer && !cursorActive) {
            if (pointerCursor == 0L) {
                pointerCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
            }
            GLFW.glfwSetCursor(window, pointerCursor);
            cursorActive = true;
        } else if (!shouldShowPointer && cursorActive) {
            GLFW.glfwSetCursor(window, 0L);
            cursorActive = false;
        }
    }

    private void renderHint(GuiGraphics graphics) {
        String hint;
        if (discoveredBones.size() < clues.size()) {
            hint = "Осмотрите предмет чтобы найти все улики";
        } else {
            hint = "Нажмите ESC чтобы закрыть";
        }

        int hintWidth = font.width(hint);
        int leftAreaWidth = panelX - uiMargin;
        int hintX = leftAreaWidth / 2 - hintWidth / 2;
        int hintY = height - scaleInt(30);

        graphics.drawString(font, hint, hintX, hintY, COLOR_GRAY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            // Right click - check for hotspot
            if (hoveredBone != null && !discoveredBones.contains(hoveredBone)) {
                discoverClue(hoveredBone, true);
                return true;
            }
            return true;
        }

        if (button == 0) {
            // Left click - start rotation drag
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
            lastFrameTime = System.currentTimeMillis();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            float deltaX = (float) (mouseX - lastMouseX);
            float deltaY = (float) (mouseY - lastMouseY);
            rotationY += deltaX * ROTATION_SPEED;
            rotationX += deltaY * ROTATION_SPEED;

            rotationVelocityY = deltaX * ROTATION_SPEED;
            rotationVelocityX = deltaY * ROTATION_SPEED;

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
        return false;
    }

    private void updateInertia() {
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (isDragging || dt <= 0) {
            return;
        }

        rotationY += rotationVelocityY * dt * 60.0f;
        rotationX += rotationVelocityX * dt * 60.0f;
        rotationX = Math.max(-80, Math.min(80, rotationX));

        float decay = (float) Math.pow(INERTIA_DAMPING, dt * 60.0f);
        rotationVelocityX *= decay;
        rotationVelocityY *= decay;

        if (Math.abs(rotationVelocityX) < 0.01f) {
            rotationVelocityX = 0.0f;
        }
        if (Math.abs(rotationVelocityY) < 0.01f) {
            rotationVelocityY = 0.0f;
        }
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

    private void discoverClue(String boneId, boolean triggerCommand) {
        if (discoveredBones.contains(boneId)) {
            return;
        }

        discoveredBones.add(boneId);
        if (triggerCommand) {
            lastDiscoveredBone = boneId;
            discoveryAnimTime = System.currentTimeMillis();
            playDiscoverySound();
        }

        // Send packet to server
        PacketDistributor.sendToServer(new DiscoverCluePacket(presetId, boneId, triggerCommand));
    }

    private void renderDebugHotspots(GuiGraphics graphics) {
        if (hotspotCubes == null || hotspotCubes.isEmpty()) {
            return;
        }

        for (ClueModelParser.CubeData cube : hotspotCubes) {
            List<float[]> hull = modelRenderer.getCubeScreenHull(cube);
            if (hull.isEmpty()) {
                continue;
            }

            int color = 0x66FFFFFF;
            if (discoveredBones.contains(cube.name())) {
                color = 0x6600FF00;
            } else if (cube.name().equals(hoveredBone)) {
                color = 0x66FF00FF;
            }

            for (int i = 0; i < hull.size(); i++) {
                float[] a = hull.get(i);
                float[] b = hull.get((i + 1) % hull.size());
                int x0 = Math.round(a[0]);
                int y0 = Math.round(a[1]);
                int x1 = Math.round(b[0]);
                int y1 = Math.round(b[1]);
                drawLine(graphics, x0, y0, x1, y1, color);
            }
        }
    }

    private void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private void playDiscoverySound() {
        if (minecraft.player != null && soundPath != null) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundPath);
            minecraft.player.playSound(soundEvent, 1.0f, 1.0f);
        }
    }

    @Override
    public void onClose() {
        if (cursorActive) {
            GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), 0L);
            cursorActive = false;
        }
        if (pointerCursor != 0L) {
            GLFW.glfwDestroyCursor(pointerCursor);
            pointerCursor = 0L;
        }
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
