package dcs.jagermeistars.talesmaker.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration record for NPC attributes.
 * Uses a Map-based approach to avoid Codec's 16-field limit.
 * All fields are optional - if not specified, default values are used.
 *
 * Supported JSON keys (snake_case):
 * max_health, follow_range, knockback_resistance, movement_speed, flying_speed,
 * attack_damage, attack_knockback, attack_speed, armor, armor_toughness, luck,
 * spawn_reinforcements, jump_strength, gravity, safe_fall_distance,
 * fall_damage_multiplier, step_height, scale, max_absorption,
 * block_interaction_range, entity_interaction_range, block_break_speed,
 * mining_efficiency, sneaking_speed, submerged_mining_speed, sweeping_damage_ratio,
 * movement_efficiency, water_movement_efficiency, oxygen_bonus, burning_time,
 * explosion_knockback_resistance
 */
public record AttributesConfig(Map<String, Double> values) {

    /**
     * Default configuration with no attributes specified.
     * All values will use entity defaults.
     */
    public static final AttributesConfig DEFAULT = new AttributesConfig(Map.of());

    /**
     * Codec for JSON serialization/deserialization.
     * Parses a map of string keys to double values.
     */
    public static final Codec<AttributesConfig> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
            .xmap(AttributesConfig::new, AttributesConfig::values);

    /**
     * Mapping from JSON keys to Minecraft attribute holders.
     */
    private static final Map<String, Holder<Attribute>> ATTRIBUTE_MAP = createAttributeMap();

    private static Map<String, Holder<Attribute>> createAttributeMap() {
        Map<String, Holder<Attribute>> map = new HashMap<>();
        map.put("max_health", Attributes.MAX_HEALTH);
        map.put("follow_range", Attributes.FOLLOW_RANGE);
        map.put("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE);
        map.put("movement_speed", Attributes.MOVEMENT_SPEED);
        map.put("flying_speed", Attributes.FLYING_SPEED);
        map.put("attack_damage", Attributes.ATTACK_DAMAGE);
        map.put("attack_knockback", Attributes.ATTACK_KNOCKBACK);
        map.put("attack_speed", Attributes.ATTACK_SPEED);
        map.put("armor", Attributes.ARMOR);
        map.put("armor_toughness", Attributes.ARMOR_TOUGHNESS);
        map.put("luck", Attributes.LUCK);
        map.put("spawn_reinforcements", Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        map.put("jump_strength", Attributes.JUMP_STRENGTH);
        map.put("gravity", Attributes.GRAVITY);
        map.put("safe_fall_distance", Attributes.SAFE_FALL_DISTANCE);
        map.put("fall_damage_multiplier", Attributes.FALL_DAMAGE_MULTIPLIER);
        map.put("step_height", Attributes.STEP_HEIGHT);
        map.put("scale", Attributes.SCALE);
        map.put("max_absorption", Attributes.MAX_ABSORPTION);
        map.put("block_interaction_range", Attributes.BLOCK_INTERACTION_RANGE);
        map.put("entity_interaction_range", Attributes.ENTITY_INTERACTION_RANGE);
        map.put("block_break_speed", Attributes.BLOCK_BREAK_SPEED);
        map.put("mining_efficiency", Attributes.MINING_EFFICIENCY);
        map.put("sneaking_speed", Attributes.SNEAKING_SPEED);
        map.put("submerged_mining_speed", Attributes.SUBMERGED_MINING_SPEED);
        map.put("sweeping_damage_ratio", Attributes.SWEEPING_DAMAGE_RATIO);
        map.put("movement_efficiency", Attributes.MOVEMENT_EFFICIENCY);
        map.put("water_movement_efficiency", Attributes.WATER_MOVEMENT_EFFICIENCY);
        map.put("oxygen_bonus", Attributes.OXYGEN_BONUS);
        map.put("burning_time", Attributes.BURNING_TIME);
        map.put("explosion_knockback_resistance", Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        return Map.copyOf(map);
    }

    /**
     * Applies all configured attributes to the given entity.
     * Only attributes that are present in this config will be modified.
     * Uses setBaseValue() to modify the attribute directly.
     *
     * @param entity The living entity to apply attributes to
     */
    public void applyTo(LivingEntity entity) {
        boolean healthChanged = false;

        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();

            Holder<Attribute> attributeHolder = ATTRIBUTE_MAP.get(key);
            if (attributeHolder != null) {
                AttributeInstance instance = entity.getAttribute(attributeHolder);
                if (instance != null) {
                    instance.setBaseValue(value);
                    if ("max_health".equals(key)) {
                        healthChanged = true;
                    }
                }
            }
        }

        // Special handling: if max_health was changed, heal to full
        if (healthChanged) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    /**
     * Gets a specific attribute value if configured.
     *
     * @param key The attribute key (e.g., "max_health")
     * @return Optional containing the value if configured
     */
    public Optional<Double> get(String key) {
        return Optional.ofNullable(values.get(key));
    }
}
