package dcs.jagermeistars.talesmaker.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public record NpcPreset(
                ResourceLocation id,
                Component name,
                ResourceLocation model,
                ResourceLocation texture,
                ResourceLocation emissive,
                AnimationConfig animations,
                String head,
                HitboxConfig hitbox) {

        public record AnimationEntry(String name, int durationTicks) {
                public static final int DEFAULT_DURATION = 0; // 0 = use animation's natural length

                public static final Codec<AnimationEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                                Codec.STRING.fieldOf("name").forGetter(AnimationEntry::name),
                                Codec.INT.optionalFieldOf("duration", DEFAULT_DURATION).forGetter(AnimationEntry::durationTicks))
                                .apply(instance, AnimationEntry::new));

                // Simple string codec for backwards compatibility
                public static final Codec<AnimationEntry> STRING_OR_OBJECT_CODEC = Codec.either(
                                Codec.STRING,
                                CODEC
                ).xmap(
                                either -> either.map(name -> new AnimationEntry(name, DEFAULT_DURATION), entry -> entry),
                                entry -> entry.durationTicks == DEFAULT_DURATION
                                                ? com.mojang.datafixers.util.Either.left(entry.name)
                                                : com.mojang.datafixers.util.Either.right(entry)
                );
        }

        public record AnimationConfig(
                        ResourceLocation path,
                        String idle,
                        String walk,
                        AnimationEntry death) {

                public static final Codec<AnimationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                                ResourceLocation.CODEC.fieldOf("path").forGetter(AnimationConfig::path),
                                Codec.STRING.optionalFieldOf("idle", "idle").forGetter(AnimationConfig::idle),
                                Codec.STRING.optionalFieldOf("walk", "walk").forGetter(AnimationConfig::walk),
                                AnimationEntry.STRING_OR_OBJECT_CODEC.optionalFieldOf("death", new AnimationEntry("", 40)).forGetter(AnimationConfig::death))
                                .apply(instance, AnimationConfig::new));
        }

        public record HitboxConfig(float width, float height) {
                public static final HitboxConfig DEFAULT = new HitboxConfig(0.6f, 1.8f);

                public static final Codec<HitboxConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                                Codec.FLOAT.optionalFieldOf("width", 0.6f).forGetter(HitboxConfig::width),
                                Codec.FLOAT.optionalFieldOf("height", 1.8f).forGetter(HitboxConfig::height))
                                .apply(instance, HitboxConfig::new));
        }

        // Codec for parsing from JSON - uses string for ID
        public static final Codec<NpcPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("id").forGetter(preset -> preset.id().getPath()),
                        ComponentSerialization.CODEC.fieldOf("name").forGetter(NpcPreset::name),
                        ResourceLocation.CODEC.fieldOf("model").forGetter(NpcPreset::model),
                        ResourceLocation.CODEC.fieldOf("texture").forGetter(NpcPreset::texture),
                        ResourceLocation.CODEC.optionalFieldOf("emissive").forGetter(preset -> java.util.Optional.ofNullable(preset.emissive())),
                        AnimationConfig.CODEC.fieldOf("animations").forGetter(NpcPreset::animations),
                        Codec.STRING.optionalFieldOf("head", "head").forGetter(NpcPreset::head),
                        HitboxConfig.CODEC.optionalFieldOf("hitbox", HitboxConfig.DEFAULT).forGetter(NpcPreset::hitbox))
                        .apply(instance, (id, name, model, texture, emissive, animations, head, hitbox) -> new NpcPreset(
                                        null, name, model, texture, emissive.orElse(null), animations, head, hitbox)
                        ));

        // Helper to create preset with full ResourceLocation ID
        public NpcPreset withId(ResourceLocation fullId) {
                return new NpcPreset(fullId, name, model, texture, emissive, animations, head, hitbox);
        }
}
