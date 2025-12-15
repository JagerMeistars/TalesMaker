package dcs.jagermeistars.talesmaker.data.choice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Represents a choice window configuration loaded from JSON.
 *
 * JSON format:
 * {
 *   "text": {"text": "Question text", "color": "white"},
 *   "mode": "dialog",
 *   "timer": 10,
 *   "timeout_choice": -1,
 *   "choices": [
 *     {"text": {...}, "command": "...", "hidden": {...}, "locked": {...}}
 *   ]
 * }
 */
public record ChoiceWindow(
        ResourceLocation id,
        Component text,
        List<Choice> choices,
        String mode,
        int timer,
        int timeoutChoice
) {
    /** Display mode: dialog panel at bottom with choices above */
    public static final String MODE_DIALOG = "dialog";
    /** Display mode: full screen with question and choices as simple text */
    public static final String MODE_FULL = "full";

    public static final Codec<ChoiceWindow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(ChoiceWindow::text),
            Choice.CODEC.listOf().fieldOf("choices").forGetter(ChoiceWindow::choices),
            Codec.STRING.optionalFieldOf("mode", MODE_DIALOG).forGetter(ChoiceWindow::mode),
            Codec.INT.optionalFieldOf("timer", 0).forGetter(ChoiceWindow::timer),
            Codec.INT.optionalFieldOf("timeout_choice", -1).forGetter(ChoiceWindow::timeoutChoice)
    ).apply(instance, (text, choices, mode, timer, timeoutChoice) ->
            new ChoiceWindow(null, text, choices, mode, timer, timeoutChoice)));

    /**
     * Create a copy with the full ResourceLocation ID set.
     */
    public ChoiceWindow withId(ResourceLocation fullId) {
        return new ChoiceWindow(fullId, text, choices, mode, timer, timeoutChoice);
    }

    /**
     * Check if this window uses dialog mode.
     */
    public boolean isDialogMode() {
        return MODE_DIALOG.equals(mode);
    }

    /**
     * Check if this window uses full mode.
     */
    public boolean isFullMode() {
        return MODE_FULL.equals(mode);
    }

    /**
     * Check if this window has a timer.
     */
    public boolean hasTimer() {
        return timer > 0;
    }
}
