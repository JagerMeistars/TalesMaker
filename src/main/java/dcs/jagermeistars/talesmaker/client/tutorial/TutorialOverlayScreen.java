package dcs.jagermeistars.talesmaker.client.tutorial;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class TutorialOverlayScreen extends Screen {

    private final Screen wrapped;

    public TutorialOverlayScreen(Screen wrapped) {
        super(Component.empty());
        this.wrapped = wrapped;
    }

    public Screen getWrapped() {
        return wrapped;
    }

    @Override
    public void init() {
        if (minecraft != null) {
            wrapped.init(minecraft, width, height);
        }
    }

    @Override
    public void tick() {
        if (TutorialClientManager.getCurrentPacket() == null) {
            Minecraft.getInstance().setScreen(wrapped);
            return;
        }
        wrapped.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            wrapped.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        wrapped.render(graphics, mouseX, mouseY, partialTick);
        TutorialOverlayRenderer.render(graphics, this.width, this.height, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.mouseClicked(mouseX, mouseY, button);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && TutorialInputUtil.matchesAdvanceKey(step.advanceKey(), button, true)) {
            TutorialClientManager.advanceStep();
            return true;
        }
        if (step != null && step.allowInteraction()) {
            return wrapped.mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.mouseReleased(mouseX, mouseY, button);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && step.allowInteraction()) {
            return wrapped.mouseReleased(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && step.allowInteraction()) {
            return wrapped.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && step.allowInteraction()) {
            return wrapped.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.keyPressed(keyCode, scanCode, modifiers);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && TutorialInputUtil.matchesAdvanceKey(step.advanceKey(), keyCode, false)) {
            TutorialClientManager.advanceStep();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && TutorialClientManager.isForcedActive()) {
            Minecraft.getInstance().setScreen(new PauseScreen(true));
            return true;
        }

        if (step != null && step.allowInteraction()) {
            return wrapped.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (TutorialClientManager.getCurrentPacket() == null) {
            return wrapped.charTyped(codePoint, modifiers);
        }
        TutorialClientManager.ClientStep step = TutorialClientManager.getCurrentStep();
        if (step != null && step.allowInteraction()) {
            return wrapped.charTyped(codePoint, modifiers);
        }
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
        return wrapped.isPauseScreen();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        wrapped.resize(minecraft, width, height);
    }
}
