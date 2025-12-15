package dcs.jagermeistars.talesmaker.data.choice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Represents a single choice option in a choice window.
 *
 * Choice types:
 * - Normal: No hidden or locked field - always visible and clickable
 * - Hidden: Has hidden field with predicate/advancement - not shown if condition fails
 * - Locked: Has locked field with predicate/advancement + message - shown but grayed out if condition fails
 */
public record Choice(
        Component text,
        String command,
        Optional<HiddenCondition> hidden,
        Optional<LockedCondition> locked
) {

    /**
     * Condition for hidden choices - choice is not shown if condition fails.
     */
    public record HiddenCondition(
            Optional<ResourceLocation> predicate,
            Optional<ResourceLocation> advancement
    ) {
        public static final Codec<HiddenCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("predicate").forGetter(HiddenCondition::predicate),
                ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(HiddenCondition::advancement)
        ).apply(instance, HiddenCondition::new));

        public boolean hasCondition() {
            return predicate.isPresent() || advancement.isPresent();
        }
    }

    /**
     * Condition for locked choices - choice is shown but grayed out if condition fails.
     */
    public record LockedCondition(
            Optional<ResourceLocation> predicate,
            Optional<ResourceLocation> advancement,
            Component message
    ) {
        public static final Codec<LockedCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("predicate").forGetter(LockedCondition::predicate),
                ResourceLocation.CODEC.optionalFieldOf("advancement").forGetter(LockedCondition::advancement),
                ComponentSerialization.CODEC.fieldOf("message").forGetter(LockedCondition::message)
        ).apply(instance, LockedCondition::new));

        public boolean hasCondition() {
            return predicate.isPresent() || advancement.isPresent();
        }
    }

    public static final Codec<Choice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("text").forGetter(Choice::text),
            Codec.STRING.fieldOf("command").forGetter(Choice::command),
            HiddenCondition.CODEC.optionalFieldOf("hidden").forGetter(Choice::hidden),
            LockedCondition.CODEC.optionalFieldOf("locked").forGetter(Choice::locked)
    ).apply(instance, Choice::new));

    /**
     * Check if this choice is a normal choice (no conditions).
     */
    public boolean isNormal() {
        return hidden.isEmpty() && locked.isEmpty();
    }

    /**
     * Check if this choice has a hidden condition.
     */
    public boolean isHidden() {
        return hidden.isPresent() && hidden.get().hasCondition();
    }

    /**
     * Check if this choice has a locked condition.
     */
    public boolean isLockable() {
        return locked.isPresent() && locked.get().hasCondition();
    }
}
