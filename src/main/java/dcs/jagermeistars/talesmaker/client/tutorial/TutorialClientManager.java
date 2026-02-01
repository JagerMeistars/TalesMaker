package dcs.jagermeistars.talesmaker.client.tutorial;

import dcs.jagermeistars.talesmaker.TalesMaker;
import dcs.jagermeistars.talesmaker.network.TutorialStartPacket;
import dcs.jagermeistars.talesmaker.network.TutorialStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class TutorialClientManager {

    public record ClientStep(
            Component text,
            float cutoutX,
            float cutoutY,
            float cutoutWidth,
            float cutoutHeight,
            String advanceKey,
            String openScreen,
            String command,
            boolean allowInteraction
    ) {}

    private static TutorialStartPacket currentPacket;
    private static List<ClientStep> steps = new ArrayList<>();
    private static int stepIndex = 0;
    private static boolean forcedActive = false;
    private static java.util.Optional<net.minecraft.resources.ResourceLocation> soundOpen = java.util.Optional.empty();
    private static java.util.Optional<net.minecraft.resources.ResourceLocation> soundClose = java.util.Optional.empty();
    private static java.util.Optional<net.minecraft.resources.ResourceLocation> soundAdvance = java.util.Optional.empty();

    private TutorialClientManager() {}

    public static void startTutorial(TutorialStartPacket packet) {
        currentPacket = packet;
        steps = new ArrayList<>();
        for (TutorialStartPacket.ClientStep step : packet.steps()) {
            steps.add(new ClientStep(step.text(), step.cutoutX(), step.cutoutY(),
                    step.cutoutWidth(), step.cutoutHeight(), step.advanceKey(),
                    step.openScreen(), step.command(), step.allowInteraction()));
        }
        stepIndex = 0;
        forcedActive = "forced".equalsIgnoreCase(packet.tutorialType());
        soundOpen = packet.soundOpen();
        soundClose = packet.soundClose();
        soundAdvance = packet.soundAdvance();

        Minecraft mc = Minecraft.getInstance();
        if (packet.steps().isEmpty()) {
            TalesMaker.LOGGER.warn("Tutorial {} has no steps", packet.tutorialId());
            return;
        }

        playSound(soundOpen);
        applyStepActions();

        if ("popup".equalsIgnoreCase(packet.tutorialType())) {
            mc.setScreen(new TutorialPromptScreen(packet.title(), packet.description()));
        } else {
            mc.setScreen(new TutorialScreen(packet.tutorialId()));
        }
    }

    public static boolean isForcedActive() {
        return forcedActive;
    }

    public static TutorialStartPacket getCurrentPacket() {
        return currentPacket;
    }

    public static ClientStep getCurrentStep() {
        if (steps.isEmpty()) {
            return null;
        }
        return steps.get(Math.max(0, Math.min(stepIndex, steps.size() - 1)));
    }

    public static int getStepIndex() {
        return stepIndex;
    }

    public static int getStepCount() {
        return steps.size();
    }

    public static void openTutorial() {
        if (currentPacket == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new TutorialScreen(currentPacket.tutorialId()));
    }

    public static void advanceStep() {
        if (steps.isEmpty()) {
            return;
        }
        playSound(soundAdvance);
        stepIndex++;
        if (stepIndex >= steps.size()) {
            completeTutorial();
        } else {
            applyStepActions();
        }
    }

    public static void completeTutorial() {
        Minecraft mc = Minecraft.getInstance();
        if (forcedActive && currentPacket != null) {
            PacketDistributor.sendToServer(new TutorialStatePacket(currentPacket.tutorialId(),
                    TutorialStatePacket.ACTION_COMPLETE));
        }
        sendAllowInteraction(false);
        playSound(soundClose);
        clearLocalState(mc);
    }

    public static void dismissPrompt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TutorialPromptScreen) {
            playSound(soundClose);
            mc.setScreen(null);
        }
    }

    public static void resetClientState() {
        clearLocalState(Minecraft.getInstance());
    }

    private static void clearLocalState(Minecraft mc) {
        forcedActive = false;
        currentPacket = null;
        steps = new ArrayList<>();
        stepIndex = 0;
        soundOpen = java.util.Optional.empty();
        soundClose = java.util.Optional.empty();
        soundAdvance = java.util.Optional.empty();
        if (mc.screen instanceof TutorialOverlayScreen overlay) {
            mc.setScreen(overlay.getWrapped());
            return;
        }
        if (mc.screen instanceof TutorialScreen || mc.screen instanceof TutorialPromptScreen) {
            mc.setScreen(null);
        }
    }

    private static void applyStepActions() {
        ClientStep step = getCurrentStep();
        if (step == null) {
            return;
        }
        sendAllowInteraction(step.allowInteraction());
        runCommand(step.command());
        openScreen(step.openScreen());
    }

    private static void sendAllowInteraction(boolean allow) {
        if (currentPacket == null) {
            return;
        }
        PacketDistributor.sendToServer(new dcs.jagermeistars.talesmaker.network.TutorialStepStatePacket(
                currentPacket.tutorialId(), allow));
    }

    private static void runCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        if (!normalized.isEmpty()) {
            mc.player.connection.sendCommand(normalized);
        }
    }

    private static void openScreen(String screenName) {
        if (screenName == null || screenName.isBlank()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        String normalized = screenName.trim().toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "inventory" -> {
                if (mc.player != null) {
                    mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                }
            }
            case "chat" -> mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(""));
            case "pause" -> mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            case "history" -> mc.setScreen(new dcs.jagermeistars.talesmaker.client.dialogue.DialogueHistoryScreen());
            default -> {
                // Unknown screen name - ignore
            }
        }
    }

    private static void playSound(java.util.Optional<net.minecraft.resources.ResourceLocation> soundId) {
        if (soundId == null || soundId.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        net.minecraft.sounds.SoundEvent soundEvent =
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(soundId.get());
        mc.player.playSound(soundEvent, 1.0f, 1.0f);
    }
}
