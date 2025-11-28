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
                AnimationConfig animations,
                String head) {

        public record AnimationConfig(
                        ResourceLocation path,
                        String idle,
                        String walk) {
                public static final Codec<AnimationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                                ResourceLocation.CODEC.fieldOf("path").forGetter(AnimationConfig::path),
                                Codec.STRING.optionalFieldOf("idle", "idle").forGetter(AnimationConfig::idle),
                                Codec.STRING.optionalFieldOf("walk", "walk").forGetter(AnimationConfig::walk))
                                .apply(instance, AnimationConfig::new));
        }

        // Codec for parsing from JSON - uses string for ID
        public static final Codec<NpcPreset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("id").forGetter(preset -> preset.id().getPath()),
                        ComponentSerialization.CODEC.fieldOf("name").forGetter(NpcPreset::name),
                        ResourceLocation.CODEC.fieldOf("model").forGetter(NpcPreset::model),
                        ResourceLocation.CODEC.fieldOf("texture").forGetter(NpcPreset::texture),
                        AnimationConfig.CODEC.fieldOf("animations").forGetter(NpcPreset::animations),
                        Codec.STRING.optionalFieldOf("head", "armor_head").forGetter(NpcPreset::head))
                        .apply(instance, (id, name, model, texture, animations, head) -> new NpcPreset(
                                        null, name, model,
                                        texture, animations, head) // ID will be set by manager
                        ));

        // Helper to create preset with full ResourceLocation ID
        public NpcPreset withId(ResourceLocation fullId) {
                return new NpcPreset(fullId, name, model, texture, animations, head);
        }
}
