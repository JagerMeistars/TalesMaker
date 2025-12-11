package dcs.jagermeistars.talesmaker.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Configuration for item slot bone names in NPC presets.
 * Defines which bones should be used for rendering held items.
 */
public record ItemSlotsConfig(
        String mainhand,
        String offhand) {

    public static final String DEFAULT_MAINHAND = "right_item";
    public static final String DEFAULT_OFFHAND = "left_item";

    public static final ItemSlotsConfig DEFAULT = new ItemSlotsConfig(DEFAULT_MAINHAND, DEFAULT_OFFHAND);

    public static final Codec<ItemSlotsConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mainhand", DEFAULT_MAINHAND).forGetter(ItemSlotsConfig::mainhand),
            Codec.STRING.optionalFieldOf("offhand", DEFAULT_OFFHAND).forGetter(ItemSlotsConfig::offhand))
            .apply(instance, ItemSlotsConfig::new));

    /**
     * Check if the given bone name is the mainhand slot.
     */
    public boolean isMainhand(String boneName) {
        return mainhand != null && mainhand.equals(boneName);
    }

    /**
     * Check if the given bone name is the offhand slot.
     */
    public boolean isOffhand(String boneName) {
        return offhand != null && offhand.equals(boneName);
    }

    /**
     * Check if the given bone name is any item slot.
     */
    public boolean isItemSlot(String boneName) {
        return isMainhand(boneName) || isOffhand(boneName);
    }
}
