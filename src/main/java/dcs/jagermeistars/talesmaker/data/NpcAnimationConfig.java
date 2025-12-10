package dcs.jagermeistars.talesmaker.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven animation configuration for NPC entities.
 * Supports layered animations, head tracking, body turn, and conditional variants.
 */
public record NpcAnimationConfig(
        ResourceLocation path,
        LayersConfig layers,
        TransitionsConfig transitions,
        HeadTrackingConfig headTracking,
        @Nullable BodyTurnConfig bodyTurn
) {

    // ===== Sub-records =====

    /**
     * Animation layers configuration.
     */
    public record LayersConfig(
            BaseLayerConfig base,
            Map<String, ActionEntry> action,
            Map<String, OverrideEntry> override
    ) {
        public static final Codec<LayersConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BaseLayerConfig.CODEC.fieldOf("base").forGetter(LayersConfig::base),
                Codec.unboundedMap(Codec.STRING, ActionEntry.CODEC)
                        .optionalFieldOf("action", Map.of()).forGetter(LayersConfig::action),
                Codec.unboundedMap(Codec.STRING, OverrideEntry.CODEC)
                        .optionalFieldOf("override", Map.of()).forGetter(LayersConfig::override)
        ).apply(instance, LayersConfig::new));
    }

    /**
     * Base layer animations (idle, walk, run).
     */
    public record BaseLayerConfig(
            AnimationVariants idle,
            AnimationVariants walk,
            @Nullable AnimationVariants run
    ) {
        public static final Codec<BaseLayerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AnimationVariants.CODEC.fieldOf("idle").forGetter(BaseLayerConfig::idle),
                AnimationVariants.CODEC.fieldOf("walk").forGetter(BaseLayerConfig::walk),
                AnimationVariants.CODEC.optionalFieldOf("run").forGetter(c -> Optional.ofNullable(c.run()))
        ).apply(instance, (idle, walk, run) -> new BaseLayerConfig(idle, walk, run.orElse(null))));
    }

    /**
     * Animation with optional variants based on conditions.
     */
    public record AnimationVariants(
            String defaultAnim,
            Map<String, String> variants,
            float threshold
    ) {
        public static final Codec<AnimationVariants> CODEC = Codec.either(
                // Simple string format: "idle"
                Codec.STRING,
                // Full object format: { "default": "idle", "variants": {...}, "threshold": 0.15 }
                RecordCodecBuilder.<AnimationVariants>create(instance -> instance.group(
                        Codec.STRING.fieldOf("default").forGetter(AnimationVariants::defaultAnim),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("variants", Map.of()).forGetter(AnimationVariants::variants),
                        Codec.FLOAT.optionalFieldOf("threshold", 0.0f).forGetter(AnimationVariants::threshold)
                ).apply(instance, AnimationVariants::new))
        ).xmap(
                either -> either.map(
                        str -> new AnimationVariants(str, Map.of(), 0.0f),
                        full -> full
                ),
                variants -> variants.variants().isEmpty() && variants.threshold() == 0.0f
                        ? com.mojang.datafixers.util.Either.left(variants.defaultAnim())
                        : com.mojang.datafixers.util.Either.right(variants)
        );

        /**
         * Get animation name for given conditions.
         */
        public String getAnimation(java.util.Set<String> conditions) {
            if (conditions != null && !conditions.isEmpty()) {
                for (String condition : conditions) {
                    String variant = variants.get(condition);
                    if (variant != null) {
                        return variant;
                    }
                }
            }
            return defaultAnim;
        }
    }

    /**
     * Action layer animation entry.
     */
    public record ActionEntry(
            String name,
            String mode // "once", "loop", "hold"
    ) {
        public static final Codec<ActionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(ActionEntry::name),
                Codec.STRING.optionalFieldOf("mode", "once").forGetter(ActionEntry::mode)
        ).apply(instance, ActionEntry::new));
    }

    /**
     * Override layer animation entry (highest priority).
     */
    public record OverrideEntry(
            String name,
            String mode,
            int duration,
            boolean blockHead
    ) {
        public static final Codec<OverrideEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(OverrideEntry::name),
                Codec.STRING.optionalFieldOf("mode", "hold").forGetter(OverrideEntry::mode),
                Codec.INT.optionalFieldOf("duration", 40).forGetter(OverrideEntry::duration),
                Codec.BOOL.optionalFieldOf("blockHead", true).forGetter(OverrideEntry::blockHead)
        ).apply(instance, OverrideEntry::new));
    }

    /**
     * Transitions configuration.
     */
    public record TransitionsConfig(
            int defaultTransition,
            Map<String, Integer> custom
    ) {
        public static final TransitionsConfig DEFAULT = new TransitionsConfig(5, Map.of());

        public static final Codec<TransitionsConfig> CODEC = Codec.either(
                // Simple int format: 5
                Codec.INT,
                // Full object format: { "default": 5, "idle_to_walk": 3 }
                RecordCodecBuilder.<TransitionsConfig>create(instance -> instance.group(
                        Codec.INT.optionalFieldOf("default", 5).forGetter(TransitionsConfig::defaultTransition),
                        Codec.unboundedMap(Codec.STRING, Codec.INT)
                                .optionalFieldOf("custom", Map.of()).forGetter(c -> {
                                    // Filter out "default" key for serialization
                                    java.util.Map<String, Integer> filtered = new java.util.HashMap<>(c.custom());
                                    filtered.remove("default");
                                    return filtered;
                                })
                ).apply(instance, TransitionsConfig::new))
        ).xmap(
                either -> either.map(
                        i -> new TransitionsConfig(i, Map.of()),
                        full -> full
                ),
                config -> config.custom().isEmpty()
                        ? com.mojang.datafixers.util.Either.left(config.defaultTransition())
                        : com.mojang.datafixers.util.Either.right(config)
        );

        /**
         * Get transition ticks for given transition key.
         */
        public int getTransition(String from, String to) {
            String key = from + "_to_" + to;
            return custom.getOrDefault(key, defaultTransition);
        }
    }

    /**
     * Head tracking configuration.
     */
    public record HeadTrackingConfig(
            boolean enabled,
            String bone,
            String mode, // "additive" or "replace"
            float maxYaw,
            float maxPitch,
            float speed
    ) {
        public static final HeadTrackingConfig DEFAULT = new HeadTrackingConfig(
                true, "head", "additive", 70.0f, 40.0f, 0.25f
        );

        public static final Codec<HeadTrackingConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(HeadTrackingConfig::enabled),
                Codec.STRING.optionalFieldOf("bone", "head").forGetter(HeadTrackingConfig::bone),
                Codec.STRING.optionalFieldOf("mode", "additive").forGetter(HeadTrackingConfig::mode),
                Codec.FLOAT.optionalFieldOf("maxYaw", 70.0f).forGetter(HeadTrackingConfig::maxYaw),
                Codec.FLOAT.optionalFieldOf("maxPitch", 40.0f).forGetter(HeadTrackingConfig::maxPitch),
                Codec.FLOAT.optionalFieldOf("speed", 0.25f).forGetter(HeadTrackingConfig::speed)
        ).apply(instance, HeadTrackingConfig::new));

        public boolean isAdditive() {
            return "additive".equals(mode);
        }
    }

    /**
     * Body turn animation configuration.
     */
    public record BodyTurnConfig(
            boolean enabled,
            float threshold,
            @Nullable String animation,
            float speed
    ) {
        public static final Codec<BodyTurnConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enabled", false).forGetter(BodyTurnConfig::enabled),
                Codec.FLOAT.optionalFieldOf("threshold", 45.0f).forGetter(BodyTurnConfig::threshold),
                Codec.STRING.optionalFieldOf("animation").forGetter(c -> Optional.ofNullable(c.animation())),
                Codec.FLOAT.optionalFieldOf("speed", 0.12f).forGetter(BodyTurnConfig::speed)
        ).apply(instance, (enabled, threshold, animation, speed) ->
                new BodyTurnConfig(enabled, threshold, animation.orElse(null), speed)));
    }

    // ===== Main Codec =====

    /**
     * Full codec for NpcAnimationConfig.
     * Supports both new format and wrapping in "animations" object.
     */
    public static final Codec<NpcAnimationConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("path").forGetter(NpcAnimationConfig::path),
            LayersConfig.CODEC.optionalFieldOf("layers", new LayersConfig(
                    new BaseLayerConfig(
                            new AnimationVariants("idle", Map.of(), 0),
                            new AnimationVariants("walk", Map.of(), 0),
                            null
                    ),
                    Map.of(),
                    Map.of()
            )).forGetter(NpcAnimationConfig::layers),
            TransitionsConfig.CODEC.optionalFieldOf("transitions", TransitionsConfig.DEFAULT)
                    .forGetter(NpcAnimationConfig::transitions),
            HeadTrackingConfig.CODEC.optionalFieldOf("headTracking", HeadTrackingConfig.DEFAULT)
                    .forGetter(NpcAnimationConfig::headTracking),
            BodyTurnConfig.CODEC.optionalFieldOf("bodyTurn")
                    .forGetter(c -> Optional.ofNullable(c.bodyTurn()))
    ).apply(instance, (path, layers, transitions, headTracking, bodyTurn) ->
            new NpcAnimationConfig(path, layers, transitions, headTracking, bodyTurn.orElse(null))));

    // ===== Helper methods =====

    /**
     * Get idle animation name for given conditions.
     */
    public String getIdleAnimation(java.util.Set<String> conditions) {
        return layers.base().idle().getAnimation(conditions);
    }

    /**
     * Get walk animation name for given conditions.
     */
    public String getWalkAnimation(java.util.Set<String> conditions) {
        return layers.base().walk().getAnimation(conditions);
    }

    /**
     * Get run animation name for given conditions, or walk if run is not defined.
     */
    public String getRunAnimation(java.util.Set<String> conditions) {
        if (layers.base().run() != null) {
            return layers.base().run().getAnimation(conditions);
        }
        return getWalkAnimation(conditions);
    }

    /**
     * Get run speed threshold, or -1 if run is not defined.
     */
    public float getRunThreshold() {
        if (layers.base().run() != null) {
            return layers.base().run().threshold();
        }
        return -1.0f;
    }

    /**
     * Check if head tracking should be blocked for given override.
     */
    public boolean isHeadBlockedBy(String overrideId) {
        if (overrideId == null || overrideId.isEmpty()) {
            return false;
        }
        OverrideEntry entry = layers.override().get(overrideId);
        return entry != null && entry.blockHead();
    }

    /**
     * Get action entry by ID.
     */
    @Nullable
    public ActionEntry getAction(String actionId) {
        return layers.action().get(actionId);
    }

    /**
     * Get override entry by ID.
     */
    @Nullable
    public OverrideEntry getOverride(String overrideId) {
        return layers.override().get(overrideId);
    }
}
