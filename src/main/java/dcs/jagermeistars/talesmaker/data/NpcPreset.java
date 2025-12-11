package dcs.jagermeistars.talesmaker.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * NPC preset configuration loaded from JSON.
 * Supports both legacy animation format and new NpcAnimationConfig format.
 */
public record NpcPreset(
        ResourceLocation id,
        Component name,
        ResourceLocation icon,
        ResourceLocation model,
        ResourceLocation texture,
        ResourceLocation emissive,
        NpcAnimationConfig animations,
        String head,
        HitboxConfig hitbox,
        AttributesConfig attributes,
        ItemSlotsConfig itemSlots) {

    // ===== Hitbox Config =====

    public record HitboxConfig(float width, float height) {
        public static final HitboxConfig DEFAULT = new HitboxConfig(0.6f, 1.8f);

        public static final Codec<HitboxConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("width", 0.6f).forGetter(HitboxConfig::width),
                Codec.FLOAT.optionalFieldOf("height", 1.8f).forGetter(HitboxConfig::height))
                .apply(instance, HitboxConfig::new));
    }

    // ===== Legacy Animation Support =====

    /**
     * Legacy AnimationEntry for death animation (backwards compatibility).
     */
    public record LegacyAnimationEntry(String name, int durationTicks) {
        public static final int DEFAULT_DURATION = 40;

        public static final Codec<LegacyAnimationEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(LegacyAnimationEntry::name),
                Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(LegacyAnimationEntry::durationTicks))
                .apply(instance, LegacyAnimationEntry::new));

        public static final Codec<LegacyAnimationEntry> STRING_OR_OBJECT_CODEC = Codec.either(
                Codec.STRING,
                CODEC
        ).xmap(
                either -> either.map(
                        str -> new LegacyAnimationEntry(str, DEFAULT_DURATION),
                        entry -> entry
                ),
                entry -> entry.durationTicks == DEFAULT_DURATION && !entry.name.isEmpty()
                        ? Either.left(entry.name)
                        : Either.right(entry)
        );
    }

    /**
     * Legacy AnimationConfig (backwards compatibility).
     * Automatically converts to NpcAnimationConfig.
     */
    public record LegacyAnimationConfig(
            ResourceLocation path,
            String idle,
            String walk,
            LegacyAnimationEntry death) {

        public static final Codec<LegacyAnimationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("path").forGetter(LegacyAnimationConfig::path),
                Codec.STRING.optionalFieldOf("idle", "idle").forGetter(LegacyAnimationConfig::idle),
                Codec.STRING.optionalFieldOf("walk", "walk").forGetter(LegacyAnimationConfig::walk),
                LegacyAnimationEntry.STRING_OR_OBJECT_CODEC
                        .optionalFieldOf("death", new LegacyAnimationEntry("", 40))
                        .forGetter(LegacyAnimationConfig::death))
                .apply(instance, LegacyAnimationConfig::new));

        /**
         * Convert legacy config to new NpcAnimationConfig format.
         */
        public NpcAnimationConfig toNewFormat() {
            // Build override map for death if present
            Map<String, NpcAnimationConfig.OverrideEntry> overrides;
            if (death.name() != null && !death.name().isEmpty()) {
                overrides = Map.of("death", new NpcAnimationConfig.OverrideEntry(
                        new NpcAnimationConfig.AnimationVariants(death.name(), Map.of(), 0),
                        "hold", death.durationTicks(), true));
            } else {
                overrides = Map.of();
            }

            return new NpcAnimationConfig(
                    path,
                    new NpcAnimationConfig.LayersConfig(
                            new NpcAnimationConfig.BaseLayerConfig(
                                    new NpcAnimationConfig.AnimationVariants(idle, Map.of(), 0),
                                    new NpcAnimationConfig.AnimationVariants(walk, Map.of(), 0),
                                    null // no run in legacy
                            ),
                            Map.of(), // no actions in legacy
                            overrides
                    ),
                    NpcAnimationConfig.TransitionsConfig.DEFAULT,
                    NpcAnimationConfig.HeadTrackingConfig.DEFAULT,
                    null // no body turn in legacy
            );
        }
    }

    // ===== Combined Animation Codec =====

    /**
     * Codec that accepts both legacy format and new NpcAnimationConfig format.
     *
     * Legacy format has: path, idle, walk, death
     * New format has: path, layers, headTracking, etc.
     */
    public static final Codec<NpcAnimationConfig> ANIMATION_CONFIG_CODEC = Codec.either(
            NpcAnimationConfig.CODEC,
            LegacyAnimationConfig.CODEC
    ).xmap(
            either -> either.map(
                    newFormat -> newFormat,
                    legacy -> legacy.toNewFormat()
            ),
            config -> Either.left(config) // Always serialize as new format
    );

    // ===== Main Preset Codec =====

    public static final Codec<NpcPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(preset -> preset.id() != null ? preset.id().getPath() : ""),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(NpcPreset::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(preset -> Optional.ofNullable(preset.icon())),
            ResourceLocation.CODEC.fieldOf("model").forGetter(NpcPreset::model),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(NpcPreset::texture),
            ResourceLocation.CODEC.optionalFieldOf("emissive").forGetter(preset -> Optional.ofNullable(preset.emissive())),
            ANIMATION_CONFIG_CODEC.fieldOf("animations").forGetter(NpcPreset::animations),
            Codec.STRING.optionalFieldOf("head", "head").forGetter(NpcPreset::head),
            HitboxConfig.CODEC.optionalFieldOf("hitbox", HitboxConfig.DEFAULT).forGetter(NpcPreset::hitbox),
            AttributesConfig.CODEC.optionalFieldOf("attributes", AttributesConfig.DEFAULT).forGetter(NpcPreset::attributes),
            ItemSlotsConfig.CODEC.optionalFieldOf("itemSlots", ItemSlotsConfig.DEFAULT).forGetter(NpcPreset::itemSlots))
            .apply(instance, (id, name, icon, model, texture, emissive, animations, head, hitbox, attributes, itemSlots) ->
                    new NpcPreset(null, name, icon.orElse(null), model, texture, emissive.orElse(null),
                            animations, head, hitbox, attributes, itemSlots)));

    // ===== Helper methods =====

    /**
     * Create preset with full ResourceLocation ID.
     */
    public NpcPreset withId(ResourceLocation fullId) {
        return new NpcPreset(fullId, name, icon, model, texture, emissive, animations, head, hitbox, attributes, itemSlots);
    }

    // ===== Convenience getters for backwards compatibility =====

    /**
     * Get idle animation name (convenience method).
     */
    public String getIdleAnimation() {
        return animations.getIdleAnimation(null);
    }

    /**
     * Get walk animation name (convenience method).
     */
    public String getWalkAnimation() {
        return animations.getWalkAnimation(null);
    }

    /**
     * Get death animation name (convenience method).
     */
    public String getDeathAnimation() {
        NpcAnimationConfig.OverrideEntry death = animations.getOverride("death");
        return death != null ? death.name() : "";
    }

    /**
     * Get death animation duration in ticks (convenience method).
     */
    public int getDeathDuration() {
        NpcAnimationConfig.OverrideEntry death = animations.getOverride("death");
        return death != null ? death.duration() : 40;
    }
}
