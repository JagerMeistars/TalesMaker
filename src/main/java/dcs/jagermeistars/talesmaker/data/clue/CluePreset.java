package dcs.jagermeistars.talesmaker.data.clue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Represents a clue inspection preset loaded from JSON.
 *
 * JSON format:
 * {
 *   "name": {"text": "Item Name", "color": "red"},
 *   "belonging": {"text": "Faction/Character", "color": "gray"},
 *   "description": {"text": "Item description..."},
 *   "model": "namespace:geo/clue/item.geo.json",
 *   "texture": "namespace:textures/clue/item.png",
 *   "sound": "minecraft:entity.experience_orb.pickup",
 *   "clues": [
 *     {
 *       "name": {"text": "Clue Name"},
 *       "bone": "bone_name",
 *       "description": {"text": "Clue description"},
 *       "command": "say Found clue!"
 *     }
 *   ],
 *   "on_complete": "say All clues found!"
 * }
 */
public record CluePreset(
        ResourceLocation id,
        Component name,
        Optional<Component> belonging,
        Component description,
        ResourceLocation model,
        ResourceLocation texture,
        Optional<ResourceLocation> sound,
        List<ClueData> clues,
        Optional<String> onComplete
) {
    /** Default sound for clue discovery */
    public static final ResourceLocation DEFAULT_SOUND =
            ResourceLocation.withDefaultNamespace("entity.experience_orb.pickup");

    /**
     * Represents a single clue on the item.
     */
    public record ClueData(
            Component name,
            String bone,
            Component description,
            Optional<String> command
    ) {
        public static final Codec<ClueData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.fieldOf("name").forGetter(ClueData::name),
                Codec.STRING.fieldOf("bone").forGetter(ClueData::bone),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(ClueData::description),
                Codec.STRING.optionalFieldOf("command").forGetter(ClueData::command)
        ).apply(instance, ClueData::new));
    }

    public static final Codec<CluePreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(CluePreset::name),
            ComponentSerialization.CODEC.optionalFieldOf("belonging").forGetter(CluePreset::belonging),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(CluePreset::description),
            ResourceLocation.CODEC.fieldOf("model").forGetter(CluePreset::model),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(CluePreset::texture),
            ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(CluePreset::sound),
            ClueData.CODEC.listOf().fieldOf("clues").forGetter(CluePreset::clues),
            Codec.STRING.optionalFieldOf("on_complete").forGetter(CluePreset::onComplete)
    ).apply(instance, (name, belonging, description, model, texture, sound, clues, onComplete) ->
            new CluePreset(null, name, belonging, description, model, texture, sound, clues, onComplete)));

    /**
     * Create a copy with the full ResourceLocation ID set.
     */
    public CluePreset withId(ResourceLocation fullId) {
        return new CluePreset(fullId, name, belonging, description, model, texture, sound, clues, onComplete);
    }

    /**
     * Get the sound to play when a clue is discovered.
     */
    public ResourceLocation getDiscoverySound() {
        return sound.orElse(DEFAULT_SOUND);
    }

    /**
     * Get the total number of clues on this item.
     */
    public int getClueCount() {
        return clues.size();
    }
}
