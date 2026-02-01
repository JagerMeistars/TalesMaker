package dcs.jagermeistars.talesmaker.client.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class TutorialScreen extends Screen {

    private final net.minecraft.resources.ResourceLocation tutorialId;

    public TutorialScreen(net.minecraft.resources.ResourceLocation tutorialId) {
        super(Component.empty());
        this.tutorialId = tutorialId;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TutorialOverlayRenderer.render(graphics, this.width, this.height, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && matchesAdvanceKey(step.advanceKey(), button, true)) {
            TutorialClientManager.advanceStep();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (TutorialClientManager.isForcedActive()) {
                Minecraft.getInstance().setScreen(new PauseScreen(true));
            } else {
                onClose();
            }
            return true;
        }

        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && matchesAdvanceKey(step.advanceKey(), keyCode, false)) {
            TutorialClientManager.advanceStep();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return true;
    }

    @Override
    public void onClose() {
        if (!TutorialClientManager.isForcedActive()) {
            TutorialClientManager.completeTutorial();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean matchesAdvanceKey(String keyName, int code, boolean mouse) {
        return TutorialInputUtil.matchesAdvanceKey(keyName, code, mouse);
    }
}
