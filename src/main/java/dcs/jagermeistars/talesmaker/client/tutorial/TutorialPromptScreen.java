package dcs.jagermeistars.talesmaker.client.tutorial;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TutorialPromptScreen extends Screen {

    private final Component titleText;
    private final Component descriptionText;
    public TutorialPromptScreen(Component title, Component description) {
        super(Component.empty());
        this.titleText = title;
        this.descriptionText = description;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int baseY = this.height / 2 + 20;

        addRenderableWidget(Button.builder(Component.literal("Start"), button -> {
            TutorialClientManager.openTutorial();
        }).bounds(centerX - 70, baseY, 140, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
            TutorialClientManager.dismissPrompt();
        }).bounds(centerX - 70, baseY + 24, 140, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        int centerX = this.width / 2;
        int y = this.height / 2 - 60;

        graphics.drawCenteredString(this.font, titleText, centerX, y, 0xFFFFFF);
        y += 18;

        List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(descriptionText, Math.max(200, this.width / 2));
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            graphics.drawCenteredString(this.font, line, centerX, y, 0xCCCCCC);
            y += this.font.lineHeight + 2;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            TutorialClientManager.dismissPrompt();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
