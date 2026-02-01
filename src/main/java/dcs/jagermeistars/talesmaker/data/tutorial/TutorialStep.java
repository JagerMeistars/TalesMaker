package dcs.jagermeistars.talesmaker.data.tutorial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record TutorialStep(
        Component text,
        TutorialCutout cutout,
        String advanceKey,
        String openScreen,
        String command,
        boolean allowInteraction
) {
    public static final String DEFAULT_ADVANCE_KEY = "key.mouse.right";

    public static final Codec<TutorialStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(TutorialStep::text),
            TutorialCutout.CODEC.optionalFieldOf("cutout", TutorialCutout.DEFAULT).forGetter(TutorialStep::cutout),
            Codec.STRING.optionalFieldOf("advance_key", DEFAULT_ADVANCE_KEY).forGetter(TutorialStep::advanceKey),
            Codec.STRING.optionalFieldOf("open_screen", "").forGetter(TutorialStep::openScreen),
            Codec.STRING.optionalFieldOf("command", "").forGetter(TutorialStep::command),
            Codec.BOOL.optionalFieldOf("allow_interaction", false).forGetter(TutorialStep::allowInteraction)
    ).apply(instance, TutorialStep::new));
}
