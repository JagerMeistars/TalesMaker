package dcs.jagermeistars.talesmaker.data.tutorial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record TutorialPreset(
        ResourceLocation id,
        String type,
        Component title,
        Component description,
        List<TutorialStep> steps,
        TutorialSounds sounds
) {
    public static final String TYPE_FORCED = "forced";
    public static final String TYPE_POPUP = "popup";

    public static final Codec<TutorialPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", TYPE_FORCED).forGetter(TutorialPreset::type),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(TutorialPreset::title),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(TutorialPreset::description),
            TutorialStep.CODEC.listOf().fieldOf("steps").forGetter(TutorialPreset::steps),
            TutorialSounds.CODEC.optionalFieldOf("sounds", TutorialSounds.EMPTY).forGetter(TutorialPreset::sounds)
    ).apply(instance, (type, title, description, steps, sounds) ->
            new TutorialPreset(null, type, title, description, steps, sounds)));

    public TutorialPreset withId(ResourceLocation fullId) {
        return new TutorialPreset(fullId, type, title, description, steps, sounds);
    }

    public boolean isForced() {
        return TYPE_FORCED.equalsIgnoreCase(type);
    }

    public boolean isPopup() {
        return TYPE_POPUP.equalsIgnoreCase(type);
    }
}
