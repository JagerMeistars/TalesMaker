package dcs.jagermeistars.talesmaker.data.tutorial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record TutorialSounds(
        Optional<ResourceLocation> open,
        Optional<ResourceLocation> close,
        Optional<ResourceLocation> advance
) {
    public static final TutorialSounds EMPTY = new TutorialSounds(Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<TutorialSounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("open").forGetter(TutorialSounds::open),
            ResourceLocation.CODEC.optionalFieldOf("close").forGetter(TutorialSounds::close),
            ResourceLocation.CODEC.optionalFieldOf("advance").forGetter(TutorialSounds::advance)
    ).apply(instance, TutorialSounds::new));
}
