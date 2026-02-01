package dcs.jagermeistars.talesmaker.data.tutorial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TutorialCutout(
        float x,
        float y,
        float width,
        float height
) {
    public static final TutorialCutout DEFAULT = new TutorialCutout(0.0f, 0.0f, 1.0f, 1.0f);

    public static final Codec<TutorialCutout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("x", 0.0f).forGetter(TutorialCutout::x),
            Codec.FLOAT.optionalFieldOf("y", 0.0f).forGetter(TutorialCutout::y),
            Codec.FLOAT.optionalFieldOf("width", 1.0f).forGetter(TutorialCutout::width),
            Codec.FLOAT.optionalFieldOf("height", 1.0f).forGetter(TutorialCutout::height)
    ).apply(instance, TutorialCutout::new));
}
