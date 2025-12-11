package dcs.jagermeistars.talesmaker.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dcs.jagermeistars.talesmaker.client.animation.AnimationValidator;
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
         * Fallback chain: variant -> default -> placeholder
         */
        public String getAnimation(java.util.Set<String> conditions) {
            return getAnimation(conditions, defaultAnim);
        }

        /**
         * Get animation name for given conditions with custom placeholder.
         * Fallback chain: variant -> default -> placeholder
         * Does NOT validate animation existence - use getValidatedAnimation for that.
         */
        public String getAnimation(java.util.Set<String> conditions, String placeholder) {
            // Try to find a matching variant
            if (conditions != null && !conditions.isEmpty()) {
                for (String condition : conditions) {
                    String variant = variants.get(condition);
                    if (variant != null && !variant.isEmpty()) {
                        return variant;
                    }
                }
            }
            // Fall back to default if available
            if (defaultAnim != null && !defaultAnim.isEmpty()) {
                return defaultAnim;
            }
            // Fall back to placeholder
            return placeholder;
        }

        /**
         * Get animation name with validation against animation file.
         * Fallback chain: variant (if exists in file) -> default (always, no check) -> placeholder
         * Client-side only - validates variant existence in GeckoLibCache.
         *
         * @param conditions Active conditions for variant selection
         * @param placeholder Fallback animation name if default is not specified
         * @param animationFile The animation file to validate variants against
         * @return Animation name: validated variant, or default from preset, or placeholder
         */
        public String getValidatedAnimation(java.util.Set<String> conditions, String placeholder,
                                            net.minecraft.resources.ResourceLocation animationFile) {
            // Try to find a matching variant that exists in file
            if (conditions != null && !conditions.isEmpty()) {
                for (String condition : conditions) {
                    String variant = variants.get(condition);
                    if (variant != null && !variant.isEmpty()) {
                        // Validate variant exists in animation file
                        if (AnimationValidator.validateAnimation(animationFile, variant)) {
                            return variant;
                        }
                        // Variant doesn't exist in file - fall back to default
                    }
                }
            }
            // Fall back to default from preset (trust that it exists, no validation)
            if (defaultAnim != null && !defaultAnim.isEmpty()) {
                return defaultAnim;
            }
            // Fall back to placeholder only if default is not specified
            return placeholder;
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
     * Supports variants like base animations.
     */
    public record OverrideEntry(
            AnimationVariants animation,
            String mode,
            int duration,
            boolean blockHead
    ) {
        public static final Codec<OverrideEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AnimationVariants.CODEC.fieldOf("name").forGetter(OverrideEntry::animation),
                Codec.STRING.optionalFieldOf("mode", "hold").forGetter(OverrideEntry::mode),
                Codec.INT.optionalFieldOf("duration", 40).forGetter(OverrideEntry::duration),
                Codec.BOOL.optionalFieldOf("blockHead", true).forGetter(OverrideEntry::blockHead)
        ).apply(instance, OverrideEntry::new));

        /**
         * Get animation name for given conditions.
         */
        public String getName(java.util.Set<String> conditions) {
            return animation.getAnimation(conditions);
        }

        /**
         * Get default animation name (backwards compatibility).
         */
        public String name() {
            return animation.defaultAnim();
        }
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
     * Get idle animation name for given conditions (no validation).
     * Fallback chain: variant -> default -> "idle"
     */
    public String getIdleAnimation(java.util.Set<String> conditions) {
        return layers.base().idle().getAnimation(conditions, "idle");
    }

    /**
     * Get idle animation name with file validation (client-side only).
     * Fallback chain: variant (if exists in file) -> default -> "idle"
     */
    public String getValidatedIdleAnimation(java.util.Set<String> conditions) {
        return layers.base().idle().getValidatedAnimation(conditions, "idle", path());
    }

    /**
     * Get walk animation name for given conditions (no validation).
     * Fallback chain: variant -> default -> "walk"
     */
    public String getWalkAnimation(java.util.Set<String> conditions) {
        return layers.base().walk().getAnimation(conditions, "walk");
    }

    /**
     * Get walk animation name with file validation (client-side only).
     * Fallback chain: variant (if exists in file) -> default -> "walk"
     */
    public String getValidatedWalkAnimation(java.util.Set<String> conditions) {
        return layers.base().walk().getValidatedAnimation(conditions, "walk", path());
    }

    /**
     * Get run animation name for given conditions, or walk if run is not defined (no validation).
     * Fallback chain: variant -> default -> "run" -> walk animation
     */
    public String getRunAnimation(java.util.Set<String> conditions) {
        if (layers.base().run() != null) {
            return layers.base().run().getAnimation(conditions, "run");
        }
        return getWalkAnimation(conditions);
    }

    /**
     * Get run animation name with file validation (client-side only).
     * Fallback chain: variant (if exists in file) -> default -> "run" -> validated walk
     */
    public String getValidatedRunAnimation(java.util.Set<String> conditions) {
        if (layers.base().run() != null) {
            return layers.base().run().getValidatedAnimation(conditions, "run", path());
        }
        return getValidatedWalkAnimation(conditions);
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
