package dcs.jagermeistars.talesmaker.client.dialogue;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DialogueManager {

    private static Dialogue currentDialogue = null;
    private static long dialogueStartTime = 0;
    private static int durationTicks = 60; // Default 3 seconds

    public record Dialogue(
            Component npcName,
            ResourceLocation icon,
            Component message
    ) {}

    public static void showDialogue(Component npcName, ResourceLocation icon, Component message) {
        currentDialogue = new Dialogue(npcName, icon, message);
        dialogueStartTime = System.currentTimeMillis();
        DialogueHistory.addEntry(npcName, icon, message);
    }

    public static void setDuration(int ticks) {
        durationTicks = ticks;
    }

    public static int getDuration() {
        return durationTicks;
    }

    public static Dialogue getCurrentDialogue() {
        if (currentDialogue == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - dialogueStartTime;
        long durationMs = (durationTicks * 1000L) / 20L;

        if (elapsed > durationMs) {
            currentDialogue = null;
            return null;
        }

        return currentDialogue;
    }

    public static long getDialogueStartTime() {
        return dialogueStartTime;
    }

    public static float getProgress() {
        if (currentDialogue == null) {
            return 0f;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - dialogueStartTime;
        long durationMs = (durationTicks * 1000L) / 20L;

        return Math.min(1f, (float) elapsed / durationMs);
    }

    public static void clear() {
        currentDialogue = null;
    }
}
